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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /createcurrency <id> [display name]}: adds a currency to {@code currencies.yml}.
 *
 * <p>The same result is reachable by editing {@code currencies.yml} and running
 * {@code /pixseleconomy reload}.</p>
 */
public final class CreateCurrencySubCommand implements SubCommand {

    private final PixselEconomy plugin;

    public CreateCurrencySubCommand(PixselEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String name() {
        return "createcurrency";
    }

    @Override
    public @NotNull String permission() {
        return "pixsel.economy.create";
    }

    @Override
    public @NotNull String usage() {
        return "pixseleconomy createcurrency <id> [display name]";
    }

    @Override
    public @NotNull String description() {
        return "Create a new currency.";
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
        String id = args[0].toLowerCase(Locale.ROOT);
        if (!plugin.currencies().isValidId(id)) {
            plugin.messages().send(sender, "currency.invalid-id", Map.of("id", args[0]));
            return;
        }
        if (plugin.currencies().exists(id)) {
            plugin.messages().send(sender, "currency.already-exists", Map.of("id", id));
            return;
        }
        String displayName = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : Character.toUpperCase(id.charAt(0)) + id.substring(1);

        Currency currency = Currency.builder(id)
                .displayName(displayName)
                .withdrawMinimum(plugin.settings().withdrawMinimum())
                .withdrawMaximum(plugin.settings().withdrawMaximum())
                .build();

        if (!plugin.currencies().create(currency)) {
            plugin.messages().send(sender, "currency.already-exists", Map.of("id", id));
            return;
        }
        plugin.currencies().fireChange(PixselCurrencyChangeEvent.Type.CREATE, currency);
        plugin.messages().send(sender, "currency.created",
                Map.of("currency", currency.getDisplayName(), "id", currency.getId()));
    }

    @Override
    public @NotNull List<String> complete(@NotNull CommandSender sender, @NotNull String[] args) {
        return List.of();
    }
}
