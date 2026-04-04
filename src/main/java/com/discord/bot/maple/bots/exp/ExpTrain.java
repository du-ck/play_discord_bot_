package com.discord.bot.maple.bots.exp;

import com.discord.bot.maple.bots.Util;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class ExpTrain {

    private static List<ExpTrain> instance;

    private ExpTrain() {

    }

    public static List<ExpTrain> getInstance() {
        if (instance == null) {
            instance = new ArrayList<>();
        }
        return instance;
    }

    private String expDate;
    private long nowCount;
    private long totalCount;
}
