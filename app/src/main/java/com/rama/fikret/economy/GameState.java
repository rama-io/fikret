package com.rama.fikret.economy;

import android.content.Context;

import com.rama.fikret.game.Ability;
import com.rama.fikret.helpers.DeveloperMode;
import com.rama.fikret.managers.PrefsManager;

import java.util.Locale;

public final class GameState {
    public static final int BUY_MAX = -1;
    public static final int BUY_NEXT = -2;
    public static final int UPGRADE_SPEED = 0;
    public static final int UPGRADE_VALUE = 1;
    public static final int UPGRADE_PROFIT = 2;

    private static final String PREF_KEY = "economy:state";
    private static final int MAX_SYNC_STEPS = 1000;
    private static final double MIN_SYNC_STEP_SECONDS = 1;

    private static GameState instance;

    public static synchronized GameState get(Context context) {
        if (instance == null) {
            instance = new GameState(context.getApplicationContext());
        }
        return instance;
    }

    private static final class Garden {
        int count;
        boolean manager;
        boolean accountant;
        boolean discount;
        int speedLevel;
        int valueLevel;
        double elapsed;
        boolean running;
    }

    private static final class World {
        final Garden[] gardens = new Garden[Worlds.GARDENS_PER_WORLD];
        boolean travelManager;
        boolean buyer;
        boolean quickBuy;
        int buyerGarden = -1;

        World() {
            for (int i = 0; i < gardens.length; i++) {
                gardens[i] = new Garden();
            }
        }
    }

    public static final class Quote {
        public final int amount;
        public final double cost;

        Quote(int amount, double cost) {
            this.amount = amount;
            this.cost = cost;
        }
    }

    private final PrefsManager prefs;
    private final World[] worlds = new World[Worlds.ALL.length];
    private final double[] money = new double[Worlds.ALL.length];
    private final double[] angels = new double[Worlds.ALL.length];
    private final double[] lifetimeEarnings = new double[Worlds.ALL.length];
    private final double[] angelsClaimed = new double[Worlds.ALL.length];
    private final long[] superBought = new long[Worlds.ALL.length];
    private final double[] superProfit = new double[Worlds.ALL.length];
    private final boolean[][] profitBought = new boolean[Worlds.ALL.length][Worlds.PROFIT_UPGRADE_COUNT];
    private final boolean[][] efficiencyBought = new boolean[Worlds.ALL.length][Worlds.EFFICIENCY_BIRD_COUNT];
    private final double[] upgradeProfit = new double[Worlds.ALL.length];
    private final double[] angelRate = new double[Worlds.ALL.length];
    private long lastSync;
    private int currentWorld = 0;

    private GameState(Context context) {
        prefs = PrefsManager.getInstance(context);
        for (int i = 0; i < worlds.length; i++) {
            worlds[i] = new World();
            money[i] = Worlds.ALL[i].startMoney;
        }
        load();
        for (int i = 0; i < worlds.length; i++) {
            refreshSuper(i);
        }
        if (lastSync <= 0) {
            lastSync = System.currentTimeMillis();
        }
        sync();
    }

    public synchronized void sync() {
        long now = System.currentTimeMillis();
        double dt = (now - lastSync) / 1000.0;
        lastSync = now;
        if (dt <= 0) {
            return;
        }
        int steps = (int) Math.min(MAX_SYNC_STEPS, Math.max(1, Math.ceil(dt / MIN_SYNC_STEP_SECONDS)));
        double stepSeconds = dt / steps;
        boolean bought = false;
        for (int i = 0; i < steps; i++) {
            for (int w = 0; w < worlds.length; w++) {
                for (int g = 0; g < Worlds.GARDENS_PER_WORLD; g++) {
                    advance(w, g, stepSeconds);
                }
            }
            for (int w = 0; w < worlds.length; w++) {
                bought |= runBuyer(w);
            }
        }
        if (bought) {
            save();
        }
    }

    private boolean runBuyer(int w) {
        World world = worlds[w];
        int g = world.buyerGarden;
        if (!world.buyer || g < 0) {
            return false;
        }
        Quote units = quote(w, g, BUY_MAX);
        if (units.amount <= 0 || units.cost > money[w]) {
            return false;
        }
        money[w] -= units.cost;
        world.gardens[g].count += units.amount;
        return true;
    }

