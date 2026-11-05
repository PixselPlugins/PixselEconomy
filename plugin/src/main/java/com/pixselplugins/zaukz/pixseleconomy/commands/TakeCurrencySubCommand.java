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
 * {@code /takecurrency <player> <amount> [currency]}: removes money from an account.
 */
public final class TakeCurrencySubCommand extends AbstractBalanceCommand {

    public TakeCurrencySubCommand(PixselEconomy plugin) {
        super(plugin);
    }

    @Override
    public @NotNull String name() {
        return "takecurrency";
    }

    @Override
    public @NotNull String permission() {
        return "pixsel.economy.take";
    }

    @Override
    public @NotNull String usage() {
        return "pixseleconomy takecurrency <player> <amount> [currency]";
    }

    @Override
    public @NotNull String description() {
        return "Remove money from a player.";
    }

    @Override
    public boolean standalone() {
        return false;
    }

    @Override
    protected CompletableFuture<TransactionResult> change(UUID playerId, String currencyId, double amount) {
        return plugin.economy().withdraw(playerId, currencyId, amount, BalanceChangeCause.COMMAND_TAKE);
    }

    @Override
    protected String successKey() {
        return "admin.removed";
    }
}
