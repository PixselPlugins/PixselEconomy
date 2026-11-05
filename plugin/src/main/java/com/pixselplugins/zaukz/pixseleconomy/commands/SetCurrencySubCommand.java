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
 * {@code /setcurrency <player> <amount> [currency]}: sets an exact balance.
 */
public final class SetCurrencySubCommand extends AbstractBalanceCommand {

    public SetCurrencySubCommand(PixselEconomy plugin) {
        super(plugin);
    }

    @Override
    public @NotNull String name() {
        return "setcurrency";
    }

    @Override
    public @NotNull String permission() {
        return "pixsel.economy.set";
    }

    @Override
    public @NotNull String usage() {
        return "pixseleconomy setcurrency <player> <amount> [currency]";
    }

    @Override
    public @NotNull String description() {
        return "Set a player's balance.";
    }

    @Override
    public boolean standalone() {
        return false;
    }

    @Override
    protected CompletableFuture<TransactionResult> change(UUID playerId, String currencyId, double amount) {
        return plugin.economy().setBalance(playerId, currencyId, amount, BalanceChangeCause.COMMAND_SET);
    }

    @Override
    protected String successKey() {
        return "admin.set";
    }
}
