/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.api;

import java.util.UUID;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired before a balance is written, for every change made by a command, by the API or by a
 * banknote.
 *
 * <p>Cancelling the event aborts the operation and makes it complete with
 * {@link TransactionStatus#CANCELLED}.</p>
 *
 * <h2>Threading</h2>
 * <p>The event is fired on the thread that performs the change. When it comes from the API, that is
 * the plugin's database thread, so {@link #isAsynchronous()} is {@code true} and handlers must not
 * touch the Bukkit API without re-scheduling onto the server thread. Command and banknote driven
 * changes are prepared on the database thread as well, so treat every handler as asynchronous
 * unless you check {@link #isAsynchronous()}. Keep handlers short: storage writes wait on them.</p>
 *
 * @since 1.0.0
 */
public class PixselBalanceChangeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final String currencyId;
    private final double previousBalance;
    private final double newBalance;
    private final double amount;
    private final BalanceChangeCause cause;
    private boolean cancelled;

    /**
     * Creates the event. Fired by PixselEconomy only.
     *
     * @param playerId        the account owner
     * @param currencyId      the currency being changed
     * @param previousBalance the balance before the change
     * @param newBalance      the balance after the change
     * @param amount          the requested amount of the operation
     * @param cause           what triggered the change
     * @param asynchronous    whether the event is fired off the server thread
     */
    public PixselBalanceChangeEvent(@NotNull UUID playerId, @NotNull String currencyId, double previousBalance,
                                    double newBalance, double amount, @NotNull BalanceChangeCause cause,
                                    boolean asynchronous) {
        super(asynchronous);
        this.playerId = playerId;
        this.currencyId = currencyId;
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
        this.amount = amount;
        this.cause = cause;
    }

    /**
     * Returns the account owner.
     *
     * @return the player UUID
     */
    @NotNull
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Returns the identifier of the currency being changed.
     *
     * @return the currency identifier
     */
    @NotNull
    public String getCurrencyId() {
        return currencyId;
    }

    /**
     * Returns the balance before the change.
     *
     * @return the previous balance
     */
    public double getPreviousBalance() {
        return previousBalance;
    }

    /**
     * Returns the balance the account is about to hold.
     *
     * @return the prospective balance
     */
    public double getNewBalance() {
        return newBalance;
    }

    /**
     * Returns the difference between the new and the previous balance.
     *
     * @return the signed change, negative when money is removed
     */
    public double getDifference() {
        return newBalance - previousBalance;
    }

    /**
     * Returns the requested amount of the operation, always positive.
     *
     * @return the requested amount
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Returns what triggered the change.
     *
     * @return the cause
     */
    @NotNull
    public BalanceChangeCause getCause() {
        return cause;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Returns the handler list required by the Bukkit event system.
     *
     * @return the handler list
     */
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
