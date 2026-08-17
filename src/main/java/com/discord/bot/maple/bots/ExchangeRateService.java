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

    // 네이버 API 응답 데이터
    private record NaverRateResult(
            String closePrice,        // "1,532.70"
            String fluctuations,      // "18.20"
            String fluctuationsRatio, // "1.20"
            boolean rising,           // true=RISING, false=FALLING/EVEN
            String localTradedAt,     // "2026-06-02T22:10:26+09:00"
            boolean isToday           // localTradedAt 날짜 == 오늘
    ) {}

    // 그래프 캐시만 유지 (현재 환율은 호출 시마다 실시간 조회)
    private volatile byte[] cachedChartBytes;
    private final AtomicBoolean isChartRefreshing = new AtomicBoolean(false);

    public byte[] getCachedChartBytes() { return cachedChartBytes; }

    /**
     * 그래프 캐시가 없을 때 !환율 입력 시 호출.
     * 백그라운드에서 그래프만 갱신.
     */
    public void tryRefreshChartOnce() {
        if (!isChartRefreshing.compareAndSet(false, true)) return;

        CompletableFuture.runAsync(() -> {
            try {
                cachedChartBytes = buildChartBytes();
                System.out.println("[ExchangeRateService] 그래프 캐시 갱신 완료");
            } catch (Exception e) {
                System.err.println("[ExchangeRateService] 그래프 캐시 갱신 실패: " + e.getMessage());
            } finally {
                isChartRefreshing.set(false);
            }
        });
    }

    // ───────── 캐시 갱신 ─────────

    /** 앱 시작 시 그래프 캐시 1회 로드 */
    @PostConstruct
    public void init() {
        try {
            cachedChartBytes = buildChartBytes();
            System.out.println("[ExchangeRateService] 초기 그래프 캐시 로드 완료");
        } catch (Exception e) {
            System.err.println("[ExchangeRateService] 초기 그래프 캐시 로드 실패: " + e.getMessage());
        }
    }

    /** 매 영업일 오전 11시 그래프 갱신 */
    @Scheduled(cron = "0 0 11 * * MON-FRI", zone = "Asia/Seoul")
    public void refreshChartCache() {
        try {
            cachedChartBytes = buildChartBytes();
            System.out.println("[ExchangeRateService] 그래프 캐시 갱신 완료");
        } catch (Exception e) {
            System.err.println("[ExchangeRateService] 그래프 캐시 갱신 실패: " + e.getMessage());
        }
    }

    /**
     * 수출입은행 히스토리 데이터로 그래프 PNG 생성.
     */
    private byte[] buildChartBytes() throws Exception {
        TreeMap<String, Double> hist = getHistoricalRates(22);
        // 오늘 수출입은행 데이터도 포함 시도
        String todayRate = getCurrentRate(LocalDate.now().format(SEARCH_DATE_FORMATTER));
        if (todayRate != null) {
            hist.put(LocalDate.now().toString(), Double.parseDouble(todayRate.replace(",", "")));
        }
        return getChartImage(hist);
    }

    // ───────── 실시간 환율 조회 ─────────

    /**
     * 네이버 API 실시간 호출 후 embed 반환.
     * !환율 명령어마다 호출됨.
     */
    public MessageEmbed fetchLiveEmbed() throws Exception {
        NaverRateResult naverRate = getNaverRate();
        return buildEmbed(naverRate);
    }

    // ───────── API 호출 ─────────

    /**
     * 네이버 모바일 API에서 USD/KRW 현재 환율 + 전일 대비 데이터 반환.
     */
    private NaverRateResult getNaverRate() throws Exception {
        String url = "https://m.stock.naver.com/front-api/marketIndex/exchange/exchangeCodes?exchangeCodes=FX_USDKRW";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(res.body());
        JsonNode result = root.path("result").get(0);

        String closePrice = result.path("closePrice").asText();
        String fluctuations = result.path("fluctuations").asText();
        String fluctuationsRatio = result.path("fluctuationsRatio").asText();
        boolean rising = "RISING".equals(result.path("fluctuationsType").path("name").asText());
        String localTradedAt = result.path("localTradedAt").asText();

        LocalDate tradedDate = LocalDate.parse(localTradedAt.substring(0, 10));
        boolean isToday = tradedDate.equals(LocalDate.now());

        return new NaverRateResult(closePrice, fluctuations, fluctuationsRatio, rising, localTradedAt, isToday);
    }

    /**
     * 한국수출입은행 API에서 USD/KRW 매매기준율 반환. (히스토리 그래프용)
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
                return node.path("deal_bas_r").asText();
            }
        }
        return null;
    }

    /**
     * 캘린더 기준 businessDays * 2일 조회 후 마지막 businessDays개 영업일 데이터 반환.
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

    private MessageEmbed buildEmbed(NaverRateResult naverRate) {
        boolean isToday = naverRate.isToday();

        String displayRate;
        String diffText;
        boolean rising;
        String footerText;

        if (isToday) {
            displayRate = naverRate.closePrice();
            rising = naverRate.rising();
            String arrow = rising ? "▲" : "▼";
            diffText = String.format("%s %s (%s%%)", arrow, naverRate.fluctuations(), naverRate.fluctuationsRatio());
            // "2026-06-02T22:10:26+09:00" → "06/02 22:10"
            String tradedAt = naverRate.localTradedAt().substring(5, 16).replace("T", " ");
            footerText = tradedAt + " 기준";
        } else {
            // 주말/공휴일 — 네이버가 마지막 영업일 데이터를 그대로 반환
            displayRate = naverRate.closePrice();
            rising = naverRate.rising();
            String arrow = rising ? "▲" : "▼";
            diffText = String.format("%s %s (%s%%)", arrow, naverRate.fluctuations(), naverRate.fluctuationsRatio());
            String tradedAt = naverRate.localTradedAt().substring(5, 16).replace("T", " ");
            footerText = tradedAt + " 기준";
        }

        Color sidebarColor = rising
                ? new Color(0xed, 0x42, 0x45)
                : new Color(0x58, 0x65, 0xf2);
        String titleEmoji = rising ? "📈" : "📉";

        return new EmbedBuilder()
                .setTitle(titleEmoji + "  USD / KRW  환율")
                .setColor(sidebarColor)
                .addField("현재", displayRate + " 원", true)
                .addField("전일 대비", diffText, true)
                .setImage("attachment://exchange_rate.png")
                .setFooter(footerText)
                .build();
    }
}
