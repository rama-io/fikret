package com.rama.fikret.helpers;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;

public final class SystemBars {
    private SystemBars() {
    }

    @SuppressWarnings("deprecation")
    public static void applyInsets(final View root) {
        if (Build.VERSION.SDK_INT < 21) return;

        final int padLeft = root.getPaddingLeft();
        final int padTop = root.getPaddingTop();
        final int padRight = root.getPaddingRight();
        final int padBottom = root.getPaddingBottom();

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int left, top, right, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets in = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                left = in.left;
                top = in.top;
                right = in.right;
                bottom = in.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(padLeft + left, padTop + top, padRight + right, padBottom + bottom);
            return insets;
        });
        root.requestApplyInsets();
    }

    @SuppressWarnings("deprecation")
    public static void applyFullscreen(Activity activity) {
        Window window = activity.getWindow();
        int sdk = Build.VERSION.SDK_INT;

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (sdk >= 21) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
        if (sdk >= 28) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.layoutInDisplayCutoutMode = sdk >= 30
                    ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(lp);
        }
        if (sdk >= 29) {
            window.setNavigationBarContrastEnforced(false);
        }
        if (sdk >= 30) {
            window.setDecorFitsSystemWindows(false);
        } else if (sdk >= 16) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @SuppressWarnings("deprecation")
    public static void applyInsetsAsMargins(final View view) {
        if (Build.VERSION.SDK_INT < 21) return;
        final android.view.ViewGroup.MarginLayoutParams base =
                new android.view.ViewGroup.MarginLayoutParams((android.view.ViewGroup.MarginLayoutParams) view.getLayoutParams());

        view.setOnApplyWindowInsetsListener((v, insets) -> {
            int left, top, right, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets in = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                left = in.left;
                top = in.top;
                right = in.right;
                bottom = in.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
                if (Build.VERSION.SDK_INT >= 28 && insets.getDisplayCutout() != null) {
                    left = Math.max(left, insets.getDisplayCutout().getSafeInsetLeft());
                    top = Math.max(top, insets.getDisplayCutout().getSafeInsetTop());
                    right = Math.max(right, insets.getDisplayCutout().getSafeInsetRight());
                    bottom = Math.max(bottom, insets.getDisplayCutout().getSafeInsetBottom());
                }
            }
            android.view.ViewGroup.MarginLayoutParams lp = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.setMargins(base.leftMargin + left, base.topMargin + top,
                    base.rightMargin + right, base.bottomMargin + bottom);
            v.setLayoutParams(lp);
            return insets;
        });
        view.requestApplyInsets();
    }
}
