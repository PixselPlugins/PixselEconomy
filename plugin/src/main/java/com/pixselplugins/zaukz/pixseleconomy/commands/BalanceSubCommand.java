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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /balance [player]}: shows a balance in the default currency.
 *
 * <p>The player argument is optional: without it the sender's own balance is shown. There is no
 * currency argument, so the first argument is always read as a player name and can never be
 * mistaken for a currency. Looking up somebody else needs {@code pixsel.economy.balance.others},
 * which is granted to everyone by default and can be denied to hide other players' balances.</p>
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
        return "balance [player]";
    }

    @Override
    public @NotNull String description() {
        return "Show your balance, or another player's.";
    }

    @Override
    public boolean exposedAsSubcommand() {
        return false;
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length > 1) {
            plugin.messages().send(sender, "general.invalid-usage", Map.of("usage", "/balance [player]"));
            return;
        }

        OfflinePlayer target;
        if (args.length == 1) {
            OfflinePlayer found = Targets.find(args[0]);
            if (found == null) {
                plugin.messages().send(sender, "general.unknown-player", Map.of("player", args[0]));
                return;
            }
            target = found;
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            plugin.messages().send(sender, "general.player-required");
            return;
        }

        UUID targetId = target.getUniqueId();
        boolean self = sender instanceof Player player && player.getUniqueId().equals(targetId);
        if (!self && !sender.hasPermission("pixsel.economy.balance.others")) {
            plugin.messages().send(sender, "general.no-permission");
            return;
        }

        Currency currency = plugin.currencies().getDefault();
        OfflinePlayer viewed = target;
        plugin.economy().getBalance(targetId, currency.getId()).thenAccept(balance ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Map<String, String> placeholders = Map.of(
                            "player", Targets.name(viewed),
                            "balance", currency.format(balance),
                            "currency", currency.getDisplayName(),
                            "currency_id", currency.getId(),
                            "symbol", currency.getSymbol(),
                            "amount", currency.formatNumber(balance, currency.getDecimals()));
                    plugin.messages().send(sender, self ? "balance.self" : "balance.other", placeholders);
                }));
    }

    @Override
    public @NotNull List<String> complete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            String typed = args[0].toLowerCase(Locale.ROOT);
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(typed))
                    .toList();
        }
        return List.of();
    }
}