    private void advance(int w, int g, double dt) {
        Garden garden = worlds[w].gardens[g];
        if (garden.count <= 0) {
            return;
        }
        if (!garden.manager && !garden.running) {
            return;
        }
        double cycle = cycleSeconds(w, g);
        garden.elapsed += dt;
        if (garden.elapsed < cycle) {
            return;
        }
        double revenue = revenuePerCycle(w, g);
        if (garden.manager) {
            double cycles = Math.floor(garden.elapsed / cycle);
            earn(w, cycles * revenue);
            garden.elapsed -= cycles * cycle;
        } else {
            earn(w, revenue);
            garden.elapsed = 0;
            garden.running = false;
        }
    }

    private void earn(int w, double amount) {
        money[w] += amount;
        lifetimeEarnings[w] += amount;
    }

    public synchronized double getMoney(int w) {
        return money[w];
    }

    public synchronized double getAngels(int w) {
        return angels[w];
    }

    public int getCount(int w, int g) {
        return worlds[w].gardens[g].count;
    }

    public boolean isRunning(int w, int g) {
        Garden garden = worlds[w].gardens[g];
        return garden.count > 0 && (garden.manager || garden.running);
    }

    public double progress(int w, int g) {
        Garden garden = worlds[w].gardens[g];
        if (garden.count <= 0 || !(garden.manager || garden.running)) {
            return 0;
        }
        double cycle = cycleSeconds(w, g);
        if (cycle < 0.25) {
            return 1;
        }
        return Math.min(1, garden.elapsed / cycle);
    }

    public double timeLeft(int w, int g) {
        Garden garden = worlds[w].gardens[g];
        if (garden.count <= 0 || !(garden.manager || garden.running)) {
            return 0;
        }
        return Math.max(0, cycleSeconds(w, g) - garden.elapsed);
    }

    public double cycleSeconds(int w, int g) {
        Worlds.GardenDef def = Worlds.ALL[w].gardens[g];
        return def.cycleSeconds / speedMultiplier(w, g);
    }

    public double speedMultiplier(int w, int g) {
        Garden garden = worlds[w].gardens[g];
        return Math.pow(2, garden.speedLevel + milestonesReached(garden.count));
    }

    public double valueMultiplier(int w, int g) {
        return Math.pow(2, worlds[w].gardens[g].valueLevel) * upgradeProfit[w];
    }

    public double upgradeProfitMultiplier(int w) {
        return upgradeProfit[w];
    }

    public double revenuePerCycle(int w, int g) {
        Garden garden = worlds[w].gardens[g];
        return Worlds.ALL[w].gardens[g].baseRevenue * garden.count * valueMultiplier(w, g)
                * angelBonus(w) * superProfit[w];
    }

    public double revenuePerSecond(int w, int g) {
        return revenuePerCycle(w, g) / cycleSeconds(w, g);
    }

    public synchronized double angelBonus(int w) {
        return 1 + angelRate[w] * angels[w];
    }

    /** Profit multiplier from the farm point upgrades bought on this farm. */
    public synchronized double superProfitMultiplier(int w) {
        return superProfit[w];
    }

    /** Profit given by each farm point, 0.02 = 2%. */
    public synchronized double pointsEfficiency(int w) {
        return angelRate[w];
    }

    private void refreshSuper(int w) {
        double profit = 1;
        double rate = Worlds.ANGEL_BONUS;
        for (int i = 0; i < Worlds.SUPER_UPGRADE_COUNT; i++) {
            if ((superBought[w] & (1L << i)) == 0) continue;
            if (Worlds.superKind(i) == Worlds.SUPER_PROFIT) {
                profit *= Worlds.superValue(i);
            } else {
                rate += Worlds.superValue(i);
            }
        }
        superProfit[w] = profit;
        double upgrades = 1;
        for (int i = 0; i < Worlds.PROFIT_UPGRADE_COUNT; i++) {
            if (profitBought[w][i]) upgrades *= Worlds.profitUpgradeValue(i);
        }
        for (int i = 0; i < Worlds.EFFICIENCY_BIRD_COUNT; i++) {
            if (efficiencyBought[w][i]) rate += Worlds.efficiencyBirdValue(i);
        }
        upgradeProfit[w] = upgrades;
        angelRate[w] = rate;
    }

    public boolean hasProfitUpgrade(int w, int index) {
        return profitBought[w][index];
    }

