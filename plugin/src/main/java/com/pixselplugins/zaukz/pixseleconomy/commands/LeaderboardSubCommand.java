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
import com.pixselplugins.zaukz.pixseleconomy.api.LeaderboardEntry;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@code /leaderboard [currency] [page]}: shows the richest accounts, with clickable arrows that
 * print the neighbouring page.
 *
 * <p>One extra row is requested so the command knows whether a next page exists without running a
 * second counting query.</p>
 */
public final class LeaderboardSubCommand implements SubCommand {

    private final PixselEconomy plugin;

    public LeaderboardSubCommand(PixselEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String name() {
        return "leaderboard";
    }

    @Override
    public @NotNull String permission() {
        return "pixsel.economy.leaderboard";
    }

    @Override
    public @NotNull String usage() {
        return "leaderboard [currency] [page]";
    }

    @Override
    public @NotNull String description() {
        return "Show the richest accounts for a currency.";
    }

    @Override
    public boolean exposedAsSubcommand() {
        return false;
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!plugin.settings().leaderboardEnabled()) {
            plugin.messages().send(sender, "general.feature-disabled");
            return;
        }
        Currency currency = plugin.currencies().getDefault();
        int page = 1;

        if (args.length >= 1) {
            Integer numeric = parsePage(args[0]);
            if (numeric != null) {
                page = numeric;
            } else {
                Currency named = plugin.currencies().get(args[0]);
                if (named == null) {
                    plugin.messages().send(sender, "general.unknown-currency", Map.of("currency", args[0]));
                    return;
                }
                currency = named;
            }
        }
        if (args.length >= 2) {
            Integer numeric = parsePage(args[1]);
            if (numeric == null) {
                plugin.messages().send(sender, "general.invalid-page", Map.of("input", args[1]));
                return;
            }
            page = numeric;
        }

        Currency requested = currency;
        int requestedPage = page;
        int pageSize = plugin.settings().leaderboardPageSize();

        plugin.economy().getLeaderboard(requested.getId(), requestedPage, pageSize + 1)
                .thenAccept(entries -> plugin.getServer().getScheduler().runTask(plugin,
                        () -> render(sender, requested, requestedPage, pageSize, entries)));
    }

    private void render(CommandSender sender, Currency currency, int page, int pageSize,
                        List<LeaderboardEntry> entries) {
        boolean hasNext = entries.size() > pageSize;
        List<LeaderboardEntry> visible = hasNext ? entries.subList(0, pageSize) : entries;

        plugin.messages().send(sender, "leaderboard.header", Map.of(
                "currency", currency.getDisplayName(),
                "currency_id", currency.getId(),
                "page", Integer.toString(page)));

        if (visible.isEmpty()) {
            plugin.messages().send(sender, "leaderboard.empty", Map.of("currency", currency.getDisplayName()));
            return;
        }

        for (LeaderboardEntry entry : visible) {
            plugin.messages().send(sender, "leaderboard.entry", Map.of(
                    "position", Integer.toString(entry.position()),
                    "player", entry.playerName(),
                    "balance", currency.format(entry.balance())));
        }

        String previous = page > 1
                ? plugin.messages().rawFormat("leaderboard.previous",
                        Map.of("currency", currency.getId(), "page", Integer.toString(page - 1)))
                : "";
        String next = hasNext
                ? plugin.messages().rawFormat("leaderboard.next",
                        Map.of("currency", currency.getId(), "page", Integer.toString(page + 1)))
                : "";
        if (!previous.isEmpty() || !next.isEmpty()) {
            plugin.messages().send(sender, "leaderboard.footer", Map.of(
                    "previous", previous,
                    "next", next,
                    "page", Integer.toString(page)));
        }
    }

    @Nullable
    private static Integer parsePage(String input) {
        try {
            int value = Integer.parseInt(input);
            return value < 1 ? null : value;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @Override
    public @NotNull List<String> complete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            return plugin.currencies().ids().stream()
                    .filter(id -> id.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
