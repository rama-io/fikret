package com.rama.fikret.helpers;

import com.rama.fikret.economy.Worlds;

public final class DeveloperMode {
    private DeveloperMode() {
    }

    public static final boolean ENABLED = false;

    public static final int AVAILABLE_FARMS = 1;

    public static int availableFarms() {
        if (ENABLED) {
            return Worlds.ALL.length;
        }
        return Math.max(1, Math.min(AVAILABLE_FARMS, Worlds.ALL.length));
    }

    public static boolean isFarmAvailable(int world) {
        return world >= 0 && world < availableFarms();
    }

    public static boolean isStageAvailable(int stageId) {
        return isFarmAvailable(Worlds.indexOfStage(stageId));
    }
}
