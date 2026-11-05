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
import com.pixselplugins.zaukz.pixseleconomy.api.Currency;
import com.pixselplugins.zaukz.pixseleconomy.api.TransactionResult;
import com.pixselplugins.zaukz.pixseleconomy.utils.Numbers;
import com.pixselplugins.zaukz.pixseleconomy.utils.StatusMessages;
import com.pixselplugins.zaukz.pixseleconomy.utils.Targets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /pay <player> <amount> [currency]}: moves money between two accounts and applies the
 * configured tax.
 */
public final class PaySubCommand implements SubCommand {

    private final PixselEconomy plugin;

    public PaySubCommand(PixselEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String name() {
        return "pay";
    }

    @Override
    public @NotNull String permission() {
        return "pixsel.economy.pay";
    }

    @Override
    public @NotNull String usage() {
        return "pay <player> <amount> [currency]";
    }

    @Override
    public @NotNull String description() {
        return "Send money to another player.";
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
        Player player = (Player) sender;
        if (!plugin.settings().payEnabled()) {
            plugin.messages().send(sender, "general.feature-disabled");
            return;
        }
        if (args.length < 2) {
            sendUsage(plugin, sender);
            return;
        }
        Currency currency = args.length >= 3 ? plugin.currencies().get(args[2]) : plugin.currencies().getDefault();
        if (currency == null) {
            plugin.messages().send(sender, "general.unknown-currency", Map.of("currency", args[2]));
            return;
        }
        if (!currency.isPayable()) {
            plugin.messages().send(sender, "general.currency-not-payable",
                    Map.of("currency", currency.getDisplayName()));
            return;
        }
        OfflinePlayer target = Targets.find(args[0]);
        if (target == null) {
            plugin.messages().send(sender, "general.unknown-player", Map.of("player", args[0]));
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId()) && !plugin.settings().payAllowSelf()) {
            plugin.messages().send(sender, "pay.self-disabled");
            return;
        }
        Double parsed = Numbers.parseAmount(args[1]);
        if (parsed == null || parsed <= 0) {
            plugin.messages().send(sender, "general.invalid-amount",
                    Map.of("input", args[1], "currency", currency.getDisplayName()));
            return;
        }
        double amount = currency.round(parsed);
        if (amount < plugin.settings().payMinimum()) {
            plugin.messages().send(sender, "pay.below-minimum",
                    Map.of("minimum", currency.format(plugin.settings().payMinimum()),
                            "currency", currency.getDisplayName()));
            return;
        }

        double tax = currency.round(amount * plugin.settings().payTaxPercentage() / 100.0D);
        CompletableFuture<TransactionResult> transfer = plugin.economy()
                .transfer(player.getUniqueId(), target.getUniqueId(), currency.getId(), amount,
                        BalanceChangeCause.COMMAND_PAY);

        // The tax is charged to the receiver after the transfer succeeds. The transfer result is
        // kept so the sender is always reported with their own balance, never the receiver's.
        transfer.thenCompose(outcome -> {
            if (!outcome.isSuccessful() || tax <= 0) {
                return CompletableFuture.completedFuture(outcome);
            }
            return plugin.economy().withdraw(target.getUniqueId(), currency.getId(), tax,
                            BalanceChangeCause.COMMAND_PAY)
                    .thenApply(taxResult -> {
                        if (!taxResult.isSuccessful()) {
                            plugin.debug("Could not collect the payment tax: " + taxResult.getStatus());
                        }
                        return outcome;
                    });
        }).thenAccept(outcome -> plugin.getServer().getScheduler().runTask(plugin,
                () -> report(player, target, currency, amount, tax, outcome)));
    }

    private void report(Player sender, OfflinePlayer target, Currency currency, double amount, double tax,
                        TransactionResult result) {
        if (!result.isSuccessful()) {
            StatusMessages.send(plugin.messages(), plugin.currencies(), sender, result,
                    Map.of("amount", currency.format(amount)));
            return;
        }
        plugin.messages().send(sender, "pay.success-sender", Map.of(
                "player", Targets.name(target),
                "amount", currency.format(amount),
                "tax", currency.format(tax),
                "balance", currency.format(result.getNewBalance()),
                "currency", currency.getDisplayName()));

        Player online = target.getPlayer();
        if (online != null && !online.getUniqueId().equals(sender.getUniqueId())) {
            plugin.messages().send(online, "pay.success-receiver", Map.of(
                    "player", sender.getName(),
                    "amount", currency.format(currency.round(amount - tax)),
                    "currency", currency.getDisplayName()));
        }
    }

    @Override
    public @NotNull List<String> complete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 3) {
            return plugin.currencies().ids().stream()
                    .filter(id -> id.startsWith(args[2].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
