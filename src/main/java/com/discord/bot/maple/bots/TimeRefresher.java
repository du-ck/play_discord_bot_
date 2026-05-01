package com.discord.bot.maple.bots;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 */
@Component
public class TimeRefresher {

    private final JDA jda;

    @Value("${discord.channels.server-time}")
    private String serverTimeChannelId;

    @Value("${discord.channels.server-status}")
    private String serverStatusChannelId;

    private String lastStatus = "";

    public TimeRefresher(JDA jda) {
        this.jda = jda;
    }

    //서버시간 갱신
    @Scheduled(cron = "0 0/10 * * * *")
    public void refreshServerTime() {
        LocalDateTime now = LocalDateTime.now();

        VoiceChannel channel = jda.getVoiceChannelById(serverTimeChannelId);

        if (channel == null) {
            System.err.println("채널을 찾을 수 없습니다: " + serverTimeChannelId);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date nowDate = new Date();
        channel.getManager().setName("서버시간:\u1CBC" + sdf.format(nowDate)).queue();
    }

    @Scheduled(cron = "0 * * * * *")
    public void checkLoginServer() {
        //String apiUrl = "https://www.nexon.com/api/maplestory/no-auth/v1/server-status2/na";

        try {

            /*HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String jsonResponse = response.body();

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode serversArray = rootNode.path("servers");

            if (serversArray.isArray()) {
                for (JsonNode server : serversArray) {
                    // worldName이 Hyperion인 것 찾기
                    if ("Hyperion".equals(server.path("worldName").asText())) {
                        int loginStatus = server.path("Login00").asInt();
                        String statusText = (loginStatus >= 1) ? "✅온라인" : "⛔오프라인";

                        // 3. 디스코드 채널 이름 업데이트
                        String channelId = "1461348967756468234";
                        VoiceChannel channel = jda.getVoiceChannelById(channelId);
                        if (channel != null) {
                            // 공백 넣기 : "\u1CBC"
                            channel.getManager().setName("서버상태:\u1CBC" + statusText).queue();
                        }
                        break; // Hyperion을 찾았으니 루프 종료
                    }
                }
            }*/

            String ip = "34.215.62.60";
            int port = 8484;
            int timeoutMillis = 3000;

            boolean result = isServerOnline(ip, port, timeoutMillis);
            String statusText = result ? "✅온라인" : "⛔오프라인";

            // 우리가 설정할 최종 채널 이름 형태
            String newChannelName = "서버상태:\u1CBC" + statusText;

            VoiceChannel channel = jda.getVoiceChannelById(serverStatusChannelId);

            if (channel != null) {
                // 1. 현재 채널의 이름을 가져옴
                String currentName = channel.getName();

                // 현재 이름과 새로 바꿀 이름이 다를 때만 업데이트를 진행
                if (!newChannelName.equals(currentName)) {
                    channel.getManager().setName(newChannelName).queue();
                }
            }

        } catch (Exception e) {
            System.err.println("서버 상태 갱신 중 오류 발생: " + e.getMessage());
        }
    }

    public static boolean isServerOnline(String ip, int port, int timeout) {
        try (Socket socket = new Socket()) {
            // 해당 IP와 포트로 연결 시도
            socket.connect(new InetSocketAddress(ip, port), timeout);
            return true;
        } catch (IOException e) {
            // 연결 실패 (타임아웃, 거부 등)
            return false;
        }
    }
}