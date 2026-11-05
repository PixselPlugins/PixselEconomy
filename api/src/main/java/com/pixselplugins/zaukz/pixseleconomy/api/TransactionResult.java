/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.api;

import org.jetbrains.annotations.NotNull;

/**
 * Result of a balance operation.
 *
 * <p>Always inspect {@link #getStatus()} or {@link #isSuccessful()} before showing a message to a
 * player: a failed result carries the balances as they were before the attempt.</p>
 *
 * @since 1.0.0
 */
public final class TransactionResult {

    private final TransactionStatus status;
    private final String currencyId;
    private final double amount;
    private final double previousBalance;
    private final double newBalance;

    private TransactionResult(TransactionStatus status, String currencyId, double amount,
                              double previousBalance, double newBalance) {
        this.status = status;
        this.currencyId = currencyId;
        this.amount = amount;
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
    }

    /**
     * Creates a successful result.
     *
     * @param currencyId      the currency that changed
     * @param amount          the requested amount
     * @param previousBalance the balance before the change
     * @param newBalance      the balance after the change
     * @return the result
     */
    @NotNull
    public static TransactionResult success(@NotNull String currencyId, double amount,
                                            double previousBalance, double newBalance) {
        return new TransactionResult(TransactionStatus.SUCCESS, currencyId, amount, previousBalance, newBalance);
    }

    /**
     * Creates a failed result with unchanged balances.
     *
     * @param status     the failure reason
     * @param currencyId the currency involved, or an empty string when unknown
     * @param amount     the requested amount
     * @return the result
     */
    @NotNull
    public static TransactionResult failure(@NotNull TransactionStatus status, @NotNull String currencyId,
                                            double amount) {
        return new TransactionResult(status, currencyId, amount, 0.0D, 0.0D);
    }

    /**
     * Returns whether the operation changed the balance.
     *
     * @return {@code true} for {@link TransactionStatus#SUCCESS}
     */
    public boolean isSuccessful() {
        return status == TransactionStatus.SUCCESS;
    }

    /**
     * Returns the outcome of the operation.
     *
     * @return the status, never {@code null}
     */
    @NotNull
    public TransactionStatus getStatus() {
        return status;
    }

    /**
     * Returns the identifier of the currency involved.
     *
     * @return the currency identifier, possibly empty for an unknown currency
     */
    @NotNull
    public String getCurrencyId() {
        return currencyId;
    }

    /**
     * Returns the amount that was requested.
     *
     * @return the requested amount
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Returns the balance before the operation.
     *
     * @return the previous balance, zero when the operation failed early
     */
    public double getPreviousBalance() {
        return previousBalance;
    }

    /**
     * Returns the balance after the operation.
     *
     * @return the new balance, zero when the operation failed early
     */
    public double getNewBalance() {
        return newBalance;
    }

    @Override
    public String toString() {
        return "TransactionResult{status=" + status + ", currency='" + currencyId + "', amount=" + amount
                + ", previousBalance=" + previousBalance + ", newBalance=" + newBalance + '}';
    }
}
