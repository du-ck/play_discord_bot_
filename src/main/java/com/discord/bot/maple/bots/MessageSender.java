package com.discord.bot.maple.bots;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MessageSender {

    private final JDA jda;

    public MessageSender(JDA jda) {
        this.jda = jda;
    }

    @Scheduled(cron = "0 0 9 * * WED", zone = "Asia/Seoul")      //매주 수요일 오전9시
    public void culvertNotice() {   //수로 공지

        // Discord 채널 ID를 설정하세요
        String channelId = "1343608742218043502";   //길드공지 채널id
        TextChannel channel = jda.getTextChannelById(channelId);

        if (channel == null) {
            System.err.println("채널을 찾을 수 없습니다: " + channelId);
            return;
        }

        String message = "<@&1328998233720098856> 노블을 쓰고싶다면 수로 ㄱㄱ혓";
        channel.sendMessage(message).queue();
    }
}