    public double profitUpgradeCost(int w, int index) {
        return Worlds.ALL[w].profitUpgradeCosts[index];
    }

    public boolean hasRemainingProfitUpgrades(int w) {
        for (int i = 0; i < Worlds.PROFIT_UPGRADE_COUNT; i++) {
            if (!profitBought[w][i]) return true;
        }
        return false;
    }

    public synchronized boolean buyProfitUpgrade(int w, int index) {
        sync();
        if (index < 0 || index >= Worlds.PROFIT_UPGRADE_COUNT || profitBought[w][index]) {
            return false;
        }
        double cost = profitUpgradeCost(w, index);
        if (money[w] < cost) {
            return false;
        }
        money[w] -= cost;
        profitBought[w][index] = true;
        refreshSuper(w);
        save();
        return true;
    }

    public boolean hasEfficiencyBird(int w, int index) {
        return efficiencyBought[w][index];
    }

    public double efficiencyBirdCost(int w, int index) {
        return Worlds.ALL[w].efficiencyBirdCosts[index];
    }

    public boolean hasRemainingEfficiencyBirds(int w) {
        for (int i = 0; i < Worlds.EFFICIENCY_BIRD_COUNT; i++) {
            if (!efficiencyBought[w][i]) return true;
        }
        return false;
    }

    public synchronized boolean hireEfficiencyBird(int w, int index) {
        sync();
        if (index < 0 || index >= Worlds.EFFICIENCY_BIRD_COUNT || efficiencyBought[w][index]) {
            return false;
        }
        double cost = efficiencyBirdCost(w, index);
        if (money[w] < cost) {
            return false;
        }
        money[w] -= cost;
        efficiencyBought[w][index] = true;
        refreshSuper(w);
        save();
        return true;
    }

    public boolean hasSuperUpgrade(int w, int index) {
        return (superBought[w] & (1L << index)) != 0;
    }

    public double superUpgradeCost(int index) {
        return Worlds.superCost(index);
    }

    public boolean hasRemainingSuperUpgrades(int w) {
        for (int i = 0; i < Worlds.SUPER_UPGRADE_COUNT; i++) {
            if (!hasSuperUpgrade(w, i)) return true;
        }
        return false;
    }

    public synchronized boolean buySuperUpgrade(int w, int index) {
        sync();
        if (index < 0 || index >= Worlds.SUPER_UPGRADE_COUNT || hasSuperUpgrade(w, index)) {
            return false;
        }
        double cost = Worlds.superCost(index);
        if (angels[w] < cost) {
            return false;
        }
        angels[w] -= cost;
        superBought[w] |= 1L << index;
        refreshSuper(w);
        save();
        return true;
    }

    public static double angelsForEarnings(int w, double lifetime) {
        double lakeMoney = Math.max(0, lifetime) * Worlds.START_MONEY / Worlds.ALL[w].startMoney;
        return Math.floor(Math.sqrt(lakeMoney / Worlds.ANGEL_DIVISOR));
    }

    public synchronized double pendingAngels(int w) {
        return Math.max(0, angelsForEarnings(w, lifetimeEarnings[w]) - angelsClaimed[w]);
    }

    public synchronized boolean ascend(int w) {
        sync();
        double gain = pendingAngels(w);
        if (gain <= 0) {
            return false;
        }
        angels[w] += gain;
        angelsClaimed[w] += gain;
        money[w] = Worlds.ALL[w].startMoney;
        for (Garden garden : worlds[w].gardens) {
            garden.count = 0;
            garden.speedLevel = 0;
            garden.valueLevel = 0;
            garden.elapsed = 0;
            garden.running = false;
        }
        save();
        return true;
    }

    public static int milestonesReached(int count) {
        int m = 0;
        if (count >= 25) m++;
        if (count >= 50) m++;
        if (count >= 100) m++;
        if (count >= 200) m += count / 100 - 1;
        return m;
    }

    public static int nextMilestone(int count) {
        if (count < 25) return 25;
        if (count < 50) return 50;
        if (count < 100) return 100;
        return (count / 100 + 1) * 100;
    }

    public double priceMultiplier(int w, int g) {
        Garden garden = worlds[w].gardens[g];
        double m = 1;
        if (garden.accountant) {
            m *= Worlds.ACCOUNTANT_COST_FACTOR;
        }
        if (garden.discount) {
            m /= Worlds.DISCOUNT_COST_DIVISOR;
        }
        return m;
    }

