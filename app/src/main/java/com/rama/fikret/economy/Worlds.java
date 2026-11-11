package com.rama.fikret.economy;

import com.rama.fikret.game.Maps;

public final class Worlds {
    private Worlds() {
    }

    public static final int GARDENS_PER_WORLD = 10;

    public static final double ANGEL_DIVISOR = 4e11 / 9;
    public static final double ANGEL_BONUS = 0.02;

    public static final int SUPER_PROFIT = 0;
    public static final int SUPER_EFFICIENCY = 1;

    private static final double[][] BIRD_ETERNALS_VALUES = {
            {1e4, SUPER_PROFIT, 3},
            {1e5, SUPER_EFFICIENCY, 0.02},
            {1e8, SUPER_EFFICIENCY, 0.02},
            {1e9, SUPER_PROFIT, 5},
            {1e11, SUPER_PROFIT, 9},
            {1e12, SUPER_PROFIT, 11},
            {1e17, SUPER_PROFIT, 15},
            {1e19, SUPER_EFFICIENCY, 0.10},
            {1e20, SUPER_PROFIT, 15},
            {1e22, SUPER_PROFIT, 3},
            {1e24, SUPER_PROFIT, 5},
            {1e26, SUPER_PROFIT, 5},
            {1e28, SUPER_EFFICIENCY, 0.10},
            {1e30, SUPER_PROFIT, 5},
            {1e32, SUPER_PROFIT, 7},
            {1e34, SUPER_PROFIT, 8},
            {1e36, SUPER_PROFIT, 13},
            {1e38, SUPER_PROFIT, 5},
            {1e40, SUPER_PROFIT, 3},
            {1e42, SUPER_PROFIT, 4},
            {1e44, SUPER_PROFIT, 5},
            {1e46, SUPER_PROFIT, 3},
            {1e48, SUPER_PROFIT, 19},
    };

    private static final double[][] MEDALS = {
            {1e12, 3},
            {1e15, 3},
            {1e18, 3},
            {1e21, 3},
            {1e42, 7},
            {1e45, 3},
            {1e51, 7},
            {1e54, 5},
            {1e60, 7},
            {1e66, 9},
            {1e72, 11},
            {1e75, 13},
            {1e78, 15},
            {1e84, 3},
            {3e87, 3},
            {1e93, 2},
            {1e98, 3},
            {1e99, 6},
            {1e100, 2},
            {1e102, 5},
            {1e105, 3},
            {1e111, 3},
            {1e114, 3},
            {1e117, 5},
            {1e118, 3},
            {1e120, 7},
            {1e123, 3},
            {1e126, 5},
            {1e129, 2},
            {1e138, 3},
            {1e141, 3},
            {1e144, 3},
            {1e145, 3},
            {1e153, 5},
            {1e156, 3},
            {1e159, 3},
            {1e162, 2},
            {1e163, 2},
            {1e165, 2},
            {1e166, 2},
            {1e168, 2},
            {1e169, 2},
            {1e171, 2},
            {1e172, 2},
            {1e174, 2},
            {1e177, 9},
            {1e183, 10},
            {1e189, 5},
            {1e192, 3},
            {1e195, 4},
            {1e198, 5},
            {1e207, 5},
            {1e216, 5},
            {1e228, 5},
            {1e231, 2},
            {1e237, 4},
    };

    private static final double[][] BIRD_EFFICIENCY_VALUES = {
            {1e15, 0.01},
            {1e20, 0.01},
            {1e25, 0.01},
            {1e30, 0.01},
            {1e35, 0.021},
            {1e40, 0.015},
            {1e42, 0.01},
            {1e44, 0.02},
            {1e46, 0.005},
            {1e48, 0.02},
            {1e50, 0.012},
            {1e52, 0.013},
            {10e54, 0.007},
            {1e56, 0.02},
            {1e58, 0.003},
    };

    public static final int MEDAL_COUNT = MEDALS.length;
    public static final int EFFICIENCY_BIRD_COUNT = BIRD_EFFICIENCY_VALUES.length;

    public static double medalValue(int index) {
        return MEDALS[index][1];
    }

    public static double efficiencyBirdValue(int index) {
        return BIRD_EFFICIENCY_VALUES[index][1];
    }

    private static double[] column(double[][] table, double factor) {
        double[] result = new double[table.length];
        for (int i = 0; i < table.length; i++) {
            result[i] = table[i][0] * factor;
        }
        return result;
    }

    public static final int SUPER_UPGRADE_COUNT = BIRD_ETERNALS_VALUES.length;

    public static double superCost(int index) {
        return BIRD_ETERNALS_VALUES[index][0];
    }

    public static int superKind(int index) {
        return (int) BIRD_ETERNALS_VALUES[index][1];
    }

