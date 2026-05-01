package com.discord.bot.maple.bots;

import com.discord.bot.maple.bots.exp.ExpLoader;
import com.discord.bot.maple.bots.exp.ExpTable;
import com.discord.bot.maple.bots.exp.ExpTrain;
import com.discord.bot.maple.bots.exp.ExpTrainHolder;
import com.discord.bot.maple.bots.schedule.BossScheduleListener;
import com.discord.bot.maple.bots.schedule.BossScheduleManager;
import com.discord.bot.maple.bots.schedule.BossScheduleSession;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.utils.FileUpload;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class SlashReceiveListener extends ListenerAdapter {

    private static final String[] DAY_NAMES = {"월", "화", "수", "목", "금", "토", "일"};

    private static final Map<Integer, Long> EXP_POTIONS = Map.of(
            199, 571115568L,
            209, 6120258214L,
            219, 22164317197L,
            229, 64359295696L,
            239, 137783047111L,
            249, 294971656640L,
            269, 2438047518853L
    );

    private static final DateTimeFormatter INPUT_FORMATTER  = DateTimeFormatter.ofPattern("HHmm");
    private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BossScheduleManager bossScheduleManager;
    private final Util util;
    private final CoreCalculator coreCalculator;
    private final FragmentCalculator fragmentCalculator;
    private final ExpTrainHolder expTrainHolder;

    public SlashReceiveListener(BossScheduleManager bossScheduleManager,
                                Util util,
                                CoreCalculator coreCalculator,
                                FragmentCalculator fragmentCalculator,
                                ExpTrainHolder expTrainHolder) {
        this.bossScheduleManager = bossScheduleManager;
        this.util = util;
        this.coreCalculator = coreCalculator;
        this.fragmentCalculator = fragmentCalculator;
        this.expTrainHolder = expTrainHolder;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String command = event.getName();
        List<ExpTable> expTable = ExpLoader.getExpTable();
        List<ExpTrain> expTrain = expTrainHolder.getTrains();

        switch (command) {
            case "ping":
                event.reply("**Pong!**").queue();
                break;
            case "조각계산":
                String coreType = event.getOption("코어타입").getAsString();
                int currentLevel = event.getOption("현재레벨").getAsInt();
                int targetLevel = event.getOption("목표레벨").getAsInt();

                if (currentLevel < 0 || currentLevel >= targetLevel || targetLevel > 30) {
                    event.reply("현재 레벨은 0 이상, 목표 레벨은 30이하여야 하며 목표 레벨은 현재 레벨보다 높아야 합니다.").queue();
                    break;
                }
                Map.Entry<Integer, Integer> calculateResult = coreCalculator.calculatePiece(coreType, currentLevel, targetLevel);
                File file = util.getFile("erda_piece.png");
                event.reply("**" + coreType + "** 의 **" + currentLevel + "레벨** 부터 **" + targetLevel + "레벨** 까지\n" +
                                "필요한 솔 개수는 **" + calculateResult.getKey() + " 개** 이고,\n" +
                                "필요한 조각 개수는 **" + calculateResult.getValue() + " 개** 입니다.")
                        .addFiles(FileUpload.fromData(file))
                        .queue();
                break;
            case "연뿌등록":
                String expTime = event.getOption("시작시간").getAsString();
                int expTerm = event.getOption("연뿌텀").getAsInt();
                int expCount = event.getOption("횟수").getAsInt();

                if (!Util.isValidHHmm(expTime)) {
                    event.reply("시간은 0 ~ 23, 분은 0 ~ 59 범위 내에서 입력이 가능합니다.").queue();
                    break;
                }
                if (expTime.length() != 4) {
                    event.reply("시작시간은 4글자여야 합니다. ex) 8시 5분 연뿌시작일 경우 0805 를 입력해주세요").queue();
                    break;
                }
                if (expTerm < 30) {
                    event.reply("연뿌텀은 30분 미만이 될 수 없습니다.").queue();
                    break;
                }
                for (char c : expTime.toCharArray()) {
                    if (!Character.isDigit(c)) {
                        event.reply("시작시간 입력은 숫자 4글자여야 합니다.").queue();
                        break;
                    }
                }

                LocalTime startTime = LocalTime.parse(expTime, INPUT_FORMATTER);
                LocalDateTime startDateTime = LocalDate.now().atTime(startTime);

                try {
                    expTrain.clear();
                    for (int i = 0; i < expCount; i++) {
                        LocalDateTime next = startDateTime.plusMinutes((long) expTerm * i);
                        expTrain.add(new ExpTrain(next.format(OUTPUT_FORMATTER), i + 1, expCount));
                    }
                } catch (Exception ex) {
                    expTrain.clear();
                    event.reply("오류 발생").queue();
                    break;
                }
                event.reply("등록 완료.\n **!연뿌** 로 확인하세요.").queue();
                break;
            case "연뿌초기화":
                expTrain.clear();
                event.reply("초기화 완료").queue();
                break;
            case "보스일정":
                handleBossScheduleCreate(event);
                break;
            case "보스일정결과":
                handleBossScheduleResult(event);
                break;
            case "보스일정확정":
                handleBossScheduleConfirm(event);
                break;
            case "보스일정초기화":
                handleBossScheduleReset(event);
                break;
            case "필요소재비계산":
                int totalNeedFragment = event.getOption("필요조각").getAsInt();
                int itemDropRatePct = event.getOption("아이템드롭률").getAsInt();
                int killCount = event.getOption("6분사냥마릿수").getAsInt();

                if (killCount < 0 || itemDropRatePct < 0 || totalNeedFragment < 0) {
                    event.reply("입력값은 0 이상이여야 합니다.").queue();
                    break;
                }
                int result = fragmentCalculator.calculateFragments(killCount, itemDropRatePct, totalNeedFragment);
                File file2 = util.getFile("wealth_small_2.png");
                event.reply("**" + totalNeedFragment + "** 개의 조각을 얻기 위해 \n" +
                                "필요한 소재비 개수는 **" + result + " 개** 입니다.\n" +
                                "지금 바로 재획 ㄱㄱ혓")
                        .addFiles(FileUpload.fromData(file2))
                        .queue();
                break;
        }
    }

    // ───────── 보스일정 핸들러 ─────────

    private void handleBossScheduleCreate(SlashCommandInteractionEvent event) {
        if (event.getChannelType() != ChannelType.GUILD_PUBLIC_THREAD
                && event.getChannelType() != ChannelType.GUILD_PRIVATE_THREAD) {
            event.reply("❌ 이 명령어는 **쓰레드** 안에서만 사용할 수 있습니다!").setEphemeral(true).queue();
            return;
        }

        String threadId = event.getChannel().getId();
        String bossName = event.getOption("보스이름") != null
                ? event.getOption("보스이름").getAsString()
                : "보스";

        bossScheduleManager.createSession(threadId, bossName);

        BossScheduleSession session = bossScheduleManager.getSession(threadId);
        List<ActionRow> buttons = BossScheduleListener.buildDayButtons(threadId);

        event.replyEmbeds(BossScheduleListener.buildMainEmbed(session, bossScheduleManager))
                .addComponents(buttons)
                .queue(hook -> hook.retrieveOriginal().queue(msg ->
                        bossScheduleManager.setMessageId(threadId, msg.getId())
                ));
    }

    private void handleBossScheduleResult(SlashCommandInteractionEvent event) {
        BossScheduleSession session = getSessionOrReply(event);
        if (session == null) return;

        event.deferReply().queue();

        Set<String> userIds = session.getUserSelections().keySet();
        String[] userIdArray = userIds.toArray(new String[0]);

        event.getGuild().retrieveMembersByIds(userIdArray).onSuccess(members -> {
            members.forEach(member ->
                    bossScheduleManager.cacheNickname(member.getId(), member.getEffectiveName())
            );
            event.getHook().sendMessageEmbeds(
                    BossScheduleListener.buildResultEmbed(session, bossScheduleManager)
            ).queue();
        });
    }

    private void handleBossScheduleConfirm(SlashCommandInteractionEvent event) {
        BossScheduleSession session = getSessionOrReply(event);
        if (session == null) return;

        String dayStr  = event.getOption("요일").getAsString();
        String timeStr = event.getOption("시간").getAsString();

        int dayIndex = -1;
        for (int i = 0; i < DAY_NAMES.length; i++) {
            if (DAY_NAMES[i].equals(dayStr)) { dayIndex = i; break; }
        }
        if (dayIndex == -1) {
            event.reply("❌ 요일을 올바르게 선택해 주세요.").setEphemeral(true).queue();
            return;
        }

        if (!timeStr.matches("^([01]?\\d|2[0-3]):[0-5]\\d$")) {
            event.reply("❌ 시간은 `21:00` 형식으로 입력해 주세요.").setEphemeral(true).queue();
            return;
        }

        String threadId = event.getChannel().getId();
        bossScheduleManager.confirm(threadId, dayIndex, timeStr);

        String messageId = bossScheduleManager.getMessageId(threadId);
        BossScheduleSession updated = bossScheduleManager.getSession(threadId);

        if (messageId != null) {
            event.getChannel()
                    .retrieveMessageById(messageId)
                    .queue(msg -> msg.editMessageEmbeds(
                            BossScheduleListener.buildMainEmbed(updated, bossScheduleManager)
                    ).queue());
        }

        Set<String> userIds = session.getUserSelections().keySet();
        String mentions = userIds.stream()
                .map(id -> "<@" + id + ">")
                .reduce("", (a, b) -> a + " " + b);

        event.reply("🎉 **" + session.getBossName() + "** 입장 시간이 확정되었습니다!\n"
                + "📅 **" + dayStr + "요일  " + timeStr + "**\n"
                + mentions).queue();
    }

    private void handleBossScheduleReset(SlashCommandInteractionEvent event) {
        String threadId = event.getChannel().getId();
        bossScheduleManager.removeSession(threadId);
        event.reply("🗑️ 보스 일정 조율이 초기화되었습니다.").queue();
    }

    /**
     * 현재 쓰레드의 보스 일정 세션을 조회하고,
     * 없으면 에러 메시지를 전송 후 null을 반환합니다.
     */
    private BossScheduleSession getSessionOrReply(SlashCommandInteractionEvent event) {
        String threadId = event.getChannel().getId();
        BossScheduleSession session = bossScheduleManager.getSession(threadId);
        if (session == null) {
            event.reply("❌ 진행 중인 보스 일정 조율이 없습니다. `/보스일정` 으로 먼저 시작하세요.")
                    .setEphemeral(true).queue();
        }
        return session;
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        List<CommandData> commandDatas = new ArrayList<>();
        commandDatas.add(Commands.slash("ping", "Pong을 해줍니다."));
        event.getGuild().updateCommands().addCommands(commandDatas).queue();
    }
}