    public synchronized Quote quote(int w, int g, int amount) {
        Worlds.GardenDef def = Worlds.ALL[w].gardens[g];
        int owned = worlds[w].gardens[g].count;
        double first = def.baseCost * priceMultiplier(w, g) * Math.pow(def.coefficient, owned);
        double ratio = def.coefficient;

        if (amount == BUY_NEXT) {
            amount = nextMilestone(owned) - owned;
        } else if (amount == BUY_MAX) {
            double n = Math.floor(Math.log(money[w] * (ratio - 1) / first + 1) / Math.log(ratio));
            if (Double.isNaN(n) || n < 0) {
                n = 0;
            }
            amount = (int) Math.min(n, 100000);
        }
        if (amount <= 0) {
            return new Quote(0, first);
        }
        double cost = first * (Math.pow(ratio, amount) - 1) / (ratio - 1);
        return new Quote(amount, cost);
    }

    public synchronized boolean buy(int w, int g, int amount) {
        sync();
        Quote q = quote(w, g, amount);
        if (q.amount <= 0 || q.cost > money[w]) {
            return false;
        }
        money[w] -= q.cost;
        worlds[w].gardens[g].count += q.amount;
        save();
        return true;
    }

    public synchronized boolean harvest(int w, int g) {
        sync();
        Garden garden = worlds[w].gardens[g];
        if (garden.count <= 0 || garden.manager || garden.running) {
            return false;
        }
        garden.running = true;
        garden.elapsed = 0;
        return true;
    }

    public boolean hasManager(int w, int g) {
        return worlds[w].gardens[g].manager;
    }

    public double managerCost(int w, int g) {
        return Worlds.ALL[w].managerCosts[g];
    }

    public synchronized boolean hireManager(int w, int g) {
        sync();
        Garden garden = worlds[w].gardens[g];
        double cost = managerCost(w, g);
        if (garden.manager || money[w] < cost) {
            return false;
        }
        money[w] -= cost;
        garden.manager = true;
        if (!garden.running) {
            garden.elapsed = 0;
        }
        unlockAbilityFor(w);
        save();
        return true;
    }

    private void unlockAbilityFor(int w) {
        Ability ability = Ability.forStage(Worlds.ALL[w].stageId);
        if (ability != null) {
            prefs.unlockAbility(ability);
        }
    }

    public boolean hasTravelManager(int w) {
        return DeveloperMode.ENABLED || worlds[w].travelManager;
    }

    public boolean isFarmUnlocked(int w) {
        if (!DeveloperMode.isFarmAvailable(w)) {
            return false;
        }
        return w == 0 || DeveloperMode.ENABLED || worlds[w - 1].travelManager;
    }

    public int unlockedFarmCount() {
        int count = 0;
        for (int w = 0; w < Worlds.ALL.length; w++) {
            if (isFarmUnlocked(w)) count++;
        }
        return count;
    }

    public double travelManagerCost(int w) {
        return Worlds.ALL[w].travelManagerCost;
    }

    public synchronized boolean hireTravelManager(int w) {
        sync();
        World world = worlds[w];
        double cost = travelManagerCost(w);
        if (world.travelManager || money[w] < cost) {
            return false;
        }
        money[w] -= cost;
        world.travelManager = true;
        save();
        return true;
    }

    public boolean hasBuyer(int w) {
        return worlds[w].buyer;
    }

    public double buyerCost(int w) {
        return Worlds.ALL[w].buyerCost;
    }

    public synchronized boolean hireBuyer(int w) {
        sync();
        World world = worlds[w];
        double cost = buyerCost(w);
        if (world.buyer || money[w] < cost) {
            return false;
        }
        money[w] -= cost;
        world.buyer = true;
        world.buyerGarden = -1;
        save();
        return true;
    }

    public boolean hasQuickBuy(int w) {
        return worlds[w].quickBuy;
    }

    public double quickBuyCost(int w) {
        return Worlds.ALL[w].quickBuyCost;
    }

    public synchronized boolean hireQuickBuy(int w) {
        sync();
        World world = worlds[w];
        double cost = quickBuyCost(w);
        if (world.quickBuy || money[w] < cost) {
            return false;
        }
        money[w] -= cost;
        world.quickBuy = true;
        save();
        return true;
    }

