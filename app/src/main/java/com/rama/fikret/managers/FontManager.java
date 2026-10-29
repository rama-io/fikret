package com.rama.fikret.managers;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FontManager {
    private static Typeface jersey25;

    public static Typeface getJersey25(Context context) {
        if (jersey25 == null) {
            try {
                jersey25 = Typeface.createFromAsset(context.getApplicationContext().getAssets(), "fonts/jersey25_regular.otf");
            } catch (Exception e) {
                jersey25 = Typeface.DEFAULT;
            }
        }
        return jersey25;
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
