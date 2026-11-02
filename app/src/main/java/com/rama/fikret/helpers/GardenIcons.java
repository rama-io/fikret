package com.rama.fikret.helpers;

import com.rama.fikret.R;
import com.rama.fikret.economy.Worlds;
import com.rama.fikret.game.Maps;

public final class GardenIcons {
    private GardenIcons() {
    }

    private static final int[] LAKE = {
            R.drawable.lake_yuca, R.drawable.lake_mango, R.drawable.lake_cajuil,
            R.drawable.lake_guayaba, R.drawable.lake_carambola, R.drawable.lake_jobo,
            R.drawable.lake_papaya, R.drawable.lake_yautia, R.drawable.lake_mamey,
            R.drawable.lake_guanabana
    };

    private static final int[] FOREST = {
            R.drawable.forest_abacaxi, R.drawable.forest_maracuya, R.drawable.forest_pitanga,
            R.drawable.forest_jabuticaba, R.drawable.forest_acai, R.drawable.forest_cupuacu,
            R.drawable.forest_guarana, R.drawable.forest_acerola, R.drawable.forest_bacaba,
            R.drawable.forest_tucuma
    };

    public static int forGarden(int world, int g) {
        int[] icons = world == Worlds.indexOfStage(Maps.FOREST) ? FOREST : LAKE;
        return icons[Math.max(0, Math.min(g, icons.length - 1))];
    }
}