    public boolean hasRemainingSupervisors(int w) {
        World world = worlds[w];
        if (!world.travelManager || !world.buyer || !world.quickBuy) {
            return true;
        }
        for (int g = 0; g < Worlds.GARDENS_PER_WORLD; g++) {
            if (!hasManager(w, g)) return true;
            if (hasDiscountList(w) && !hasDiscount(w, g)) return true;
        }
        return hasRemainingEfficiencyBirds(w);
    }

    /** Anything still to buy with farm points: accountants and super upgrades. */
    public boolean hasRemainingSuperSupervisors(int w) {
        if (hasRemainingSuperUpgrades(w)) {
            return true;
        }
        if (hasAccountantList(w)) {
            for (int g = 0; g < Worlds.GARDENS_PER_WORLD; g++) {
                if (!hasAccountant(w, g)) return true;
            }
        }
        return false;
    }

    public boolean hasRemainingUpgrades(int w) {
        if (hasRemainingProfitUpgrades(w)) {
            return true;
        }
        for (int g = 0; g < Worlds.GARDENS_PER_WORLD; g++) {
            if (!upgradeMaxed(w, g, UPGRADE_SPEED) || !upgradeMaxed(w, g, UPGRADE_VALUE)) {
                return true;
            }
        }
        return false;
    }

    public synchronized int buyAllUpgrades(int w) {
        sync();
        if (!worlds[w].quickBuy) {
            return 0;
        }
        int bought = 0;
        while (true) {
            Garden best = null;
            int bestProfit = -1;
            boolean bestSpeed = false;
            double bestCost = Double.MAX_VALUE;
            for (int g = 0; g < Worlds.GARDENS_PER_WORLD; g++) {
                Garden garden = worlds[w].gardens[g];
                if (garden.count <= 0) continue;
                for (int type = 0; type < 2; type++) {
                    boolean speed = type == UPGRADE_SPEED;
                    int level = speed ? garden.speedLevel : garden.valueLevel;
                    if (level >= Worlds.UPGRADE_MAX_LEVEL) continue;
                    double cost = Worlds.upgradePrice(w, level, speed, g);
                    if (cost <= money[w] && cost < bestCost) {
                        best = garden;
                        bestSpeed = speed;
                        bestCost = cost;
                    }
                }
            }
            for (int i = 0; i < Worlds.PROFIT_UPGRADE_COUNT; i++) {
                if (profitBought[w][i]) continue;
                double cost = profitUpgradeCost(w, i);
                if (cost <= money[w] && cost < bestCost) {
                    best = null;
                    bestProfit = i;
                    bestCost = cost;
                }
            }
            if (best == null && bestProfit < 0) break;
            money[w] -= bestCost;
            if (bestProfit >= 0) {
                profitBought[w][bestProfit] = true;
                refreshSuper(w);
            } else if (bestSpeed) best.speedLevel++; else best.valueLevel++;
            bought++;
        }
        if (bought > 0) save();
        return bought;
    }

    public int getBuyerGarden(int w) {
        return worlds[w].buyer ? worlds[w].buyerGarden : -1;
    }

    public synchronized void toggleBuyer(int w, int g) {
        World world = worlds[w];
        if (!world.buyer) {
            return;
        }
        world.buyerGarden = world.buyerGarden == g ? -1 : g;
        save();
    }

    public boolean hasAccountantList(int w) {
        return Worlds.ALL[w].accountantCosts != null;
    }

    public boolean hasDiscountList(int w) {
        return Worlds.ALL[w].discountCosts != null;
    }

    public boolean hasAccountant(int w, int g) {
        return worlds[w].gardens[g].accountant;
    }

    public boolean hasDiscount(int w, int g) {
        return worlds[w].gardens[g].discount;
    }

    public double accountantCost(int w, int g) {
        return Worlds.ALL[w].accountantCosts[g];
    }

    public double discountCost(int w, int g) {
        return Worlds.ALL[w].discountCosts[g];
    }

    public synchronized boolean hireAccountant(int w, int g) {
        sync();
        Garden garden = worlds[w].gardens[g];
        if (!hasAccountantList(w) || garden.accountant) {
            return false;
        }
        double cost = accountantCost(w, g);
        if (angels[w] < cost) {
            return false;
        }
        angels[w] -= cost;
        garden.accountant = true;
        save();
        return true;
    }