    public static double superValue(int index) {
        return BIRD_ETERNALS_VALUES[index][2];
    }

    public static final String CURRENCY_LAKE = "DOP";
    public static final String CURRENCY_FOREST = "BRL";
    public static final String CURRENCY_CAVE = "VND";
    public static final String CURRENCY_VOLCANO = "IDR";
    public static final String CURRENCY_NUCLEAR = "CNY";
    public static final String CURRENCY_ARCTIC = "RUB";
    public static final String CURRENCY_BUBBLEGUM = "BSD";
    public static final String CURRENCY_SPACE = "JPY";
    public static final String CURRENCY_NIGHTMARE = "EUR";

    public static final double START_MONEY = 4;

    private static final double[] BIRD_MANAGER_COSTS = {
            1e3,
            1e4,
            1e5,
            1e6,
            1e7,
            1e8,
            1e9,
            1e10,
            1e11,
            1e12
    };

    private static final double GUIDE_BIRD_COST = 1e33;

    private static final double BUYER_COST_LAKE = 1e21;

    private static final double QUICK_BUY_COST = 1e23;

    public static final double ACCOUNTANT_COST_FACTOR = 0.9;
    private static final double[] BIRD_ACCOUNTANT_COSTS = {
            10,
            100,
            1e3,
            1e4,
            1e5,
            1e7,
            1e8,
            1e9,
            1e10,
            1e11
    };

    public static final double DISCOUNT_COST_DIVISOR = 100000;
    private static final double[] BIRD_DISCOUNT_COST = {
            1e60,
            1e62,
            1e64,
            1e66,
            1e68,
            1e70,
            1e72,
            1e74,
            1e76,
            1e78
    };

    public static final double UPGRADE_VALUE_FACTOR = 100;
    public static final double UPGRADE_SPEED_FACTOR = 250;
    public static final double UPGRADE_GROWTH = 10;
    public static final int UPGRADE_MAX_LEVEL = 10;
    public static final double UPGRADE_MIN_STEP = 1.05;

    public static final class GardenDef {
        public final String name;
        public final double baseCost;
        public final double coefficient;
        public final double cycleSeconds;
        public final double baseRevenue;

        GardenDef(String name, double baseCost, double coefficient, double cycleSeconds, double baseRevenue) {
            this.name = name;
            this.baseCost = baseCost;
            this.coefficient = coefficient;
            this.cycleSeconds = cycleSeconds;
            this.baseRevenue = baseRevenue;
        }
    }

    public static final class WorldDef {
        public final int stageId;
        public final String name;
        public final GardenDef[] gardens;
        public final String currency;
        public final double[] managerCosts;
        public final double travelManagerCost;
        public final double buyerCost;
        public final double quickBuyCost;
        public final double[] accountantCosts;
        public final double[] discountCosts;
        public final double[] medalThresholds;
        public final double[] efficiencyBirdCosts;
        public final boolean placeholderPrices;
        public final double startMoney;

        WorldDef(int stageId, String name, String currency, GardenDef[] gardens, double[] managerCosts, double travelManagerCost, double[] accountantCosts, double[] discountCosts, double currencyScale, boolean placeholderPrices, double startMoney) {
            this.startMoney = startMoney;
            this.medalThresholds = column(MEDALS, currencyScale);
            this.efficiencyBirdCosts = column(BIRD_EFFICIENCY_VALUES, currencyScale);
            this.stageId = stageId;
            this.name = name;
            this.currency = currency;
            this.gardens = gardens;
            this.managerCosts = managerCosts;
            this.travelManagerCost = travelManagerCost;
            GardenDef top = gardens[gardens.length - 1];
            this.buyerCost = BUYER_COST_LAKE * (top.baseRevenue / top.cycleSeconds) / LAKE_TOP_INCOME;
            this.quickBuyCost = QUICK_BUY_COST * (startMoney / START_MONEY);
            this.accountantCosts = accountantCosts;
            this.discountCosts = discountCosts;
            this.placeholderPrices = placeholderPrices;
        }
    }

    private static final double[][] LAKE_NUMBERS = {
            {4e0, 1.07, 0.6, 1e0},
            {5e1, 1.15, 3, 2e1},
            {6e2, 1.14, 6, 3e2},
            {7e3, 1.13, 12, 4e3},
            {8e4, 1.12, 24, 5e4},
            {9e5, 1.11, 96, 6e5},
            {1e6, 1.1, 384, 7e6},
            {2e7, 1.09, 1536, 8e7},
            {3e8, 1.08, 6144, 9e8},
            {4e9, 1.07, 36864, 1e10},
    };

    private static final double LAKE_TOP_INCOME = LAKE_NUMBERS[GARDENS_PER_WORLD - 1][3] / LAKE_NUMBERS[GARDENS_PER_WORLD - 1][2];

