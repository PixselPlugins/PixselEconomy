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
 * What caused a balance change. Useful when filtering
 * {@link PixselBalanceChangeEvent} notifications, for example to ignore internal command output or
 * to log withdrawals separately from API deposits.
 *
 * @since 1.0.0
 */
public enum BalanceChangeCause {

    /** A balance change requested through the public API. */
    API,

    /** Another plugin reported the change through the API. */
    PLUGIN,

    /** {@code /pay}. */
    COMMAND_PAY,

    /** {@code /addcurrency}. */
    COMMAND_ADD,

    /** {@code /takecurrency}. */
    COMMAND_TAKE,

    /** {@code /setcurrency}. */
    COMMAND_SET,

    /** {@code /resetall}. */
    COMMAND_RESET,

    /** A player turned a balance into a banknote. */
    WITHDRAW_ISSUE,

    /** A player redeemed a banknote. */
    WITHDRAW_REDEEM,

    /** A new account received its starting balance. */
    STARTING_BALANCE,

    /** Administrative or internal maintenance. */
    OTHER
}
