/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a currency is created or deleted, either from the config file or with
 * {@code /createcurrency} and {@code /deletecurrency}.
 *
 * <p>The event is not cancellable and is always fired on the server thread.</p>
 *
 * @since 1.0.0
 */
public class PixselCurrencyChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Type type;
    private final Currency currency;

    /**
     * Creates the event. Fired by PixselEconomy only.
     *
     * @param type     whether the currency was created or deleted
     * @param currency the currency that changed
     */
    public PixselCurrencyChangeEvent(@NotNull Type type, @NotNull Currency currency) {
        this.type = type;
        this.currency = currency;
    }

    /**
     * Returns what happened to the currency.
     *
     * @return the change type
     */
    @NotNull
    public Type getType() {
        return type;
    }

    /**
     * Returns the currency that changed. When it was deleted, it is still readable here.
     *
     * @return the currency
     */
    @NotNull
    public Currency getCurrency() {
        return currency;
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

    /**
     * The kind of change a currency went through.
     */
    public enum Type {
        /** A currency became available. */
        CREATE,
        /** A currency and all balances in it were removed. */
        DELETE
    }
}
