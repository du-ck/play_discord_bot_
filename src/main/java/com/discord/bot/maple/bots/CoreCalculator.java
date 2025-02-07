package com.discord.bot.maple.bots;

public class CoreCalculator {

    private final int[] SKILL_CORE_COSTS = {100, 30, 35, 40, 45, 50, 55, 60, 65, 200, 80, 90, 100, 110, 120, 130, 140, 150, 160, 350, 170, 180, 190, 200, 210, 220, 230, 240, 250, 500};
    private final int[] MASTERY_CORE_COSTS = {50, 15, 18, 20, 23, 25, 28, 30, 33, 100, 40, 45, 50, 55, 60, 65, 70, 75, 80, 175, 85, 90, 95, 100, 105, 110, 115, 120, 125, 250};
    private final int[] ENHANCEMENT_CORE_COSTS = {75, 23, 27, 30, 34, 38, 42, 45, 49, 150, 60, 68, 75, 83, 90, 98, 105, 113, 120, 263, 128, 135, 143, 150, 158, 165, 173, 180, 188, 375};
    private final int[] SHARED_CORE_COSTS = {125, 38, 44, 50, 57, 63, 69, 75, 82, 300, 110, 124, 138, 152, 165, 179, 193, 207, 220, 525, 234, 248, 262, 275, 289, 303, 317, 330, 344, 750};

    private static CoreCalculator instance;
    private CoreCalculator() {

    }

    public static CoreCalculator getInstance() {
        if (instance == null) {
            instance = new CoreCalculator();
        }
        return instance;
    }

    public int calculatePiece(String coreType, int currentLevel, int targetLevel) {
        int piece = 0;
        int[] costs = new int[30];
        switch (coreType) {
            case "강화코어":
                costs = ENHANCEMENT_CORE_COSTS;
                break;
            case "마스터리코어":
                costs = MASTERY_CORE_COSTS;
                break;
            case "스킬코어":
                costs = SKILL_CORE_COSTS;
                break;
            case "공용코어":
                costs = SHARED_CORE_COSTS;
                break;
        }
        for (int i = currentLevel; i < targetLevel; i++) {
            piece += costs[i];
        }
        return piece;
    }
}
