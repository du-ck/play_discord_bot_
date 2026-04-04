package com.discord.bot.maple.bots.schedule;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.*;
import java.util.List;

@Component
public class BossScheduleListener extends ListenerAdapter {

    private static final String[] DAY_NAMES  = {"월", "화", "수", "목", "금", "토", "일"};
    private static final String[] DAY_EMOJIS = {"📅", "📅", "📅", "📅", "📅", "🎮", "🎮"};

    private final BossScheduleManager manager;

    public BossScheduleListener(BossScheduleManager manager) {
        this.manager = manager;
    }

    // ───────── 버튼 클릭 ─────────
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (id.startsWith("sched_day_")) handleDayButton(event, id);
    }

    private void handleDayButton(ButtonInteractionEvent event, String id) {
        // sched_day_{threadId}_{dayIndex}_{half}
        String[] p = id.split("_");
        if (p.length < 5) return;

        String threadId = p[2];
        int dayIndex    = Integer.parseInt(p[3]);
        int half        = Integer.parseInt(p[4]);

        BossScheduleSession session = manager.getSession(threadId);
        if (session == null) {
            event.reply("❌ 진행 중인 일정 조율이 없습니다.").setEphemeral(true).queue();
            return;
        }

        // 닉네임 캐싱 (버튼 누른 시점)
        String userId = event.getUser().getId();
        String nickname = event.getMember() != null
                ? event.getMember().getEffectiveName()
                : event.getUser().getName();
        manager.cacheNickname(userId, nickname);

        String dayName = DAY_NAMES[dayIndex];

        Set<Integer> existing = Optional.ofNullable(session.getUserSelections().get(userId))
                .map(m -> m.get(dayIndex))
                .orElse(Collections.emptySet());

        int startSlot = half == 0 ? 0  : 24;
        int endSlot   = half == 0 ? 23 : 47;

        StringSelectMenu.Builder menu = StringSelectMenu
                .create("sched_select_" + threadId + "_" + dayIndex + "_" + half)
                .setPlaceholder(dayName + "요일 " + (half == 0 ? "오전(00:00~11:30)" : "오후(12:00~23:30)") + " 가능 시간 선택")
                .setMinValues(0)
                .setMaxValues(endSlot - startSlot + 1);

        for (int slot = startSlot; slot <= endSlot; slot++) {
            String label = BossScheduleSession.slotToTime(slot);
            boolean selected = existing.contains(slot);
            menu.addOption(label, String.valueOf(slot),
                    selected
                            ? net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("✅")
                            : net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("🕐"));
        }

        Button switchBtn = half == 0
                ? Button.secondary("sched_day_" + threadId + "_" + dayIndex + "_1", "오후 보기 (12:00~23:30)")
                : Button.secondary("sched_day_" + threadId + "_" + dayIndex + "_0", "오전 보기 (00:00~11:30)");

        event.reply("**" + dayName + "요일** 가능한 시간대를 선택하세요.\n*(✅ = 이미 선택됨 / 다시 제출하면 해당 시간대만 덮어씁니다)*")
                .addComponents(ActionRow.of(menu.build()), ActionRow.of(switchBtn))
                .setEphemeral(true)
                .queue();
    }

    // ───────── 셀렉트 메뉴 제출 ─────────
    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String id = event.getComponentId();
        if (id.startsWith("sched_select_")) handleTimeSelect(event, id);
    }

    private void handleTimeSelect(StringSelectInteractionEvent event, String id) {
        // sched_select_{threadId}_{dayIndex}_{half}
        String[] p = id.split("_");
        if (p.length < 5) return;

        String threadId = p[2];
        int dayIndex    = Integer.parseInt(p[3]);
        int half        = Integer.parseInt(p[4]);

        BossScheduleSession session = manager.getSession(threadId);
        if (session == null) {
            event.reply("❌ 진행 중인 일정 조율이 없습니다.").setEphemeral(true).queue();
            return;
        }

        String userId  = event.getUser().getId();
        String dayName = DAY_NAMES[dayIndex];

        // 닉네임 캐싱 (선택 제출 시점에도 갱신)
        String nickname = event.getMember() != null
                ? event.getMember().getEffectiveName()
                : event.getUser().getName();
        manager.cacheNickname(userId, nickname);

        // 새로 선택한 슬롯
        Set<Integer> newSlots = new TreeSet<>();
        for (String v : event.getValues()) newSlots.add(Integer.parseInt(v));

        // 기존 슬롯 병합 (해당 half만 덮어쓰기)
        Set<Integer> merged = Optional.ofNullable(session.getUserSelections().get(userId))
                .map(m -> m.get(dayIndex))
                .map(TreeSet::new)
                .orElse(new TreeSet<>());

        int halfStart = half == 0 ? 0  : 24;
        int halfEnd   = half == 0 ? 23 : 47;
        merged.removeIf(s -> s >= halfStart && s <= halfEnd);
        merged.addAll(newSlots);

        // DB 저장
        manager.setSlots(threadId, userId, dayIndex, merged);

        // 메인 임베드 실시간 업데이트
        String messageId = manager.getMessageId(threadId);
        if (messageId != null) {
            BossScheduleSession updated = manager.getSession(threadId);
            event.getChannel()
                    .retrieveMessageById(messageId)
                    .queue(msg -> msg.editMessageEmbeds(buildMainEmbed(updated, manager)).queue());
        }

        String reply = merged.isEmpty()
                ? "**" + dayName + "요일** 선택을 초기화했습니다."
                : "**" + dayName + "요일** 저장 완료!\n> " + String.join(", ", BossScheduleSession.slotsToRanges(merged));

        event.reply(reply).setEphemeral(true).queue();
    }

    // ───────── 조율 현황 임베드 (실시간) ─────────
    public static MessageEmbed buildMainEmbed(BossScheduleSession session, BossScheduleManager manager) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("⚔️ " + session.getBossName() + " 입장 시간 조율");

        // 확정 여부에 따라 색상 변경
        boolean isConfirmed = session.getConfirmedDay() != null;
        eb.setColor(isConfirmed ? new Color(87, 242, 135) : new Color(88, 101, 242));

        StringBuilder desc = new StringBuilder();

        // 확정 배너
        if (isConfirmed) {
            desc.append("🎉 **입장 시간 확정!**\n");
            desc.append("```\n")
                .append(DAY_NAMES[session.getConfirmedDay()]).append("요일  ")
                .append(session.getConfirmedTime())
                .append("\n```\n");
        } else {
            desc.append("아래 **요일 버튼**을 클릭해 가능한 시간대를 선택하세요.\n");
            desc.append("여러 요일 복수 선택 가능 · 30분 단위\n\n");
        }

        Map<String, Map<Integer, Set<Integer>>> all = session.getUserSelections();

        for (int d = 0; d < 7; d++) {
            int count = session.getParticipantCount(d);
            desc.append("**").append(DAY_EMOJIS[d]).append(" ").append(DAY_NAMES[d]).append("요일**");
            if (count > 0) desc.append("  (").append(count).append("명 응답)");
            desc.append("\n");

            for (Map.Entry<String, Map<Integer, Set<Integer>>> entry : all.entrySet()) {
                Set<Integer> slots = entry.getValue().get(d);
                if (slots == null || slots.isEmpty()) continue;
                String name = manager.getNickname(entry.getKey());
                desc.append("  └ **").append(name).append("** : ")
                        .append(String.join(", ", BossScheduleSession.slotsToRanges(slots)))
                        .append("\n");
            }

            if (count >= 2) {
                Set<Integer> inter = session.getIntersection(d);
                if (!inter.isEmpty()) {
                    desc.append("  ✨ **공통 가능 시간: ")
                            .append(String.join(", ", BossScheduleSession.slotsToRanges(inter)))
                            .append("** (").append(count).append("명)\n");
                } else {
                    desc.append("  ❌ 겹치는 시간 없음\n");
                }
            }
            desc.append("\n");
        }

        eb.setDescription(desc.toString());

        if (isConfirmed) {
            eb.setFooter("✅ 확정 완료 · /보스일정초기화 로 새 조율 시작");
        } else {
            eb.setFooter("요일 버튼 클릭 → 시간 선택 · /보스일정결과 로 최종 결과 확인");
        }
        return eb.build();
    }

    // ───────── 최종 결과 임베드 (/보스일정결과) ─────────
    public static MessageEmbed buildResultEmbed(BossScheduleSession session, BossScheduleManager manager) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("⚔️ " + session.getBossName() + " 입장 시간 조율 결과");
        eb.setColor(new Color(87, 242, 135));

        // 확정 정보가 있으면 상단에 강조 표시
        if (session.getConfirmedDay() != null) {
            eb.setDescription("🎉 **" + DAY_NAMES[session.getConfirmedDay()] + "요일  "
                    + session.getConfirmedTime() + "** 으로 확정되었습니다!");
        }

        boolean anyResult = false;

        for (int d = 0; d < 7; d++) {
            int count = session.getParticipantCount(d);
            if (count == 0) continue;

            Set<Integer> inter = session.getIntersection(d);
            StringBuilder fieldValue = new StringBuilder();

            Map<String, Map<Integer, Set<Integer>>> all = session.getUserSelections();
            for (Map.Entry<String, Map<Integer, Set<Integer>>> entry : all.entrySet()) {
                Set<Integer> slots = entry.getValue().get(d);
                if (slots == null || slots.isEmpty()) continue;
                String name = manager.getNickname(entry.getKey());
                fieldValue.append("**").append(name).append("** : ")
                        .append(String.join(", ", BossScheduleSession.slotsToRanges(slots)))
                        .append("\n");
            }

            if (!inter.isEmpty()) {
                fieldValue.append("✅ **공통 가능 시간: ")
                        .append(String.join(", ", BossScheduleSession.slotsToRanges(inter)))
                        .append("** (").append(count).append("명)");
                anyResult = true;
            } else if (count >= 2) {
                fieldValue.append("❌ 겹치는 시간 없음");
            } else {
                fieldValue.append("⚠️ 아직 1명만 응답");
            }

            eb.addField(DAY_EMOJIS[d] + " " + DAY_NAMES[d] + "요일  (" + count + "명 응답)",
                    fieldValue.toString(), false);
        }

        if (!anyResult && session.getConfirmedDay() == null) {
            eb.setDescription("아직 충분한 응답이 없습니다. 파티원들이 시간을 선택해 주세요!");
        }

        return eb.build();
    }

    // ───────── 요일 버튼 Row ─────────
    public static List<ActionRow> buildDayButtons(String threadId) {
        List<Button> row1 = new ArrayList<>();
        List<Button> row2 = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            Button btn = Button.primary("sched_day_" + threadId + "_" + d + "_0", DAY_NAMES[d] + "요일");
            if (d < 4) row1.add(btn);
            else       row2.add(btn);
        }
        return List.of(ActionRow.of(row1), ActionRow.of(row2));
    }
}
