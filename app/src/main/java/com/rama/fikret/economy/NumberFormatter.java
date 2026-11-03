package com.rama.fikret.economy;

import java.util.Locale;

public final class NumberFormatter {
    private NumberFormatter() {
    }

    private static final double SCIENTIFIC_FROM = 1e5;

    public static String money(int world, double value) {
        return Worlds.currencyOf(world) + " " + number(value);
    }

    private static String scientific(double value) {
        String[] parts = String.format(Locale.US, "%.2e", value).split("e");
        return trim(parts[0]) + "e" + Integer.parseInt(parts[1]);
    }

    public static String number(double value) {
        if (Double.isNaN(value) || value < 0) {
            value = 0;
        }
        if (Double.isInfinite(value)) {
            return "inf";
        }
        if (value < 1000) {
            if (value < 10) {
                return trim(String.format(Locale.US, "%.2f", value));
            }
            if (value < 100) {
                return trim(String.format(Locale.US, "%.1f", value));
            }
            return String.format(Locale.US, "%.0f", value);
        }
        if (Math.round(value) >= SCIENTIFIC_FROM) {
            return scientific(value);
        }
        return String.format(Locale.US, "%,d", Math.round(value));
    }

    public static String multiplier(double value) {
        if (value >= SCIENTIFIC_FROM) {
            return number(value);
        }
        return value == Math.rint(value)
                ? String.format(Locale.US, "%.0f", value)
                : trim(String.format(Locale.US, "%.2f", value));
    }

    private static String trim(String s) {
        if (s.indexOf('.') < 0) {
            return s;
        }
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 0 && s.charAt(end - 1) == '.') {
            end--;
        }
        return s.substring(0, end);
    }

    public static String duration(double seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        if (seconds < 10) {
            return String.format(Locale.US, "%.1fs", seconds);
        }
        long total = (long) Math.ceil(seconds);
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        if (h > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", h, m, s);
        }
        return String.format(Locale.US, "%d:%02d", m, s);
    }
}
