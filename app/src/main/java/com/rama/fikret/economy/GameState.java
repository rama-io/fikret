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

    private static final String PREF_KEY = "economy:state";

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
    private long lastSync;
    private int currentWorld = 0;

    private GameState(Context context) {
        prefs = PrefsManager.getInstance(context);
        for (int i = 0; i < worlds.length; i++) {
            worlds[i] = new World();
            money[i] = Worlds.ALL[i].startMoney;
        }
        load();
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
        for (int w = 0; w < worlds.length; w++) {
            for (int g = 0; g < Worlds.GARDENS_PER_WORLD; g++) {
                advance(w, g, dt);
            }
        }
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

    public synchronized double getLifetimeEarnings(int w) {
        return lifetimeEarnings[w];
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
        return Math.pow(2, worlds[w].gardens[g].valueLevel);
    }

    public double revenuePerCycle(int w, int g) {
        Garden garden = worlds[w].gardens[g];
        return Worlds.ALL[w].gardens[g].baseRevenue * garden.count * valueMultiplier(w, g)
                * angelBonus(w);
    }

    public double revenuePerSecond(int w, int g) {
        return revenuePerCycle(w, g) / cycleSeconds(w, g);
    }

    public synchronized double angelBonus(int w) {
        return 1 + Worlds.ANGEL_BONUS * angels[w];
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
        sb.append("time=").append(lastSync).append('\n');
        sb.append("world=").append(currentWorld).append('\n');
        for (int w = 0; w < worlds.length; w++) {
            World world = worlds[w];
            sb.append(String.format(Locale.US, "w%d=%d\n", w, world.travelManager ? 1 : 0));
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
