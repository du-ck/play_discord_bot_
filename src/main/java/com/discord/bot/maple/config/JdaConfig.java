package com.discord.bot.maple.config;

import com.discord.bot.maple.bots.MessageReceiveListener;
import com.discord.bot.maple.bots.SlashReceiveListener;
import com.discord.bot.maple.bots.schedule.BossScheduleListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumSet;

@Configuration
public class JdaConfig {

    @Bean
    public JDA jda(BotConfig botConfig,
                   MessageReceiveListener messageReceiveListener,
                   SlashReceiveListener slashReceiveListener,
                   BossScheduleListener bossScheduleListener) throws Exception {

        EnumSet<GatewayIntent> intents = EnumSet.of(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT
        );

        JDA jda = JDABuilder.createDefault(botConfig.getBotToken())
                .enableIntents(intents)
                .setActivity(Activity.customStatus("재핵ㄱㄱ혓"))
                .addEventListeners(messageReceiveListener)    // Spring 빈 주입
                .addEventListeners(slashReceiveListener)      // Spring 빈 주입
                .addEventListeners(bossScheduleListener)      // Spring 빈 주입
                .build();

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
                                        .addChoice("공용코어", "공용코어")
                                        .addChoice("3차공용코어", "3차공용코어"),
                                new OptionData(OptionType.INTEGER, "현재레벨", "현재 레벨", true),
                                new OptionData(OptionType.INTEGER, "목표레벨", "목표 레벨", true)
                        ),
                Commands.slash("연뿌등록", "연뿌시작시간과 횟수를 등록하여 테이블을 만듭니다.")
                        .addOptions(
                                new OptionData(OptionType.STRING, "시작시간", "ex) 오후 3시20분 연뿌시작일 경우 1520 입력", true),
                                new OptionData(OptionType.INTEGER, "연뿌텀", "32분마다 연뿌면 32 입력", true),
                                new OptionData(OptionType.INTEGER, "횟수", "10연뿌면 10 입력", true)
                        ),
                Commands.slash("연뿌초기화", "등록한 연뿌테이블을 초기화 합니다."),
                Commands.slash("필요소재비계산", "필요한 조각을 입력하여 현재상황에 맞는 소재비 개수를 계산합니다.")
                        .addOptions(
                                new OptionData(OptionType.INTEGER, "필요조각", "필요한 조각 개수 ex) 35200", true),
                                new OptionData(OptionType.INTEGER, "아이템드롭률", "아이템 드롭률 % 단위 ex) 320", true),
                                new OptionData(OptionType.INTEGER, "6분사냥마릿수", "6분 동안 잡는 몬스터 수 ex) 1920", true)
                        ),
                Commands.slash("보스일정", "이 쓰레드의 보스 입장 시간 조율을 시작합니다.")
                        .addOptions(
                                new OptionData(OptionType.STRING, "보스이름", "보스 이름 ex) 카링", false)
                        ),
                Commands.slash("보스일정확정", "이 쓰레드의 보스 입장 시간을 확정하고 파티원에게 알립니다.")
                        .addOptions(
                                new OptionData(OptionType.STRING, "요일", "확정할 요일", true)
                                        .addChoice("월", "월").addChoice("화", "화")
                                        .addChoice("수", "수").addChoice("목", "목")
                                        .addChoice("금", "금").addChoice("토", "토")
                                        .addChoice("일", "일"),
                                new OptionData(OptionType.STRING, "시간", "확정할 시간 ex) 21:00", true)
                        ),
                Commands.slash("보스일정결과", "이 쓰레드의 보스 입장 시간 조율 결과를 확인합니다."),
                Commands.slash("보스일정초기화", "이 쓰레드의 보스 입장 시간 조율을 초기화합니다.")
        ).queue();
    }
}
