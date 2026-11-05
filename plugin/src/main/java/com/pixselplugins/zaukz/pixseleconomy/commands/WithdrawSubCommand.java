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
import com.pixselplugins.zaukz.pixseleconomy.api.Currency;
import com.pixselplugins.zaukz.pixseleconomy.utils.Numbers;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /withdraw <amount> [currency]}: turns part of a balance into a redeemable banknote.
 */
public final class WithdrawSubCommand implements SubCommand {

    private final PixselEconomy plugin;

    public WithdrawSubCommand(PixselEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String name() {
        return "withdraw";
    }

    @Override
    public @NotNull String permission() {
        return "pixsel.economy.withdraw";
    }

    @Override
    public @NotNull String usage() {
        return "withdraw <amount> [currency]";
    }

    @Override
    public @NotNull String description() {
        return "Turn a balance into a redeemable banknote.";
    }

    @Override
    public boolean exposedAsSubcommand() {
        return false;
    }

    @Override
    public boolean playerOnly() {
        return true;
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!plugin.settings().withdrawEnabled()) {
            plugin.messages().send(sender, "general.feature-disabled");
            return;
        }
        if (args.length < 1) {
            sendUsage(plugin, sender);
            return;
        }
        Currency currency = args.length >= 2 ? plugin.currencies().get(args[1]) : plugin.currencies().getDefault();
        if (currency == null) {
            plugin.messages().send(sender, "general.unknown-currency", Map.of("currency", args[1]));
            return;
        }
        Double amount = Numbers.parseAmount(args[0]);
        if (amount == null || amount <= 0) {
            plugin.messages().send(sender, "general.invalid-amount",
                    Map.of("input", args[0], "currency", currency.getDisplayName()));
            return;
        }
        plugin.banknotes().withdraw((Player) sender, currency, amount);
    }

    @Override
    public @NotNull List<String> complete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2) {
            return plugin.currencies().ids().stream()
                    .filter(id -> id.startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
