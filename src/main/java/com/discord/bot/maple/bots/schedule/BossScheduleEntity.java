package com.discord.bot.maple.bots.schedule;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "boss_schedule")
@Getter @Setter
@NoArgsConstructor
public class BossScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String threadId;   // 디스코드 쓰레드 ID

    @Column(nullable = false)
    private String bossName;   // 보스 이름

    @Column(nullable = false)
    private String userId;     // 디스코드 유저 ID

    @Column(nullable = false)
    private int dayIndex;      // 요일 (0=월 ~ 6=일)

    // 선택한 슬롯들을 콤마로 이어서 저장 ex) "40,41,42,43"
    @Column(length = 200)
    private String slots;

    // 확정 정보 (파티장이 확정한 요일/시간, 해당 쓰레드의 대표 row 1개에만 저장)
    @Column
    private Integer confirmedDay;    // 확정 요일 (0=월~6=일), null이면 미확정

    @Column
    private String confirmedTime;    // 확정 시간 ex) "21:00"

    public BossScheduleEntity(String threadId, String bossName, String userId, int dayIndex, String slots) {
        this.threadId = threadId;
        this.bossName = bossName;
        this.userId   = userId;
        this.dayIndex = dayIndex;
        this.slots    = slots;
    }
}
