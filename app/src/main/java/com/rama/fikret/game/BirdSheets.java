package com.rama.fikret.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;

import com.rama.fikret.R;
import com.rama.fikret.helpers.FarmArt;
import com.rama.fikret.helpers.ViewUpdates;

public final class BirdSheets {
    private static final int COLUMNS = 2;
    private static final int ROWS = 4;

    private static final int[] ART_COLORS = {
            0xe3e2a8, // body
            0xd0b16c, // shade
            0xf4eece, // light
            0x27232a, // outline
            0xf87b1b, // beak
            0x652e03, // feet
    };
    private static final int[] FARM_ATTRS = {R.attr.supervisorBody, R.attr.supervisorShade, R.attr.supervisorLight, R.attr.supervisorOutline, R.attr.supervisorBeak, R.attr.supervisorFeet,};

    private static final int TOLERANCE_SQ = 30 * 30 * 3;

    private static final int ICON_FRAME = 1;

    private static final SparseArray<SpriteSheet> CACHE = new SparseArray<>();
    private static final SparseArray<Bitmap> ICONS = new SparseArray<>();
    private static Bitmap original;

    private BirdSheets() {
    }

    public static synchronized SpriteSheet forWorld(Context context, int world) {
        int style = FarmArt.styleOf(world);
        SpriteSheet cached = CACHE.get(style);
        if (cached != null) {
            return cached;
        }
        if (original == null) {
            original = new SpriteSheet(context.getResources(), R.drawable.bird, COLUMNS, ROWS).getBitmap();
        }
        Context themed = new ContextThemeWrapper(context, style);
        int[] target = new int[FARM_ATTRS.length];
        boolean same = true;
        for (int i = 0; i < target.length; i++) {
            target[i] = ViewUpdates.themeColor(themed, FARM_ATTRS[i]) & 0xFFFFFF;
            same &= target[i] == ART_COLORS[i];
        }
        Bitmap bitmap = same ? original : recolor(original, target);
        SpriteSheet sheet = new SpriteSheet(bitmap, COLUMNS, ROWS);
        CACHE.put(style, sheet);
        return sheet;
    }

    public static synchronized Drawable icon(Context context, int world) {
        int style = FarmArt.styleOf(world);
        Bitmap frame = ICONS.get(style);
        if (frame == null) {
            SpriteSheet sheet = forWorld(context, world);
            Rect r = sheet.frameRect(Direction.RIGHT.column, ICON_FRAME);
            frame = Bitmap.createBitmap(sheet.getBitmap(), r.left, r.top, r.width(), r.height());
            ICONS.put(style, frame);
        }
        BitmapDrawable drawable = new BitmapDrawable(context.getResources(), frame);
        drawable.setFilterBitmap(false);
        return drawable;
    }

    private static Bitmap recolor(Bitmap source, int[] target) {
        int w = source.getWidth();
        int h = source.getHeight();
        int[] pixels = new int[w * h];
        source.getPixels(pixels, 0, w, 0, 0, w, h);
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int alpha = p >>> 24;
            if (alpha == 0) {
                continue;
            }
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = p & 0xFF;
            int best = -1;
            int bestDistance = TOLERANCE_SQ + 1;
            for (int k = 0; k < ART_COLORS.length; k++) {
                int dr = r - ((ART_COLORS[k] >> 16) & 0xFF);
                int dg = g - ((ART_COLORS[k] >> 8) & 0xFF);
                int db = b - (ART_COLORS[k] & 0xFF);
                int distance = dr * dr + dg * dg + db * db;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = k;
                }
            }
            if (best >= 0) {
                pixels[i] = (alpha << 24) | target[best];
            }
        }
        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        result.setPixels(pixels, 0, w, 0, 0, w, h);
        return result;
    }
}
