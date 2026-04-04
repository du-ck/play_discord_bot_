package com.discord.bot.maple.bots;

public class FragmentCalculator {

    private static FragmentCalculator instance;

    public static FragmentCalculator getInstance() {
        if (instance == null) {
            instance = new FragmentCalculator();
        }
        return instance;
    }

    /**
     * 1소재 조각 기대값
     * @param killCount 6분단위 마릿수
     * @param itemDropRatePct 아이템드롭률
     * @param totalNeedFragment 필요한 조각 개수
     * @return
     */
    public int calculateFragments(int killCount, int itemDropRatePct, int totalNeedFragment) {
        double fragmentDropRate = 0.000425; //조각 드랍률
        double itemDropRate = (double)itemDropRatePct / 100;

        double fragmentsCount = (killCount * 5) * fragmentDropRate * (1 + Math.log(1 + itemDropRate));

        double cycle = totalNeedFragment / fragmentsCount;
        double roundedCycles = Math.round(cycle);

        return (int)roundedCycles;
    }


}
