package com.discord.bot.maple.bots;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private volatile String cachedMessage;
    private volatile byte[] cachedChartBytes;
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);

    public String getCachedMessage() { return cachedMessage; }
    public byte[] getCachedChartBytes() { return cachedChartBytes; }

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
     * getCurrentRate(null) + getHistoricalRates(7) 병렬 호출 후 캐시 갱신.
     */
    private void fetchAndCache() throws Exception {
        CompletableFuture<String> rateFuture = CompletableFuture.supplyAsync(() -> {
            try { return getCurrentRate(null); } catch (Exception e) { return null; }
        });
        CompletableFuture<TreeMap<String, Double>> histFuture = CompletableFuture.supplyAsync(() -> {
            try { return getHistoricalRates(7); } catch (Exception e) { return new TreeMap<>(); }
        });

        String rate = rateFuture.get();
        TreeMap<String, Double> hist = histFuture.get();

        // 오늘 데이터가 있으면 그래프에도 포함
        if (rate != null) {
            hist.put(LocalDate.now().toString(), Double.parseDouble(rate.replace(",", "")));
        }

        byte[] chart = getChartImage(hist);
        cachedMessage = buildRateMessage(rate, hist);
        cachedChartBytes = chart;
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
                    TreeMap<String, Double> hist = getHistoricalRates(7);
                    hist.put(LocalDate.now().toString(), Double.parseDouble(rate.replace(",", "")));
                    byte[] chart = getChartImage(hist);
                    cachedMessage = buildRateMessage(rate, hist);
                    cachedChartBytes = chart;
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
        String url = "https://www.koreaexim.go.kr/site/program/financial/exchangeJSON"
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

    /** QuickChart.io에서 다크 테마 꺾은선 그래프 PNG 바이트 배열 반환. */
    private byte[] getChartImage(TreeMap<String, Double> rates) throws Exception {
        List<String> labels = rates.keySet().stream()
                .map(d -> d.substring(5))
                .collect(Collectors.toList());
        List<Double> data = new ArrayList<>(rates.values());

        ObjectNode chart = objectMapper.createObjectNode();
        chart.put("type", "line");

        // 데이터
        ObjectNode chartData = chart.putObject("data");
        ArrayNode labelsArr = chartData.putArray("labels");
        labels.forEach(labelsArr::add);
        ObjectNode dataset = chartData.putArray("datasets").addObject();
        dataset.put("label", "USD/KRW");
        dataset.put("fill", true);
        dataset.put("borderColor", "rgb(99, 179, 237)");
        dataset.put("backgroundColor", "rgba(99, 179, 237, 0.15)");
        dataset.put("pointBackgroundColor", "rgb(99, 179, 237)");
        dataset.put("pointBorderColor", "rgb(99, 179, 237)");
        dataset.put("pointRadius", 4);
        dataset.put("pointHoverRadius", 6);
        dataset.put("borderWidth", 2);
        dataset.put("tension", 0.4);
        ArrayNode dataArr = dataset.putArray("data");
        data.forEach(dataArr::add);

        // 옵션
        ObjectNode options = chart.putObject("options");

        // 범례 숨김
        options.putObject("plugins").putObject("legend").put("display", false);

        // 축
        ObjectNode scales = options.putObject("scales");

        ObjectNode xAxis = scales.putObject("x");
        xAxis.putObject("grid").put("color", "rgba(255,255,255,0.08)");
        xAxis.putObject("ticks").put("color", "#aaaaaa");

        ObjectNode yAxis = scales.putObject("y");
        yAxis.putObject("grid").put("color", "rgba(255,255,255,0.08)");
        ObjectNode yTicks = yAxis.putObject("ticks");
        yTicks.put("color", "#aaaaaa");
        yTicks.put("precision", 0);

        String chartJson = objectMapper.writeValueAsString(chart);
        // backgroundColor는 Discord 다크 테마와 어울리는 #2f3136
        String url = "https://quickchart.io/chart?c="
                + URLEncoder.encode(chartJson, StandardCharsets.UTF_8)
                + "&width=800&height=400&backgroundColor=%232f3136";

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<byte[]> res = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
        return res.body();
    }

    // ───────── 메시지 빌드 ─────────

    private String buildRateMessage(String currentRateStr, TreeMap<String, Double> historical) {
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
        double pct = prevRate != 0 ? (diff / prevRate * 100) : 0;

        String arrow = diff >= 0 ? "▲" : "▼";
        String colorCode = diff >= 0 ? "[31m" : "[34m";
        String reset = "[0m";

        String displayRate = (currentRateStr != null)
                ? currentRateStr
                : String.format("%.2f", latestHistorical);
        String source = (currentRateStr != null)
                ? "※ 매 영업일 오전 11시 갱신"
                : "※ 주말/공휴일로 인해 최근 영업일 기준";

        return "```ansi\n"
                + "💱 USD/KRW 환율\n"
                + "현재: " + displayRate + " 원\n"
                + "전일 대비: " + colorCode
                + String.format("%s %.2f (%+.2f%%)", arrow, Math.abs(diff), pct)
                + reset + "\n"
                + source + "\n"
                + "```";
    }
}
