package com.discord.bot.maple.bots;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.XYStyler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Component
public class ExchangeRateService {

    @Value("${koreaexim.api.key}")
    private String koreaeximApiKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter SEARCH_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_RETRIES = 10;
    private static final long RETRY_INTERVAL_MS = 120_000L; // 2분

    // 캐시
    private volatile MessageEmbed cachedEmbed;
    private volatile byte[] cachedChartBytes;
    private volatile String cachedRateStr;  // null이면 주말/공휴일
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);

    public MessageEmbed getCachedEmbed() { return cachedEmbed; }
    public byte[] getCachedChartBytes() { return cachedChartBytes; }
    public String getCachedRateStr() { return cachedRateStr; }

    /**
     * 캐시가 없을 때 !환율 입력 시 호출.
     * 재시도 루프 없이 1회만 시도하고 성공 시 캐시 저장. 백그라운드 실행.
     */
    public void tryRefreshOnce() {
        if (!isRefreshing.compareAndSet(false, true)) return;

        CompletableFuture.runAsync(() -> {
            try {
                fetchAndCache();
                System.out.println("[ExchangeRateService] 수동 캐시 갱신 완료");
            } catch (Exception e) {
                System.err.println("[ExchangeRateService] 수동 캐시 갱신 실패: " + e.getMessage());
            } finally {
                isRefreshing.set(false);
            }
        });
    }

    // ───────── 캐시 갱신 ─────────

    /** 앱 시작 시 최초 1회 로드 */
    @PostConstruct
    public void init() {
        try {
            fetchAndCache();
            System.out.println("[ExchangeRateService] 초기 환율 캐시 로드 완료");
        } catch (Exception e) {
            System.err.println("[ExchangeRateService] 초기 환율 캐시 로드 실패: " + e.getMessage());
        }
    }

    /**
     * 공통 fetch + 캐시 저장 로직.
     * getCurrentRate(오늘날짜) + getHistoricalRates(22) 병렬 호출 후 캐시 갱신.
     * searchdate를 명시해야 오늘 데이터가 확실히 반환됨 (null 조회는 어제 데이터가 올 수 있음).
     */
    private void fetchAndCache() throws Exception {
        String todayParam = LocalDate.now().format(SEARCH_DATE_FORMATTER);

        CompletableFuture<String> rateFuture = CompletableFuture.supplyAsync(() -> {
            try { return getCurrentRate(todayParam); } catch (Exception e) { return null; }
        });
        CompletableFuture<TreeMap<String, Double>> histFuture = CompletableFuture.supplyAsync(() -> {
            try { return getHistoricalRates(22); } catch (Exception e) { return new TreeMap<>(); }
        });

        String rate = rateFuture.get();
        TreeMap<String, Double> hist = histFuture.get();

        // 오늘 데이터가 있으면 그래프에도 포함
        if (rate != null) {
            hist.put(LocalDate.now().toString(), Double.parseDouble(rate.replace(",", "")));
        }

        byte[] chart = getChartImage(hist);
        cachedEmbed = buildEmbed(rate, hist);
        cachedChartBytes = chart;
        cachedRateStr = rate;
    }

    /**
     * 매 영업일 오전 11시 갱신.
     * 오늘 날짜로 데이터를 조회하고, 미갱신이면 2분 간격으로 최대 10회 재시도.
     * Exception 발생 시에도 재시도 계속 (네트워크 일시 오류 대응).
     * (10회 초과 시 공휴일로 간주하고 종료)
     */
    @Scheduled(cron = "0 0 11 * * MON-FRI", zone = "Asia/Seoul")
    public void refreshCache() {
        String today = LocalDate.now().format(SEARCH_DATE_FORMATTER);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String rate = getCurrentRate(today);

                if (rate != null) {
                    TreeMap<String, Double> hist = getHistoricalRates(22);
                    hist.put(LocalDate.now().toString(), Double.parseDouble(rate.replace(",", "")));
                    byte[] chart = getChartImage(hist);
                    cachedEmbed = buildEmbed(rate, hist);
                    cachedChartBytes = chart;
                    cachedRateStr = rate;
                    System.out.println("[ExchangeRateService] 환율 캐시 갱신 완료 (" + attempt + "회 시도)");
                    return;
                }

                // 아직 미갱신
                System.out.println("[ExchangeRateService] 환율 미갱신 (" + attempt + "/" + MAX_RETRIES + "), " +
                        (RETRY_INTERVAL_MS / 60_000) + "분 후 재시도");
                Thread.sleep(RETRY_INTERVAL_MS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[ExchangeRateService] 재시도 중 인터럽트");
                return;
            } catch (Exception e) {
                // 네트워크 일시 오류 등 → 재시도 계속
                System.err.println("[ExchangeRateService] 환율 갱신 오류 (" + attempt + "/" + MAX_RETRIES + "): " + e.getMessage());
                try {
                    Thread.sleep(RETRY_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        System.err.println("[ExchangeRateService] 최대 재시도 초과 — 공휴일이거나 API 장애 가능성");
    }

    // ───────── API 호출 ─────────

    /**
     * 한국수출입은행 API에서 USD/KRW 매매기준율 반환.
     * @param searchDate yyyyMMdd 형식의 날짜. null이면 당일 최신 데이터 조회.
     *                   주말/공휴일 또는 미갱신 시 null 반환.
     */
    private String getCurrentRate(String searchDate) throws Exception {
        String url = "https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON"
                + "?authkey=" + koreaeximApiKey + "&data=AP01"
                + (searchDate != null ? "&searchdate=" + searchDate : "");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode array = objectMapper.readTree(res.body());
        for (JsonNode node : array) {
            if ("USD".equals(node.path("cur_unit").asText())) {
                return node.path("deal_bas_r").asText(); // 예: "1,395.00"
            }
        }
        return null;
    }

    /**
     * 캘린더 기준 businessDays * 2일 조회 후 마지막 businessDays개 영업일 데이터 반환.
     * 주말/공휴일 제외 후에도 항상 businessDays개에 가까운 데이터 포인트 보장.
     * 각 날짜를 병렬 요청으로 가져옴.
     */
    private TreeMap<String, Double> getHistoricalRates(int businessDays) throws Exception {
        int calendarDays = businessDays * 2;
        Map<String, Double> temp = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = calendarDays; i >= 1; i--) {
            final String date = LocalDate.now().minusDays(i).toString();
            final String searchDate = LocalDate.now().minusDays(i).format(SEARCH_DATE_FORMATTER);
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    String rate = getCurrentRate(searchDate);
                    if (rate != null) {
                        temp.put(date, Double.parseDouble(rate.replace(",", "")));
                    }
                } catch (Exception ignored) {}
            }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        TreeMap<String, Double> sorted = new TreeMap<>(temp);

        // 최근 businessDays개만 반환
        if (sorted.size() > businessDays) {
            List<String> keys = new ArrayList<>(sorted.keySet());
            TreeMap<String, Double> result = new TreeMap<>();
            keys.subList(keys.size() - businessDays, keys.size())
                    .forEach(k -> result.put(k, sorted.get(k)));
            return result;
        }
        return sorted;
    }

    /** XChart XYChart(Date x축)로 Discord 다크 테마 꺾은선(Area) 그래프 PNG 바이트 배열 반환. */
    private byte[] getChartImage(TreeMap<String, Double> rates) throws Exception {
        List<Date> xData = rates.keySet().stream()
                .map(d -> Date.from(LocalDate.parse(d)
                        .atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .collect(Collectors.toList());
        List<Double> yData = new ArrayList<>(rates.values());

        XYChart chart = new XYChartBuilder().width(800).height(400).build();
        XYStyler styler = chart.getStyler();

        Color bg        = new Color(0x2f, 0x31, 0x36);
        Color plotBg    = new Color(0x2b, 0x2d, 0x31);
        Color lineColor = new Color(99, 179, 237);
        Color fillColor = new Color(99, 179, 237, 60);
        Color textColor = new Color(0xbb, 0xbb, 0xbb);
        Color gridColor = new Color(255, 255, 255, 25);

        styler.setChartBackgroundColor(bg);
        styler.setPlotBackgroundColor(plotBg);
        styler.setPlotBorderColor(bg);
        styler.setChartFontColor(textColor);
        styler.setAxisTickLabelsColor(textColor);
        styler.setPlotGridLinesColor(gridColor);
        styler.setPlotGridLinesStroke(new BasicStroke(0.5f));
        styler.setLegendVisible(false);
        styler.setChartTitleVisible(false);
        styler.setPlotBorderVisible(false);
        styler.setXAxisTitleVisible(false);
        styler.setYAxisTitleVisible(false);
        styler.setDatePattern("MM/dd");
        styler.setYAxisDecimalPattern("#,###");

        // Y축 여백: 데이터 최솟값보다 약간 낮게 설정해 선이 바닥에 붙지 않게
        double minVal = yData.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double maxVal = yData.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double padding = (maxVal - minVal) * 0.1;
        styler.setYAxisMin(minVal - padding);
        styler.setYAxisMax(maxVal + padding);

        XYSeries series = chart.addSeries("USD/KRW", xData, yData);
        series.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Area);
        series.setLineColor(lineColor);
        series.setFillColor(fillColor);
        series.setMarker(SeriesMarkers.NONE);
        series.setLineWidth(2.5f);

        return BitmapEncoder.getBitmapBytes(chart, BitmapEncoder.BitmapFormat.PNG);
    }

    // ───────── 메시지 빌드 ─────────

    private MessageEmbed buildEmbed(String currentRateStr, TreeMap<String, Double> historical) {
        // hist에 오늘 데이터가 포함될 수 있으므로 오늘 키 제외 후 전일 계산
        String todayKey = LocalDate.now().toString();
        List<Map.Entry<String, Double>> entries = historical.entrySet().stream()
                .filter(e -> !e.getKey().equals(todayKey))
                .collect(Collectors.toList());
        int size = entries.size();

        double latestHistorical = size > 0 ? entries.get(size - 1).getValue() : 0;
        double secondLatest    = size >= 2 ? entries.get(size - 2).getValue() : latestHistorical;

        double currentRate = (currentRateStr != null)
                ? Double.parseDouble(currentRateStr.replace(",", ""))
                : latestHistorical;

        // 오늘 데이터 있음 → 전일 = 어제(역사 마지막)
        // 오늘 데이터 없음(주말) → 전일 = 마지막에서 두 번째
        double prevRate = (currentRateStr != null) ? latestHistorical : secondLatest;

        double diff = currentRate - prevRate;
        double pct  = prevRate != 0 ? (diff / prevRate * 100) : 0;

        String arrow = diff >= 0 ? "▲" : "▼";

        String displayRate = (currentRateStr != null)
                ? currentRateStr
                : String.format("%.2f", latestHistorical);
        String source = (currentRateStr != null)
                ? "매 영업일 오전 11시 갱신"
                : "주말/공휴일로 인해 최근 영업일 오전 11시 기준";

        // 상승 → 빨간 사이드바 / 하락 → 파란 사이드바 (한국 주식 관례)
        Color sidebarColor = diff >= 0
                ? new Color(0xed, 0x42, 0x45)
                : new Color(0x58, 0x65, 0xf2);

        String titleEmoji = diff >= 0 ? "📈" : "📉";
        String diffText = String.format("%s %.2f (%+.2f%%)", arrow, Math.abs(diff), pct);

        return new EmbedBuilder()
                .setTitle(titleEmoji + "  USD / KRW  환율")
                .setColor(sidebarColor)
                .addField("현재", displayRate + " 원", true)
                .addField("전일 대비", diffText, true)
                .setImage("attachment://exchange_rate.png")
                .setFooter(source)
                .build();
    }
}
