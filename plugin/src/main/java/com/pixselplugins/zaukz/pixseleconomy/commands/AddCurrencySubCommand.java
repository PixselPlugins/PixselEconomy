/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.commands;

import com.pixselplugins.zaukz.pixseleconomy.PixselEconomy;
import com.pixselplugins.zaukz.pixseleconomy.api.BalanceChangeCause;
import com.pixselplugins.zaukz.pixseleconomy.api.TransactionResult;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /addcurrency <player> <amount> [currency]}: adds money to an account.
 */
public final class AddCurrencySubCommand extends AbstractBalanceCommand {

    public AddCurrencySubCommand(PixselEconomy plugin) {
        super(plugin);
    }

    @Override
    public @NotNull String name() {
        return "addcurrency";
    }

    @Override
    public @NotNull String permission() {
        return "pixsel.economy.add";
    }

    @Override
    public @NotNull String usage() {
        return "pixseleconomy addcurrency <player> <amount> [currency]";
    }

    @Override
    public @NotNull String description() {
        return "Add money to a player.";
    }

    @Override
    public boolean standalone() {
        return false;
    }

    @Override
    protected CompletableFuture<TransactionResult> change(UUID playerId, String currencyId, double amount) {
        return plugin.economy().deposit(playerId, currencyId, amount, BalanceChangeCause.COMMAND_ADD);
    }

    @Override
    protected String successKey() {
        return "admin.added";
    }
}
