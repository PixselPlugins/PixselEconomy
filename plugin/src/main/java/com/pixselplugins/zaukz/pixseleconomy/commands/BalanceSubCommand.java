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
import com.pixselplugins.zaukz.pixseleconomy.utils.Targets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /balance [player] [currency]}: shows a balance.
 *
 * <p>The player argument is optional: without it the sender's own balance is shown. A single
 * argument is resolved as a player first and as a currency second, so {@code /bal gems} and
 * {@code /bal Notch} both work.</p>
 */
public final class BalanceSubCommand implements SubCommand {

    private final PixselEconomy plugin;

    public BalanceSubCommand(PixselEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String name() {
        return "balance";
    }

    @Override
    public @NotNull String permission() {
        return "pixsel.economy.balance";
    }

    @Override
    public @NotNull String usage() {
        return "balance [player] [currency]";
    }

    @Override
    public @NotNull String description() {
        return "Show a balance.";
    }

    @Override
    public boolean exposedAsSubcommand() {
        return false;
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        OfflinePlayer target = null;
        Currency currency = null;

        if (args.length >= 1) {
            // A player name wins over a currency id, so /bal <player> keeps working even when a
            // currency happens to share the name. A name that is no player is read as a currency.
            target = Targets.find(args[0]);
            if (target == null) {
                currency = plugin.currencies().get(args[0]);
            }
            if (target == null && currency == null) {
                plugin.messages().send(sender, "general.unknown-player-or-currency",
                        Map.of("input", args[0]));
                return;
            }
        }
        if (args.length >= 2) {
            Currency named = plugin.currencies().get(args[1]);
            if (named == null) {
                plugin.messages().send(sender, "general.unknown-currency", Map.of("currency", args[1]));
                return;
            }
            currency = named;
        }
        if (currency == null) {
            currency = plugin.currencies().getDefault();
        }
        if (target == null) {
            if (!(sender instanceof Player player)) {
                plugin.messages().send(sender, "general.player-required");
                return;
            }
            target = player;
        }

        UUID targetId = target.getUniqueId();
        boolean self = sender instanceof Player player && player.getUniqueId().equals(targetId);
        if (!self && !sender.hasPermission("pixsel.economy.balance.others")) {
            plugin.messages().send(sender, "general.no-permission");
            return;
        }

        Currency requested = currency;
        OfflinePlayer viewed = target;
        plugin.economy().getBalance(targetId, requested.getId()).thenAccept(balance ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Map<String, String> placeholders = Map.of(
                            "player", Targets.name(viewed),
                            "balance", requested.format(balance),
                            "currency", requested.getDisplayName(),
                            "currency_id", requested.getId(),
                            "symbol", requested.getSymbol(),
                            "amount", requested.formatNumber(balance, requested.getDecimals()));
                    plugin.messages().send(sender, self ? "balance.self" : "balance.other", placeholders);
                }));
    }

    @Override
    public @NotNull List<String> complete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            return java.util.stream.Stream.concat(
                            plugin.currencies().ids().stream(),
                            plugin.getServer().getOnlinePlayers().stream().map(Player::getName))
                    .filter(value -> value.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2) {
            return plugin.currencies().ids().stream()
                    .filter(id -> id.startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
