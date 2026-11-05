/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.utils;

import com.pixselplugins.zaukz.pixseleconomy.api.Currency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Small helpers for parsing and shortening amounts entered by players.
 */
public final class Numbers {

    private static final String[] SUFFIXES = {"K", "M", "B", "T", "Q"};

    private Numbers() {
    }

    /**
     * Parses a user supplied amount. Grouping commas are accepted, and the result must be a finite
     * number of zero or more.
     *
     * @param input the raw argument
     * @return the parsed amount, or {@code null} when the input is not a usable number
     */
    @Nullable
    public static Double parseAmount(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
            double value = Double.parseDouble(input.replace(",", "").trim());
            if (!Double.isFinite(value) || value < 0) {
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * Shortens a value for compact display, for example {@code 12500} becomes {@code 12.5K}.
     *
     * @param value    the value to shorten
     * @param decimals the number of decimals when the value is below one thousand
     * @return the shortened text
     */
    @NotNull
    public static String shortFormat(double value, int decimals) {
        double absolute = Math.abs(value);
        if (absolute < 1000.0D) {
            return Currency.formatNumber(value, decimals);
        }
        double scaled = value;
        int index = -1;
        while (Math.abs(scaled) >= 1000.0D && index < SUFFIXES.length - 1) {
            scaled /= 1000.0D;
            index++;
        }
        return Currency.formatNumber(scaled, Math.min(decimals, 2)) + SUFFIXES[index];
    }
}
