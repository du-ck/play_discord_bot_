package com.discord.bot.maple.bots.status;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "bot_status")
@Getter @Setter
@NoArgsConstructor
public class BotStatusEntity {

    /** 봇 상태는 하나뿐이므로 항상 이 PK 한 행만 사용한다 */
    public static final Long SINGLETON_ID = 1L;

    public static final String DEFAULT_STATUS = "다음주면 LA콘이구나....";

    /** Discord 커스텀 상태 길이 제한 */
    public static final int MAX_LENGTH = 128;

    @Id
    private Long id = SINGLETON_ID;

    @Column(nullable = false, length = MAX_LENGTH)
    private String text;

    @Column
    private String updatedBy;   // 변경한 디스코드 유저 ID

    @Column
    private LocalDateTime updatedAt;

    public BotStatusEntity(String text, String updatedBy) {
        this.id = SINGLETON_ID;
        this.text = text;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }
}
