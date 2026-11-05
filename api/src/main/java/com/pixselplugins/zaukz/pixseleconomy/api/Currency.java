/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.api;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * An immutable description of one currency, including how amounts of it are displayed.
 *
 * <p>Instances are created by the plugin and handed out by
 * {@link EconomyService#getCurrency(String)}. Third-party code only reads them.</p>
 *
 * @since 1.0.0
 */
public final class Currency {

    private final String id;
    private final String displayName;
    private final String symbol;
    private final String format;
    private final int decimals;
    private final double defaultBalance;
    private final double maxBalance;
    private final boolean payable;
    private final boolean withdrawable;
    private final double withdrawMinimum;
    private final double withdrawMaximum;
    private final double withdrawFee;
    private final double withdrawFeePercentage;

    private Currency(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.symbol = builder.symbol;
        this.format = builder.format;
        this.decimals = builder.decimals;
        this.defaultBalance = builder.defaultBalance;
        this.maxBalance = builder.maxBalance;
        this.payable = builder.payable;
        this.withdrawable = builder.withdrawable;
        this.withdrawMinimum = builder.withdrawMinimum;
        this.withdrawMaximum = builder.withdrawMaximum;
        this.withdrawFee = builder.withdrawFee;
        this.withdrawFeePercentage = builder.withdrawFeePercentage;
    }

    /**
     * Returns the internal identifier of this currency, for example {@code "coins"}.
     *
     * @return the lowercase identifier
     */
    @NotNull
    public String getId() {
        return id;
    }

    /**
     * Returns the human-readable name of this currency, for example {@code "Coins"}.
     *
     * @return the display name
     */
    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the symbol placed in front of amounts.
     *
     * @return the symbol, possibly empty
     */
    @NotNull
    public String getSymbol() {
        return symbol;
    }

    /**
     * Returns the format template used by {@link #format(double)}.
     *
     * <p>Supported tokens are {@code {symbol}}, {@code {amount}}, {@code {currency}} and
     * {@code {display_name}}.</p>
     *
     * @return the format template
     */
    @NotNull
    public String getFormat() {
        return format;
    }

    /**
     * Returns how many fraction digits amounts of this currency are rounded and displayed with.
     *
     * @return the number of decimals, between 0 and 8
     */
    public int getDecimals() {
        return decimals;
    }

    /**
     * Returns the balance new accounts start with.
     *
     * @return the starting balance
     */
    public double getDefaultBalance() {
        return defaultBalance;
    }

    /**
     * Returns the highest balance an account may hold.
     *
     * @return the maximum balance, or {@link Double#MAX_VALUE} when unlimited
     */
    public double getMaxBalance() {
        return maxBalance;
    }

    /**
     * Returns whether players may send this currency to each other with {@code /pay}.
     *
     * @return {@code true} when player-to-player payments are allowed
     */
    public boolean isPayable() {
        return payable;
    }

    /**
     * Returns whether this currency is enabled for the {@code /withdraw} feature.
     *
     * @return {@code true} when banknotes may be created for this currency
     */
    public boolean isWithdrawable() {
        return withdrawable;
    }

    /**
     * Returns the smallest amount that may be withdrawn as a banknote.
     *
     * @return the minimum withdrawal amount
     */
    public double getWithdrawMinimum() {
        return withdrawMinimum;
    }

    /**
     * Returns the largest amount that may be withdrawn as a single banknote.
     *
     * @return the maximum withdrawal amount
     */
    public double getWithdrawMaximum() {
        return withdrawMaximum;
    }

    /**
     * Returns the flat fee charged for each withdrawal.
     *
     * @return the flat withdrawal fee
     */
    public double getWithdrawFee() {
        return withdrawFee;
    }

    /**
     * Returns the percentage fee charged for each withdrawal.
     *
     * @return the withdrawal fee in percent, for example {@code 2.5} for 2.5%
     */
    public double getWithdrawFeePercentage() {
        return withdrawFeePercentage;
    }

    /**
     * Rounds an amount to the precision used by this currency.
     *
     * @param amount the amount to round
     * @return the rounded amount
     */
    public double round(double amount) {
        return BigDecimal.valueOf(amount).setScale(decimals, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Formats an amount with grouping separators, the configured decimals and this currency's
     * format template.
     *
     * @param amount the amount to format
     * @return the formatted text, ready to be shown as plain text
     */
    @NotNull
    public String format(double amount) {
        return format.replace("{symbol}", symbol)
                .replace("{amount}", formatNumber(round(amount), decimals))
                .replace("{currency}", id)
                .replace("{display_name}", displayName);
    }

    /**
     * Formats a number with grouping separators and a fixed number of decimals.
     *
     * @param value    the value to format
     * @param decimals the number of fraction digits
     * @return the formatted number using {@code ,} as grouping separator and {@code .} as decimal
     *         separator
     */
    @NotNull
    public static String formatNumber(double value, int decimals) {
        String plain = BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).toPlainString();
        int separator = plain.indexOf('.');
        String whole = separator < 0 ? plain : plain.substring(0, separator);
        String fraction = separator < 0 ? "" : plain.substring(separator);

        StringBuilder grouped = new StringBuilder(whole.length() + 4);
        for (int index = whole.length() - 1; index >= 0; index--) {
            grouped.append(whole.charAt(index));
            int digitsLeft = whole.length() - index;
            boolean digit = Character.isDigit(whole.charAt(index));
            if (digit && digitsLeft % 3 == 0 && index > 0) {
                grouped.append(',');
            }
        }
        return grouped.reverse().append(fraction).toString();
    }

    /**
     * Starts building a currency. Intended for the plugin and for advanced integrations that
     * construct currency definitions themselves.
     *
     * @param id the lowercase currency identifier
     * @return a new builder
     */
    @NotNull
    public static Builder builder(@NotNull String id) {
        return new Builder(Objects.requireNonNull(id, "id"));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Currency currency)) {
            return false;
        }
        return id.equals(currency.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Currency{id='" + id + "', decimals=" + decimals + "}";
    }

    /**
     * Builder for {@link Currency}. Every field has a sensible default so only the interesting
     * values have to be set.
     */
    public static final class Builder {

        private final String id;
        private String displayName;
        private String symbol = "";
        private String format = "{symbol}{amount}";
        private int decimals = 2;
        private double defaultBalance;
        private double maxBalance = Double.MAX_VALUE;
        private boolean payable = true;
        private boolean withdrawable = true;
        private double withdrawMinimum = 1.0D;
        private double withdrawMaximum = 100_000.0D;
        private double withdrawFee;
        private double withdrawFeePercentage;

        private Builder(String id) {
            this.id = id.toLowerCase();
            this.displayName = id;
        }

        public Builder displayName(@NotNull String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder symbol(@NotNull String symbol) {
            this.symbol = symbol;
            return this;
        }

        public Builder format(@NotNull String format) {
            this.format = format;
            return this;
        }

        public Builder decimals(int decimals) {
            this.decimals = Math.clamp(decimals, 0, 8);
            return this;
        }

        public Builder defaultBalance(double defaultBalance) {
            this.defaultBalance = defaultBalance;
            return this;
        }

        public Builder maxBalance(double maxBalance) {
            this.maxBalance = maxBalance <= 0 ? Double.MAX_VALUE : maxBalance;
            return this;
        }

        public Builder payable(boolean payable) {
            this.payable = payable;
            return this;
        }

        public Builder withdrawable(boolean withdrawable) {
            this.withdrawable = withdrawable;
            return this;
        }

        public Builder withdrawMinimum(double withdrawMinimum) {
            this.withdrawMinimum = withdrawMinimum;
            return this;
        }

        public Builder withdrawMaximum(double withdrawMaximum) {
            this.withdrawMaximum = withdrawMaximum;
            return this;
        }

        public Builder withdrawFee(double withdrawFee) {
            this.withdrawFee = withdrawFee;
            return this;
        }

        public Builder withdrawFeePercentage(double withdrawFeePercentage) {
            this.withdrawFeePercentage = withdrawFeePercentage;
            return this;
        }

        @NotNull
        public Currency build() {
            return new Currency(this);
        }
    }
}
