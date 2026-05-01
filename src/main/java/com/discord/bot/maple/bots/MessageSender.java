package com.discord.bot.maple.bots;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MessageSender {

    private final JDA jda;

    @Value("${discord.channels.guild-notice}")
    private String guildNoticeChannelId;

    public MessageSender(JDA jda) {
        this.jda = jda;
    }

    @Scheduled(cron = "0 0 18 * * WED", zone = "Asia/Seoul")      //매주 수요일 오전9시
    public void culvertNotice() {   //수로 공지
        TextChannel channel = jda.getTextChannelById(guildNoticeChannelId);

        if (channel == null) {
            System.err.println("채널을 찾을 수 없습니다: " + guildNoticeChannelId);
            return;
        }

        //String message = "<@&1328998233720098856> 노블을 쓰고싶다면 수로 ㄱㄱ혓";
        String message = "@everyone 오늘은 수로 / 플래그 정산일입니다.\n" +
                "수플 참여기록 없을 시 노블스킬 사용 제한됩니다.";
        channel.sendMessage(message).queue();
    }
}
