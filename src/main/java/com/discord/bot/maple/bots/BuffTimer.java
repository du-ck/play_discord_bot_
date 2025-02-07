package com.discord.bot.maple.bots;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Component
public class BuffTimer {

    private final JDA jda;

    public BuffTimer(JDA jda) {
        this.jda = jda;
    }

    // 매 시간 30분마다 실행
    @Scheduled(cron = "0 30 * * * *")   //초 분
    public void run() {
        LocalDateTime now = LocalDateTime.now();

        // Discord 채널 ID를 설정하세요
        String channelId = "1331537856480415784";   //test channel id
        TextChannel channel = jda.getTextChannelById(channelId);

        if (channel == null) {
            System.err.println("채널을 찾을 수 없습니다: " + channelId);
            return;
        }

        // 현재 시간이 짝수 시간인지 확인
        if (now.getHour() % 2 == 0) {
            String message = "버프 받으러 ㄱㄱㄱ";
            System.out.println(message);
            channel.sendMessage(message).queue();
        } else {
            String message = "버프 받으러 ㄱㄱㄱ";
            System.out.println(message);
            channel.sendMessage(message).queue();
        }
    }
}