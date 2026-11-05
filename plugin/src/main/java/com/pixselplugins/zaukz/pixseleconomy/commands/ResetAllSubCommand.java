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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@code /resetall [currency]}: wipes every balance of one currency, or of every currency.
 *
 * <p>The command is intentionally hard to trigger. It must be run several times in a row, within a
 * short window, before anything happens; a different scope or a pause restarts the count. Both
 * values are configurable under {@code safety.reset-all}.</p>
 */
public final class ResetAllSubCommand implements SubCommand {

    private record Confirmation(@Nullable String currencyId, int count, long timestamp) {
    }

    private final PixselEconomy plugin;
    private final Map<String, Confirmation> pending = new HashMap<>();

    public ResetAllSubCommand(PixselEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String name() {
        return "resetall";
    }

    @Override
    public @NotNull String permission() {
        return "pixsel.economy.resetall";
    }

    @Override
    public @NotNull String usage() {
        return "pixseleconomy resetall [currency|cancel]";
    }

    @Override
    public @NotNull String description() {
        return "Reset every balance (requires repeated confirmation).";
    }

    @Override
    public boolean standalone() {
        return false;
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        String senderKey = sender.getName();

        if (args.length >= 1 && args[0].equalsIgnoreCase("cancel")) {
            if (pending.remove(senderKey) != null) {
                plugin.messages().send(sender, "reset.cancelled");
            }
            return;
        }

        Currency currency = null;
        if (args.length >= 1) {
            currency = plugin.currencies().get(args[0]);
            if (currency == null) {
                plugin.messages().send(sender, "currency.not-found", Map.of("currency", args[0]));
                return;
            }
        }

        String currencyId = currency == null ? null : currency.getId();
        String scope = currency == null ? "all currencies" : currency.getDisplayName();
        int required = plugin.settings().resetConfirmations();
        long windowMillis = plugin.settings().resetWindowSeconds() * 1000L;
        long now = System.currentTimeMillis();

        Confirmation previous = pending.get(senderKey);
        int count = 1;
        if (previous != null && Objects.equals(previous.currencyId(), currencyId)
                && now - previous.timestamp() <= windowMillis) {
            count = previous.count() + 1;
        }

        if (count < required) {
            pending.put(senderKey, new Confirmation(currencyId, count, now));
            plugin.messages().send(sender, "reset.warning", Map.of(
                    "count", Integer.toString(count),
                    "required", Integer.toString(required),
                    "remaining", Integer.toString(required - count),
                    "seconds", Integer.toString(plugin.settings().resetWindowSeconds()),
                    "scope", scope));
            return;
        }

        pending.remove(senderKey);
        plugin.economy().resetAll(currencyId).thenAccept(affected ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (affected < 0) {
                        plugin.messages().send(sender, "general.storage-error");
                        return;
                    }
                    plugin.messages().send(sender, "reset.done", Map.of(
                            "rows", Integer.toString(affected),
                            "scope", scope));
                }));
    }

    @Override
    public @NotNull List<String> complete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            return java.util.stream.Stream.concat(plugin.currencies().ids().stream(),
                            java.util.stream.Stream.of("cancel"))
                    .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }
}
