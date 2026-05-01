package com.discord.bot.maple.bots.exp;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 연뿌(연속 사냥 열차) 데이터를 애플리케이션 전체에서 공유하는 Spring 빈
 */
@Component
public class ExpTrainHolder {

    private final List<ExpTrain> trains = new ArrayList<>();

    public List<ExpTrain> getTrains() {
        return trains;
    }
}
