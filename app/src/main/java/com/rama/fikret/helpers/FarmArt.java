package com.rama.fikret.helpers;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;

import com.rama.fikret.R;
import com.rama.fikret.economy.Worlds;
import com.rama.fikret.game.Maps;

public final class FarmArt {
    private FarmArt() {
    }

    public static int styleOf(int world) {
        switch (Worlds.ALL[world].stageId) {
            case Maps.FOREST:
                return R.style.Farm_Forest;
            case Maps.CAVE:
                return R.style.Farm_Cave;
            case Maps.VOLCANO:
                return R.style.Farm_Volcano;
            case Maps.NUCLEAR:
                return R.style.Farm_Nuclear;
            case Maps.ARCTIC:
                return R.style.Farm_Arctic;
            case Maps.BUBBLEGUM_LAND:
                return R.style.Farm_Bubblegum;
            case Maps.SPACE:
                return R.style.Farm_Space;
            case Maps.NIGHTMARE:
                return R.style.Farm_Nightmare;
            default:
                return R.style.Farm_Lake;
        }
    }

    public static int applyTheme(android.app.Activity activity, int world) {
        int style = styleOf(world);
        activity.getTheme().applyStyle(style, true);
        return style;
    }

    private static Drawable load(Context context, int world, int drawableRes) {
        return new ContextThemeWrapper(context, styleOf(world)).getDrawable(drawableRes);
    }

    public static Drawable supervisor(Context context, int world) {
        return load(context, world, R.drawable.supervisor);
    }
}
