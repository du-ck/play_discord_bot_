package com.discord.bot.maple.bots.schedule;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
public class BossScheduleManager {

    private final BossScheduleRepository repo;

    // 메시지 ID는 휘발성이어도 괜찮으므로 메모리에만 유지
    private final Map<String, String> messageIdCache = new HashMap<>();
    // 보스 이름도 캐시 (DB에서 읽어도 되지만 편의상)
    private final Map<String, String> bossNameCache  = new HashMap<>();
    // userId → 닉네임 캐시 (이벤트 시점에 저장)
    private final Map<String, String> nicknameCache  = new HashMap<>();

    public BossScheduleManager(BossScheduleRepository repo) {
        this.repo = repo;
    }

    // ───────── 닉네임 캐시 ─────────
    public void cacheNickname(String userId, String nickname) {
        nicknameCache.put(userId, nickname);
    }

    public String getNickname(String userId) {
        return nicknameCache.getOrDefault(userId, userId); // 없으면 userId 그대로
    }

    // ───────── 세션 생성 ─────────
    public void createSession(String threadId, String bossName) {
        // 기존 데이터 초기화 후 새로 시작
        repo.deleteByThreadId(threadId);
        bossNameCache.put(threadId, bossName);
        messageIdCache.remove(threadId);
    }

    // ───────── 세션 존재 여부 ─────────
    public boolean hasSession(String threadId) {
        return bossNameCache.containsKey(threadId)
                || !repo.findByThreadId(threadId).isEmpty();
    }

    // ───────── 메시지 ID 관리 ─────────
    public void setMessageId(String threadId, String messageId) {
        messageIdCache.put(threadId, messageId);
    }
    public String getMessageId(String threadId) {
        return messageIdCache.get(threadId);
    }

    // ───────── 슬롯 저장 (upsert) ─────────
    public void setSlots(String threadId, String userId, int dayIndex, Set<Integer> slots) {
        String bossName = getBossName(threadId);
        String slotsStr = slotsToString(slots);

        Optional<BossScheduleEntity> existing =
                repo.findByThreadIdAndUserIdAndDayIndex(threadId, userId, dayIndex);

        if (existing.isPresent()) {
            existing.get().setSlots(slotsStr);
            repo.save(existing.get());
        } else {
            repo.save(new BossScheduleEntity(threadId, bossName, userId, dayIndex, slotsStr));
        }
    }

    // ───────── 확정 저장 ─────────
    public void confirm(String threadId, int dayIndex, String time) {
        List<BossScheduleEntity> rows = repo.findByThreadId(threadId);
        if (rows.isEmpty()) return;
        // 첫 번째 row에 확정 정보 저장
        BossScheduleEntity first = rows.get(0);
        first.setConfirmedDay(dayIndex);
        first.setConfirmedTime(time);
        repo.save(first);
    }

    // ───────── 세션 전체 조회 → BossScheduleSession 빌드 ─────────
    public BossScheduleSession getSession(String threadId) {
        List<BossScheduleEntity> rows = repo.findByThreadId(threadId);
        if (rows.isEmpty() && !bossNameCache.containsKey(threadId)) return null;

        String bossName = getBossName(threadId);
        BossScheduleSession session = new BossScheduleSession(threadId, bossName);
        session.setMessageId(messageIdCache.get(threadId));

        for (BossScheduleEntity row : rows) {
            Set<Integer> slots = stringToSlots(row.getSlots());
            session.setSlots(row.getUserId(), row.getDayIndex(), slots);
            // 확정 정보는 confirmedDay 가 null이 아닌 row에서 읽기
            if (row.getConfirmedDay() != null) {
                session.setConfirmed(row.getConfirmedDay(), row.getConfirmedTime());
            }
        }
        return session;
    }

    // ───────── 세션 삭제 ─────────
    @Transactional
    public void removeSession(String threadId) {
        repo.deleteByThreadId(threadId);
        bossNameCache.remove(threadId);
        messageIdCache.remove(threadId);
    }

    // ───────── 내부 유틸 ─────────
    private String getBossName(String threadId) {
        if (bossNameCache.containsKey(threadId)) return bossNameCache.get(threadId);
        List<BossScheduleEntity> rows = repo.findByThreadId(threadId);
        return rows.isEmpty() ? "보스" : rows.get(0).getBossName();
    }

    private String slotsToString(Set<Integer> slots) {
        if (slots == null || slots.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int s : new TreeSet<>(slots)) {
            if (sb.length() > 0) sb.append(",");
            sb.append(s);
        }
        return sb.toString();
    }

    private Set<Integer> stringToSlots(String str) {
        Set<Integer> result = new TreeSet<>();
        if (str == null || str.isBlank()) return result;
        for (String s : str.split(",")) {
            try { result.add(Integer.parseInt(s.trim())); } catch (NumberFormatException ignored) {}
        }
        return result;
    }
}
