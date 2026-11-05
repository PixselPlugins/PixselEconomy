/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.api;

/**
 * Outcome of a balance operation.
 *
 * @since 1.0.0
 */
public enum TransactionStatus {

    /** The balance was changed and persisted. */
    SUCCESS,

    /** The requested amount was zero, negative or not a finite number. */
    INVALID_AMOUNT,

    /** No currency with the requested identifier exists. */
    UNKNOWN_CURRENCY,

    /** The account does not hold enough money for the operation. */
    INSUFFICIENT_FUNDS,

    /** The account is not cached and could not be loaded from storage. */
    ACCOUNT_UNAVAILABLE,

    /** The operation would push the balance above the currency's maximum. */
    LIMIT_EXCEEDED,

    /** The currency is configured as not payable to other players. */
    NOT_PAYABLE,

    /** A {@link PixselBalanceChangeEvent} listener cancelled the change. */
    CANCELLED,

    /** The storage backend reported an error. The balance was not changed. */
    STORAGE_ERROR
}
