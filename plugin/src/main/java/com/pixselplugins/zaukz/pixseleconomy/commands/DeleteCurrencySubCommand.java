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
import com.pixselplugins.zaukz.pixseleconomy.api.PixselCurrencyChangeEvent;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /deletecurrency <id>}: removes a currency and every balance stored in it.
 *
 * <p>The default currency cannot be deleted, so the plugin always has a usable currency after a
 * reload.</p>
 */
public final class DeleteCurrencySubCommand implements SubCommand {

    private final PixselEconomy plugin;

    public DeleteCurrencySubCommand(PixselEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String name() {
        return "deletecurrency";
    }

    @Override
    public @NotNull String permission() {
        return "pixsel.economy.delete";
    }

    @Override
    public @NotNull String usage() {
        return "pixseleconomy deletecurrency <id>";
    }

    @Override
    public @NotNull String description() {
        return "Delete a currency and its balances.";
    }

    @Override
    public boolean standalone() {
        return false;
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 1) {
            sendUsage(plugin, sender);
            return;
        }
        Currency currency = plugin.currencies().get(args[0]);
        if (currency == null) {
            plugin.messages().send(sender, "currency.not-found", Map.of("currency", args[0]));
            return;
        }
        if (currency.getId().equals(plugin.currencies().defaultId())) {
            plugin.messages().send(sender, "currency.delete-default",
                    Map.of("currency", currency.getDisplayName()));
            return;
        }

        plugin.databaseExecutor().execute(() -> {
            int removed;
            try {
                removed = plugin.storage().deleteCurrency(currency.getId());
            } catch (SQLException exception) {
                plugin.getLogger().log(Level.SEVERE,
                        "Could not delete the balances of currency " + currency.getId() + ".", exception);
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> plugin.messages().send(sender, "general.storage-error"));
                return;
            }
            int rows = removed;
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.currencies().remove(currency.getId());
                plugin.economy().forgetCurrency(currency.getId());
                plugin.currencies().fireChange(PixselCurrencyChangeEvent.Type.DELETE, currency);
                plugin.messages().send(sender, "currency.deleted", Map.of(
                        "currency", currency.getDisplayName(),
                        "id", currency.getId(),
                        "rows", Integer.toString(rows)));
            });
        });
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
