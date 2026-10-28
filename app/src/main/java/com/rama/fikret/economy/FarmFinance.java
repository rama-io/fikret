package com.rama.fikret.economy;

/**
 * A read-only snapshot of what one farm earns right now, split by where the income comes from.
 *
 * Only gardens with a supervisor count as income: the others earn only when tapped. Their
 * would-be income is kept apart as {@link #idleIncome}.
 *
 * The income of a garden is {@code base * upgrades * milestones * points}. These factors multiply,
 * so the part above the base is shared out by the log of each factor: a factor that doubled the
 * income twice as much as another gets twice the share. The four sources always add up to
 * {@link #income}.
 */
public final class FarmFinance {
    public static final int SOURCE_BASE = 0;
    public static final int SOURCE_UPGRADES = 1;
    public static final int SOURCE_MILESTONES = 2;
    public static final int SOURCE_POINTS = 3;
    public static final int SOURCES = 4;

    private static final double LN2 = Math.log(2);

    public final int world;
    /** Money per second from supervised gardens. */
    public final double income;
    public final double[] bySource = new double[SOURCES];
    public final double[] byGarden = new double[Worlds.GARDENS_PER_WORLD];
    /** Money per second the gardens without a supervisor would add if they had one. */
    public final double idleIncome;
    /** {@link #income} converted to Lake money, so farms with different currencies can be added. */
    public final double lakeIncome;
    public final double lifetime;

    public final double points;
    public final double pointsBonusPercent;
    public final double pendingPoints;
    /** How much profit grows by when ascending now: 1.1 means +10%. */
    public final double ascendFactor;

    /** The upgrade or supervisor that pays itself back fastest, or null when there is none. */
    public final String bestBuy;
    public final double bestBuyPayback;
    /** Seconds until the best buy is affordable at the current income; 0 when it already is. */
    public final double bestBuyWait;

    private FarmFinance(GameState s, int w) {
        world = w;
        double pointsMultiplier = s.angelBonus(w);
        double lnPoints = Math.log(pointsMultiplier);
        double total = 0;
        double idle = 0;
        double money = s.getMoney(w);

        String bestName = null;
        double bestCost = 0;
        double bestGain = 0;
        double bestRatio = Double.POSITIVE_INFINITY;

        for (int g = 0; g < Worlds.GARDENS_PER_WORLD; g++) {
            int count = s.getCount(w, g);
            if (count <= 0) {
                continue;
            }
            Worlds.GardenDef def = Worlds.ALL[w].gardens[g];
            double potential = s.revenuePerSecond(w, g);

            if (!s.hasManager(w, g)) {
                idle += potential;
                double cost = s.managerCost(w, g);
                if (cost / potential < bestRatio) {
                    bestRatio = cost / potential;
                    bestName = def.name + " supervisor";
                    bestCost = cost;
                    bestGain = potential;
                }
                continue;
            }

            byGarden[g] = potential;
            total += potential;

            double base = def.baseRevenue * count / def.cycleSeconds;
            double lnUpgrades = (s.upgradeLevel(w, g, GameState.UPGRADE_SPEED)
                    + s.upgradeLevel(w, g, GameState.UPGRADE_VALUE)) * LN2;
            double lnMilestones = GameState.milestonesReached(count) * LN2;
            double lnAll = lnUpgrades + lnMilestones + lnPoints;
            double extra = Math.max(0, potential - base);
            if (lnAll > 0) {
                bySource[SOURCE_UPGRADES] += extra * lnUpgrades / lnAll;
                bySource[SOURCE_MILESTONES] += extra * lnMilestones / lnAll;
                bySource[SOURCE_POINTS] += extra * lnPoints / lnAll;
            }

            for (int type = 0; type < 2; type++) {
                if (s.upgradeMaxed(w, g, type)) {
                    continue;
                }
                double cost = s.upgradeCost(w, g, type);
                // Both kinds of upgrade double this garden's income.
                if (cost / potential < bestRatio) {
                    bestRatio = cost / potential;
                    bestName = def.name + (type == GameState.UPGRADE_SPEED ? " Speed x2" : " Value x2");
                    bestCost = cost;
                    bestGain = potential;
                }
            }
        }
        // The base slice is what is left once the other three are taken out, so the four always add up.
        bySource[SOURCE_BASE] = total - bySource[SOURCE_UPGRADES] - bySource[SOURCE_MILESTONES]
                - bySource[SOURCE_POINTS];

        income = total;
        idleIncome = idle;
        lakeIncome = total * Worlds.START_MONEY / Worlds.ALL[w].startMoney;
        lifetime = s.getLifetimeEarnings(w);

        points = s.getAngels(w);
        pointsBonusPercent = (pointsMultiplier - 1) * 100;
        pendingPoints = s.pendingAngels(w);
        ascendFactor = (1 + Worlds.ANGEL_BONUS * (points + pendingPoints)) / pointsMultiplier;

        bestBuy = bestName;
        bestBuyPayback = bestGain > 0 ? bestCost / bestGain : Double.POSITIVE_INFINITY;
        bestBuyWait = bestName == null ? 0
                : money >= bestCost ? 0
                : total > 0 ? (bestCost - money) / total : Double.POSITIVE_INFINITY;
    }

    public static FarmFinance of(GameState state, int world) {
        return new FarmFinance(state, world);
    }

    /** Garden indexes with income, biggest first. */
    public int[] gardensByIncome() {
        int n = 0;
        int[] order = new int[byGarden.length];
        for (int g = 0; g < byGarden.length; g++) {
            if (byGarden[g] > 0) {
                order[n++] = g;
            }
        }
        for (int i = 1; i < n; i++) {
            int key = order[i];
            int j = i - 1;
            while (j >= 0 && byGarden[order[j]] < byGarden[key]) {
                order[j + 1] = order[j];
                j--;
            }
            order[j + 1] = key;
        }
        return java.util.Arrays.copyOf(order, n);
    }
}
