package com.discord.bot.maple.bots.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BossScheduleRepository extends JpaRepository<BossScheduleEntity, Long> {

    // 특정 쓰레드의 모든 선택 데이터
    List<BossScheduleEntity> findByThreadId(String threadId);

    // 특정 쓰레드 + 유저 + 요일 (upsert용)
    Optional<BossScheduleEntity> findByThreadIdAndUserIdAndDayIndex(String threadId, String userId, int dayIndex);

    // 특정 쓰레드 전체 삭제 (초기화용)
    void deleteByThreadId(String threadId);
}