    private static final String[] LAKE_NAMES = {
            "Yuca",
            "Mango",
            "Cajuil",
            "Guayaba",
            "Carambola",
            "Jobo",
            "Papaya",
            "Yautía",
            "Mamey",
            "Guanabana"
    };

    // Brazil
    private static final String[] FOREST_NAMES = {
            "Abacaxi",
            "Maracujá",
            "Pitanga",
            "Jabuticaba",
            "Açaí",
            "Cupuaçu",
            "Guaraná",
            "Acerola",
            "Bacaba",
            "Tucumã"
    };

    // Vietnam
    private static final String[] CAVE_NAMES = {
            "Nhãn",
            "Vải",
            "Bưởi",
            "Sấu",
            "Trám đen",
            "Dâu da đất",
            "Thị",
            "Táo mèo",
            "Mắc mật",
            "Bứa"
    };

    // Indonesia
    private static final String[] VOLCANO_NAMES = {
            "Salak",
            "Durian",
            "Manggis",
            "Rambutan",
            "Duku",
            "Kepel",
            "Matoa",
            "Duwet",
            "Gandaria",
            "Kemang"
    };

    // China
    private static final String[] NUCLEAR_NAMES = {
            "杨梅",
            "枣",
            "猕猴桃",
            "五味子",
            "山楂",
            "黄皮",
            "悬钩子",
            "野梨",
            "柿",
            "酸枣"
    };

    // Russia
    private static final String[] ARCTIC_NAMES = {
            "Морошка",
            "Брусника",
            "Черника",
            "Голубика",
            "Водяника",
            "Княженика",
            "Костяника",
            "Жимолость",
            "Рябина",
            "Шиповник"
    };

    // Bahamas
    private static final String[] BUBBLEGUM_NAMES = {
            "Guana berry",
            "Governor's plum",
            "Darling plum",
            "Pigeon plum",
            "Coco plum",
            "Sea grape",
            "Saffron plum",
            "Andros wild guava",
            "Sapodilla",
            "Sugar apple"
    };

    // Japan
    private static final String[] SPACE_NAMES = {
            "アケビ",
            "サルナシ",
            "ヤマブドウ",
            "シークヮーサー",
            "スダチ",
            "カボス",
            "ユズ",
            "ヤマモモ",
            "ムベ",
            "ハスカップ"
    };

    // Spain
    private static final String[] NIGHTMARE_NAMES = {
            "Granada",
            "Cereza",
            "Uva tinta",
            "Higo",
            "Madroño",
            "Tomate",
            "Pimiento choricero",
            "Remolacha",
            "Lombarda",
            "Frambuesa"
    };

    private static GardenDef[] build(String[] names, double[][] numbers) {
        GardenDef[] result = new GardenDef[GARDENS_PER_WORLD];
        for (int i = 0; i < GARDENS_PER_WORLD; i++) {
            result[i] = new GardenDef(names[i], numbers[i][0], numbers[i][1], numbers[i][2], numbers[i][3]);
        }
        return result;
    }

    private static GardenDef[] buildFromLake(String[] names, double currencyScale, double timeScale, double coefficientOffset) {
        double[][] numbers = new double[GARDENS_PER_WORLD][];
        for (int i = 0; i < GARDENS_PER_WORLD; i++) {
            double[] base = LAKE_NUMBERS[i];
            double coefficient = i == 0 ? base[1] : Math.round((base[1] + coefficientOffset) * 10000d) / 10000d;
            double time = i == 0 ? base[2] : base[2] * timeScale;
            numbers[i] = new double[]{base[0] * currencyScale, coefficient, time, base[3] * currencyScale};
        }
        return build(names, numbers);
    }

