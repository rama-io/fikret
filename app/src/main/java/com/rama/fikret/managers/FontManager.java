package com.rama.fikret.managers;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FontManager {
    private static final String JERSEY_PATH = "fonts/jersey25_regular.otf";
    private static final String UNIFONT_PATH = "fonts/unifont-ex-16px-square.ttf";

    private static Typeface jersey25;
    private static Typeface unifont;

    public static Typeface get(Context context) {
        if (jersey25 == null) {
            jersey25 = load(context, JERSEY_PATH);
        }
        return jersey25;
    }

    public static Typeface getUnicode(Context context) {
        if (unifont == null) {
            unifont = load(context, UNIFONT_PATH);
        }
        return unifont;
    }

    private static Typeface load(Context context, String path) {
        try {
            return Typeface.createFromAsset(context.getApplicationContext().getAssets(), path);
        } catch (Exception e) {
            return Typeface.DEFAULT;
        }
    }

    public static void apply(View view, Typeface typeface) {
        if (view instanceof TextView) {
            ((TextView) view).setTypeface(typeface);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                apply(group.getChildAt(i), typeface);
            }
        }
    }
}
