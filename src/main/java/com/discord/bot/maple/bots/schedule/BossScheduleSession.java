package com.discord.bot.maple.bots.schedule;

import java.util.*;

public class BossScheduleSession {

    private final String threadId;
    private final String bossName;
    private String messageId;

    // 확정 정보
    private Integer confirmedDay;   // null = 미확정
    private String confirmedTime;

    public Integer getConfirmedDay() { return confirmedDay; }
    public String getConfirmedTime() { return confirmedTime; }
    public void setConfirmed(Integer day, String time) {
        this.confirmedDay = day;
        this.confirmedTime = time;
    }

    // 유저ID → (요일인덱스(0=월~6=일) → 선택한 슬롯 Set(0=00:00 ~ 47=23:30))
    private final Map<String, Map<Integer, Set<Integer>>> userSelections = new HashMap<>();

    public BossScheduleSession(String threadId, String bossName) {
        this.threadId = threadId;
        this.bossName = bossName;
    }

    public String getThreadId() { return threadId; }
    public String getBossName() { return bossName; }
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public Map<String, Map<Integer, Set<Integer>>> getUserSelections() {
        return userSelections;
    }

    /**
     * 특정 유저의 특정 요일 슬롯 목록을 교체 저장
     */
    public void setSlots(String userId, int dayIndex, Set<Integer> slots) {
        userSelections.computeIfAbsent(userId, k -> new HashMap<>()).put(dayIndex, slots);
    }

    /**
     * 특정 요일에 참여한 유저 수
     */
    public int getParticipantCount(int dayIndex) {
        int count = 0;
        for (Map<Integer, Set<Integer>> dayMap : userSelections.values()) {
            if (dayMap.containsKey(dayIndex) && !dayMap.get(dayIndex).isEmpty()) count++;
        }
        return count;
    }

    /**
     * 특정 요일의 모든 유저 슬롯 교집합 반환 (참여자 없으면 빈 Set)
     */
    public Set<Integer> getIntersection(int dayIndex) {
        List<Set<Integer>> slotsPerUser = new ArrayList<>();
        for (Map<Integer, Set<Integer>> dayMap : userSelections.values()) {
            Set<Integer> slots = dayMap.get(dayIndex);
            if (slots != null && !slots.isEmpty()) {
                slotsPerUser.add(slots);
            }
        }
        if (slotsPerUser.isEmpty()) return Collections.emptySet();

        Set<Integer> intersection = new TreeSet<>(slotsPerUser.get(0));
        for (int i = 1; i < slotsPerUser.size(); i++) {
            intersection.retainAll(slotsPerUser.get(i));
        }
        return intersection;
    }

    /**
     * 슬롯 인덱스 → "HH:mm" 문자열 변환
     */
    public static String slotToTime(int slot) {
        int hour = slot / 2;
        int min = (slot % 2) * 30;
        return String.format("%02d:%02d", hour, min);
    }

    /**
     * 연속된 슬롯을 "HH:mm ~ HH:mm" 범위 문자열 리스트로 변환
     */
    public static List<String> slotsToRanges(Set<Integer> slots) {
        if (slots.isEmpty()) return Collections.emptyList();
        List<Integer> sorted = new ArrayList<>(new TreeSet<>(slots));
        List<String> ranges = new ArrayList<>();

        int start = sorted.get(0);
        int prev = sorted.get(0);

        for (int i = 1; i < sorted.size(); i++) {
            int cur = sorted.get(i);
            if (cur == prev + 1) {
                prev = cur;
            } else {
                // prev+1 슬롯의 시작이 끝 시간
                ranges.add(slotToTime(start) + " ~ " + slotToTime(prev + 1));
                start = cur;
                prev = cur;
            }
        }
        ranges.add(slotToTime(start) + " ~ " + slotToTime(prev + 1));
        return ranges;
    }
}