    public synchronized boolean hireDiscountManager(int w, int g) {
        sync();
        Garden garden = worlds[w].gardens[g];
        if (!hasDiscountList(w) || garden.discount) {
            return false;
        }
        double cost = discountCost(w, g);
        if (money[w] < cost) {
            return false;
        }
        money[w] -= cost;
        garden.discount = true;
        save();
        return true;
    }

    public int upgradeLevel(int w, int g, int type) {
        Garden garden = worlds[w].gardens[g];
        return type == UPGRADE_SPEED ? garden.speedLevel : garden.valueLevel;
    }

    public boolean upgradeMaxed(int w, int g, int type) {
        return upgradeLevel(w, g, type) >= Worlds.UPGRADE_MAX_LEVEL;
    }

    public double upgradeCost(int w, int g, int type) {
        return upgradeCostAt(w, g, type, upgradeLevel(w, g, type));
    }

    public double upgradeCostAt(int w, int g, int type, int tier) {
        return Worlds.upgradePrice(w, tier, type == UPGRADE_SPEED, g);
    }

    public synchronized boolean buyUpgrade(int w, int g, int type, int tier) {
        sync();
        int level = upgradeLevel(w, g, type);
        if (tier < level || tier >= Worlds.UPGRADE_MAX_LEVEL) {
            return false;
        }
        double cost = upgradeCostAt(w, g, type, tier);
        if (money[w] < cost) {
            return false;
        }
        money[w] -= cost;
        Garden garden = worlds[w].gardens[g];
        if (type == UPGRADE_SPEED) {
            garden.speedLevel = tier + 1;
        } else {
            garden.valueLevel = tier + 1;
        }
        save();
        return true;
    }

    public int getCurrentWorld() {
        return currentWorld;
    }

    public synchronized void onStageEntered(int stageId) {
        int index = Worlds.indexOfStage(stageId);
        if (index >= 0 && index != currentWorld) {
            currentWorld = index;
            save();
        }
    }

    public synchronized void save() {
        StringBuilder sb = new StringBuilder();
        for (int w = 0; w < money.length; w++) {
            sb.append(w == 0 ? "money" : "money." + w).append('=')
                    .append(Double.doubleToLongBits(money[w])).append('\n');
        }
        for (int w = 0; w < angels.length; w++) {
            sb.append(w == 0 ? "angels" : "angels." + w).append('=')
                    .append(Double.doubleToLongBits(angels[w])).append('\n');
        }
        // Farm 0 keeps the original key names so older saves still load.
        for (int w = 0; w < lifetimeEarnings.length; w++) {
            sb.append(w == 0 ? "lifetime" : "lifetime." + w).append('=')
                    .append(Double.doubleToLongBits(lifetimeEarnings[w])).append('\n');
            sb.append(w == 0 ? "claimed" : "claimed." + w).append('=')
                    .append(Double.doubleToLongBits(angelsClaimed[w])).append('\n');
        }
        for (int w = 0; w < superBought.length; w++) {
            sb.append("su.").append(w).append('=').append(superBought[w]).append('\n');
        }
        for (int w = 0; w < worlds.length; w++) {
            sb.append("pu.").append(w).append('=').append(bits(profitBought[w])).append('\n');
            sb.append("eb.").append(w).append('=').append(bits(efficiencyBought[w])).append('\n');
        }
        sb.append("time=").append(lastSync).append('\n');
        sb.append("world=").append(currentWorld).append('\n');
        for (int w = 0; w < worlds.length; w++) {
            World world = worlds[w];
            sb.append(String.format(Locale.US, "w%d=%d,%d,%d,%d\n", w, world.travelManager ? 1 : 0, world.buyer ? 1 : 0, world.buyerGarden, world.quickBuy ? 1 : 0));
            for (int g = 0; g < world.gardens.length; g++) {
                Garden garden = world.gardens[g];
                sb.append(String.format(Locale.US, "g%d.%d=%d,%d,%d,%d,%d,%d,%d,%d\n", w, g,
                        garden.count, garden.manager ? 1 : 0, garden.speedLevel, garden.valueLevel,
                        garden.running ? 1 : 0, Double.doubleToLongBits(garden.elapsed),
                        garden.accountant ? 1 : 0, garden.discount ? 1 : 0));
            }
        }
        prefs.putString(PREF_KEY, sb.toString());
    }

