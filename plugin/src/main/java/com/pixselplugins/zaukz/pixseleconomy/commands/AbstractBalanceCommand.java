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
import com.pixselplugins.zaukz.pixseleconomy.api.TransactionResult;
import com.pixselplugins.zaukz.pixseleconomy.utils.Numbers;
import com.pixselplugins.zaukz.pixseleconomy.utils.StatusMessages;
import com.pixselplugins.zaukz.pixseleconomy.utils.Targets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Shared implementation of {@code /addcurrency}, {@code /takecurrency} and {@code /setcurrency}.
 */
public abstract class AbstractBalanceCommand implements SubCommand {

    protected final PixselEconomy plugin;

    protected AbstractBalanceCommand(PixselEconomy plugin) {
        this.plugin = plugin;
    }

    /**
     * Performs the balance change.
     *
     * @param playerId   the target account
     * @param currencyId the currency identifier
     * @param amount     the requested amount
     * @return a future completing with the outcome
     */
    protected abstract CompletableFuture<TransactionResult> change(UUID playerId, String currencyId, double amount);

    /**
     * Returns the message key used when the change succeeded.
     *
     * @return the message key
     */
    protected abstract String successKey();

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

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sendUsage(plugin, sender);
            return;
        }
        OfflinePlayer target = Targets.find(args[0]);
        if (target == null) {
            plugin.messages().send(sender, "general.unknown-player", Map.of("player", args[0]));
            return;
        }
        Double parsed = Numbers.parseAmount(args[1]);
        if (parsed == null) {
            plugin.messages().send(sender, "general.invalid-amount", Map.of("input", args[1], "currency", ""));
            return;
        }
        Currency currency = args.length >= 3 ? plugin.currencies().get(args[2]) : plugin.currencies().getDefault();
        if (currency == null) {
            plugin.messages().send(sender, "general.unknown-currency", Map.of("currency", args[2]));
            return;
        }

        double amount = currency.round(parsed);
        change(target.getUniqueId(), currency.getId(), amount)
                .thenAccept(result -> plugin.getServer().getScheduler().runTask(plugin,
                        () -> report(sender, target, currency, amount, result)));
    }

    private void report(CommandSender sender, OfflinePlayer target, Currency currency, double amount,
                        TransactionResult result) {
        if (!result.isSuccessful()) {
            StatusMessages.send(plugin.messages(), plugin.currencies(), sender, result,
                    Map.of("amount", currency.format(amount)));
            return;
        }
        plugin.messages().send(sender, successKey(), Map.of(
                "player", Targets.name(target),
                "amount", currency.format(amount),
                "balance", currency.format(result.getNewBalance()),
                "currency", currency.getDisplayName()));

        Player online = target.getPlayer();
        if (online != null && !online.getName().equals(sender.getName())) {
            plugin.messages().send(online, "admin.balance-changed", Map.of(
                    "balance", currency.format(result.getNewBalance()),
                    "currency", currency.getDisplayName()));
        }
    }
}
