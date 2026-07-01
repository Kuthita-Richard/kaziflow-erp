package com.kaziflow.utils;

import java.text.NumberFormat;
import java.util.Locale;

public class KESFormatter {

    private static final NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);

    static {
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(2);
    }

    public static String format(double amount) {
        return "KES " + formatter.format(amount);
    }

    public static String formatShort(double amount) {
        if (amount >= 1_000_000) {
            return String.format("KES %.1fM", amount / 1_000_000);
        } else if (amount >= 1_000) {
            return String.format("KES %.1fK", amount / 1_000);
        }
        return format(amount);
    }

    public static String formatNumber(double amount) {
        return formatter.format(amount);
    }
}
