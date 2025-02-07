package com.discord.bot.maple.config;

import com.discord.bot.maple.bots.MessageReceiveListener;
import com.discord.bot.maple.bots.SlashReceiveListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Configuration
public class JdaConfig {

    @Bean
    public JDA jda(BotConfig botConfig) throws Exception {
        EnumSet<GatewayIntent> intents = EnumSet.of(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT
        );

        // JDA 인스턴스 생성
        JDA jda = JDABuilder.createDefault(botConfig.getBotToken())
                .enableIntents(intents)
                .setActivity(Activity.customStatus("개미친 재핵 중"))
                .addEventListeners(new MessageReceiveListener())
                .addEventListeners(new SlashReceiveListener())
                .build();

        // 슬래시 명령어 등록
        registerSlashCommands(jda);

        return jda;
    }

    private void registerSlashCommands(JDA jda) {
        jda.updateCommands().addCommands(
                Commands.slash("조각계산", "필요한 조각을 계산합니다.")
                        .addOptions(
                                new OptionData(OptionType.STRING, "코어타입", "코어종류 선택", true)
                                        .addChoice("강화코어", "강화코어")
                                        .addChoice("마스터리코어", "마스터리코어")
                                        .addChoice("스킬코어", "스킬코어")
                                        .addChoice("공용코어", "공용코어"),
                                new OptionData(OptionType.INTEGER, "현재레벨", "현재 레벨", true),
                                new OptionData(OptionType.INTEGER, "목표레벨", "목표 레벨", true)

                        )
        ).queue();
    }
}