    private static String bits(boolean[] flags) {
        StringBuilder sb = new StringBuilder();
        for (boolean flag : flags) {
            sb.append(flag ? '1' : '0');
        }
        return sb.toString();
    }

    private static void readBits(String value, boolean[] flags) {
        for (int i = 0; i < flags.length; i++) {
            flags[i] = i < value.length() && value.charAt(i) == '1';
        }
    }

    private void load() {
        String data = prefs.getString(PREF_KEY, null);
        if (data == null) {
            return;
        }
        try {
            String[] lines = data.split("\n");
            for (String line : lines) {
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String key = line.substring(0, eq);
                String value = line.substring(eq + 1).trim();
                if (key.equals("money")) {
                    money[0] = Double.longBitsToDouble(Long.parseLong(value));
                } else if (key.startsWith("money.")) {
                    int w = Integer.parseInt(key.substring(6));
                    if (w >= 0 && w < money.length) {
                        money[w] = Double.longBitsToDouble(Long.parseLong(value));
                    }
                } else if (key.equals("angels") || key.startsWith("angels.")) {
                    int w = key.equals("angels") ? 0 : Integer.parseInt(key.substring(7));
                    if (w >= 0 && w < angels.length) {
                        angels[w] = Double.longBitsToDouble(Long.parseLong(value));
                    }
                } else if (key.equals("lifetime") || key.startsWith("lifetime.")) {
                    int w = key.equals("lifetime") ? 0 : Integer.parseInt(key.substring(9));
                    if (w >= 0 && w < lifetimeEarnings.length) {
                        lifetimeEarnings[w] = Double.longBitsToDouble(Long.parseLong(value));
                    }
                } else if (key.equals("claimed") || key.startsWith("claimed.")) {
                    int w = key.equals("claimed") ? 0 : Integer.parseInt(key.substring(8));
                    if (w >= 0 && w < angelsClaimed.length) {
                        angelsClaimed[w] = Double.longBitsToDouble(Long.parseLong(value));
                    }
                } else if (key.startsWith("pu.") || key.startsWith("eb.")) {
                    int w = Integer.parseInt(key.substring(3));
                    if (w >= 0 && w < worlds.length) {
                        readBits(value, key.startsWith("pu.") ? profitBought[w] : efficiencyBought[w]);
                    }
                } else if (key.startsWith("su.")) {
                    int w = Integer.parseInt(key.substring(3));
                    if (w >= 0 && w < superBought.length) {
                        superBought[w] = Long.parseLong(value);
                    }
                } else if (key.equals("time")) {
                    lastSync = Long.parseLong(value);
                } else if (key.equals("world")) {
                    int w = Integer.parseInt(value);
                    if (w >= 0 && w < worlds.length) currentWorld = w;
                } else if (key.startsWith("w")) {
                    int w = Integer.parseInt(key.substring(1));
                    String[] p = value.split(",");
                    if (w < worlds.length) {
                        worlds[w].travelManager = p[0].equals("1");
                        worlds[w].buyer = p.length > 1 && p[1].equals("1");
                        worlds[w].buyerGarden = p.length > 2 ? Integer.parseInt(p[2]) : -1;
                        worlds[w].quickBuy = p.length > 3 && p[3].equals("1");
                        if (worlds[w].buyerGarden >= Worlds.GARDENS_PER_WORLD) worlds[w].buyerGarden = -1;
                    }
                } else if (key.startsWith("g")) {
                    String[] ids = key.substring(1).split("\\.");
                    int w = Integer.parseInt(ids[0]);
                    int g = Integer.parseInt(ids[1]);
                    String[] p = value.split(",");
                    if (w < worlds.length && g < Worlds.GARDENS_PER_WORLD) {
                        Garden garden = worlds[w].gardens[g];
                        garden.count = Integer.parseInt(p[0]);
                        garden.manager = p[1].equals("1");
                        garden.speedLevel = Integer.parseInt(p[2]);
                        garden.valueLevel = Integer.parseInt(p[3]);
                        garden.running = p[4].equals("1");
                        garden.elapsed = Double.longBitsToDouble(Long.parseLong(p[5]));
                        garden.accountant = p.length > 6 && p[6].equals("1");
                        garden.discount = p.length > 7 && p[7].equals("1");
                    }
                }
            }
        } catch (RuntimeException corrupt) {
            // A damaged save should not crash the app; fall back to what was read so far.
        }
    }
}