    private static double[] scaled(double[] values, double factor) {
        double[] result = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i] * factor;
        }
        return result;
    }

    private static final double SCALE_BRL = 0.28;
    private static final double SCALE_VND = 20;
    private static final double SCALE_IDR = 17;
    private static final double SCALE_CNY = 0.33;
    private static final double SCALE_RUB = 1.2;
    private static final double SCALE_BSD = 0.13;
    private static final double SCALE_JPY = 1.6;
    private static final double SCALE_EUR = 0.12;

    public static final WorldDef[] ALL = {
            new WorldDef(
                    Maps.LAKE,
                    "Lake",
                    CURRENCY_LAKE,
                    build(LAKE_NAMES, LAKE_NUMBERS),
                    BIRD_MANAGER_COSTS,
                    GUIDE_BIRD_COST,
                    BIRD_ACCOUNTANT_COSTS,
                    BIRD_DISCOUNT_COST,
                    1,
                    false,
                    START_MONEY
            ),
            farm(
                    Maps.FOREST,
                    "Forest",
                    CURRENCY_FOREST,
                    FOREST_NAMES,
                    SCALE_BRL,
                    1.1,
                    0
            ),
            farm(
                    Maps.CAVE,
                    "Cave",
                    CURRENCY_CAVE,
                    CAVE_NAMES,
                    SCALE_VND,
                    1.2,
                    0
            ),
            farm(
                    Maps.VOLCANO,
                    "Volcano",
                    CURRENCY_VOLCANO,
                    VOLCANO_NAMES,
                    SCALE_IDR,
                    1.3,
                    0.005
            ),
            farm(
                    Maps.NUCLEAR,
                    "Nuclear",
                    CURRENCY_NUCLEAR,
                    NUCLEAR_NAMES,
                    SCALE_CNY,
                    1.4,
                    0.005
            ),
            farm(
                    Maps.ARCTIC,
                    "Arctic",
                    CURRENCY_ARCTIC,
                    ARCTIC_NAMES,
                    SCALE_RUB,
                    1.5,
                    0.01
            ),
            farm(
                    Maps.BUBBLEGUM_LAND,
                    "Bubblegum land",
                    CURRENCY_BUBBLEGUM,
                    BUBBLEGUM_NAMES,
                    SCALE_BSD,
                    1.6,
                    0.01
            ),
            farm(
                    Maps.SPACE,
                    "Space",
                    CURRENCY_SPACE,
                    SPACE_NAMES,
                    SCALE_JPY,
                    1.7,
                    0.015
            ),
            farm(
                    Maps.NIGHTMARE,
                    "Nightmare",
                    CURRENCY_NIGHTMARE,
                    NIGHTMARE_NAMES,
                    SCALE_EUR,
                    1.8,
                    0.02
            ),
    };

    private static WorldDef farm(int stageId, String name, String currency, String[] names, double currencyScale, double timeScale, double coefficientOffset) {
        return new WorldDef(stageId, name, currency, buildFromLake(names, currencyScale, timeScale, coefficientOffset), scaled(BIRD_MANAGER_COSTS, currencyScale), GUIDE_BIRD_COST * currencyScale, BIRD_ACCOUNTANT_COSTS, scaled(BIRD_DISCOUNT_COST, currencyScale), currencyScale, true, START_MONEY * currencyScale);
    }

    private static final double[][][][] UPGRADE_PRICES = buildUpgradePrices();

    private static double[][][][] buildUpgradePrices() {
        double[][][][] table = new double[ALL.length][UPGRADE_MAX_LEVEL][2][GARDENS_PER_WORLD];
        for (int w = 0; w < ALL.length; w++) {
            double previous = 0;
            for (int tier = 0; tier < UPGRADE_MAX_LEVEL; tier++) {
                for (int speed = 0; speed < 2; speed++) {
                    double factor = speed == 1 ? UPGRADE_SPEED_FACTOR : UPGRADE_VALUE_FACTOR;
                    for (int g = 0; g < GARDENS_PER_WORLD; g++) {
                        double natural = ALL[w].gardens[g].baseCost * factor * Math.pow(UPGRADE_GROWTH, tier);
                        double price = Math.max(natural, previous * UPGRADE_MIN_STEP);
                        table[w][tier][speed][g] = price;
                        previous = price;
                    }
                }
            }
        }
        return table;
    }

    public static double upgradePrice(int world, int tier, boolean speed, int garden) {
        return UPGRADE_PRICES[world][tier][speed ? 1 : 0][garden];
    }

    public static String currencyOf(int world) {
        return ALL[world].currency;
    }

    public static int indexOfStage(int stageId) {
        for (int i = 0; i < ALL.length; i++) {
            if (ALL[i].stageId == stageId) {
                return i;
            }
        }
        return -1;
    }

    public static String stageName(int stageId) {
        switch (stageId) {
            case Maps.LAKE:
                return "Lake";
            case Maps.FOREST:
                return "Forest";
            case Maps.CAVE:
                return "Cave";
            case Maps.VOLCANO:
                return "Volcano";
            case Maps.NUCLEAR:
                return "Nuclear";
            case Maps.ARCTIC:
                return "Arctic";
            case Maps.BUBBLEGUM_LAND:
                return "Bubblegum land";
            case Maps.SPACE:
                return "Space";
            case Maps.NIGHTMARE:
                return "Nightmare";
            default:
                return "Stage " + stageId;
        }
    }

    public static final int[] ALL_STAGE_IDS = {
            Maps.LAKE,
            Maps.FOREST,
            Maps.CAVE,
            Maps.VOLCANO,
            Maps.NUCLEAR,
            Maps.ARCTIC,
            Maps.BUBBLEGUM_LAND,
            Maps.SPACE,
            Maps.NIGHTMARE
    };
}
