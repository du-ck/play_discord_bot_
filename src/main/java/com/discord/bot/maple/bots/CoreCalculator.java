package com.discord.bot.maple.bots;

import lombok.Builder;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class CoreCalculator {

    private final int[] SKILL_CORE_SOL_COSTS = {5,1,1,1,2,2,2,3,3,10,3,3,4,4,4,4,4,4,5,15,5,5,5,5,5,6,6,6,7,20};
    private final int[] SKILL_CORE_FRAGMENT_COSTS = {100, 30, 35, 40, 45, 50, 55, 60, 65, 200, 80, 90, 100, 110, 120, 130, 140, 150, 160, 350, 170, 180, 190, 200, 210, 220, 230, 240, 250, 500};

    private final int[] MASTERY_CORE_SOL_COSTS = {3,1,1,1,1,1,1,2,2,5,2,2,2,2,2,2,2,2,3,8,3,3,3,3,3,3,3,3,4,10};
    private final int[] MASTERY_CORE_FRAGMENT_COSTS = {50, 15, 18, 20, 23, 25, 28, 30, 33, 100, 40, 45, 50, 55, 60, 65, 70, 75, 80, 175, 85, 90, 95, 100, 105, 110, 115, 120, 125, 250};

    private final int[] ENHANCEMENT_CORE_SOL_COSTS = {4,1,1,1,2,2,2,3,3,8,3,3,3,3,3,3,3,3,4,12,4,4,4,4,4,5,5,5,6,15};
    private final int[] ENHANCEMENT_CORE_FRAGMENT_COSTS = {75, 23, 27, 30, 34, 38, 42, 45, 49, 150, 60, 68, 75, 83, 90, 98, 105, 113, 120, 263, 128, 135, 143, 150, 158, 165, 173, 180, 188, 375};

    private final int[] SHARED_CORE_SOL_COSTS = {7,2,2,2,3,3,3,5,5,14,5,5,6,6,6,6,6,6,7,17,7,7,7,7,7,9,9,9,10,20};
    private final int[] SHARED_CORE_FRAGMENT_COSTS = {125, 38, 44, 50, 57, 63, 69, 75, 82, 300, 110, 124, 138, 152, 165, 179, 193, 207, 220, 525, 234, 248, 262, 275, 289, 303, 317, 330, 344, 750};

    private static CoreCalculator instance;
    private CoreCalculator() {

    }

    public static CoreCalculator getInstance() {
        if (instance == null) {
            instance = new CoreCalculator();
        }
        return instance;
    }

    public Map.Entry<Integer, Integer> calculatePiece(String coreType, int currentLevel, int targetLevel) {

        int sols = 0;
        int fragments = 0;

        int[] solCosts = new int[30];
        int[] fragmentCosts = new int[30];

        switch (coreType) {
            case "강화코어":
                solCosts = ENHANCEMENT_CORE_SOL_COSTS;
                fragmentCosts = ENHANCEMENT_CORE_FRAGMENT_COSTS;
                break;
            case "마스터리코어":
                solCosts = MASTERY_CORE_SOL_COSTS;
                fragmentCosts = MASTERY_CORE_FRAGMENT_COSTS;
                break;
            case "스킬코어":
                solCosts = SKILL_CORE_SOL_COSTS;
                fragmentCosts = SKILL_CORE_FRAGMENT_COSTS;
                break;
            case "공용코어":
                solCosts = SHARED_CORE_SOL_COSTS;
                fragmentCosts = SHARED_CORE_FRAGMENT_COSTS;
                break;
        }
        for (int i = currentLevel; i < targetLevel; i++) {
            sols += solCosts[i];
            fragments += fragmentCosts[i];
        }
        return new AbstractMap.SimpleEntry<>(sols, fragments);
    }
}
