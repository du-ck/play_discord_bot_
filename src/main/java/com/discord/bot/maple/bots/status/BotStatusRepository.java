package com.discord.bot.maple.bots.status;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BotStatusRepository extends JpaRepository<BotStatusEntity, Long> {

    /** 저장된 상태 문구. 없으면 기본 문구를 돌려준다. */
    default String currentText() {
        return findById(BotStatusEntity.SINGLETON_ID)
                .map(BotStatusEntity::getText)
                .orElse(BotStatusEntity.DEFAULT_STATUS);
    }
}
