/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.integrations;

import com.pixselplugins.zaukz.pixseleconomy.PixselEconomy;
import com.pixselplugins.zaukz.pixseleconomy.api.Currency;
import com.pixselplugins.zaukz.pixseleconomy.api.LeaderboardEntry;
import com.pixselplugins.zaukz.pixseleconomy.utils.Numbers;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exposes economy values to PlaceholderAPI as {@code %pixseleconomy_<placeholder>%}.
 *
 * <p>Balance placeholders read the in-memory cache, so they are safe to use in scoreboards, boss
 * bars, action bars and chat without adding any database load. Leaderboard placeholders read a
 * snapshot that refreshes on a timer, because a placeholder must answer immediately.</p>
 */
public final class PixselPlaceholders extends PlaceholderExpansion {

    private enum BalanceStyle {
        PLAIN,
        FORMATTED,
        SHORT
    }

    private final PixselEconomy plugin;
    private final Map<String, List<LeaderboardEntry>> leaderboards = new ConcurrentHashMap<>();
    @Nullable
    private BukkitTask refreshTask;

    public PixselPlaceholders(PixselEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "pixseleconomy";
    }

    @Override
    public @NotNull String getAuthor() {
        return "zAukz";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    /**
     * Starts refreshing the leaderboard snapshot used by the {@code top_*} placeholders.
     */
    public void startRefreshTask() {
        stopRefreshTask();
        long ticks = 20L * plugin.settings().placeholderRefreshSeconds();
        refreshTask = plugin.getServer().getScheduler()
                .runTaskTimerAsynchronously(plugin, this::refreshLeaderboards, 20L, ticks);
    }

    private void stopRefreshTask() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    /**
     * Unregisters the expansion and stops its refresh task.
     */
    public void stop() {
        stopRefreshTask();
        unregister();
    }

    private void refreshLeaderboards() {
        int depth = plugin.settings().placeholderLeaderboardDepth();
        for (Currency currency : plugin.currencies().all()) {
            plugin.economy().getLeaderboard(currency.getId(), 1, depth)
                    .thenAccept(entries -> leaderboards.put(currency.getId(), List.copyOf(entries)));
        }
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
        if (params.isEmpty()) {
            return null;
        }
        String query = params.toLowerCase(Locale.ROOT);
        String defaultId = plugin.currencies().defaultId();

        switch (query) {
            case "currency":
                return defaultId;
            case "currency_count":
                return Integer.toString(plugin.currencies().all().size());
            case "currencies":
                return String.join(", ", plugin.currencies().ids());
            default:
                break;
        }

        Currency currency = currencyArgument(query, "balance_formatted");
        if (currency != null) {
            return balance(player, currency, BalanceStyle.FORMATTED);
        }
        currency = currencyArgument(query, "balance_short");
        if (currency != null) {
            return balance(player, currency, BalanceStyle.SHORT);
        }
        currency = currencyArgument(query, "balance");
        if (currency != null) {
            return balance(player, currency, BalanceStyle.PLAIN);
        }
        currency = currencyArgument(query, "currency_name");
        if (currency != null) {
            return currency.getDisplayName();
        }
        currency = currencyArgument(query, "currency_symbol");
        if (currency != null) {
            return currency.getSymbol();
        }

        if (query.startsWith("can_afford_")) {
            return canAfford(player, query.substring("can_afford_".length()), defaultId);
        }
        if (query.startsWith("top_")) {
            return topEntry(query.substring("top_".length()), defaultId);
        }
        return null;
    }

    private String canAfford(@Nullable OfflinePlayer player, String arguments, String defaultId) {
        int separator = arguments.lastIndexOf('_');
        String currencyId = separator < 0 ? defaultId : arguments.substring(0, separator);
        String amountText = separator < 0 ? arguments : arguments.substring(separator + 1);
        Currency currency = plugin.currencies().get(currencyId);
        Double amount = Numbers.parseAmount(amountText);
        if (player == null || currency == null || amount == null) {
            return "false";
        }
        OptionalDouble cached = plugin.economy().getCachedBalance(player.getUniqueId(), currency.getId());
        return cached.orElse(0.0D) >= amount ? "true" : "false";
    }

    private String topEntry(String arguments, String defaultId) {
        int fieldSeparator = arguments.lastIndexOf('_');
        if (fieldSeparator < 0) {
            return null;
        }
        String field = arguments.substring(fieldSeparator + 1);
        String head = arguments.substring(0, fieldSeparator);
        int positionSeparator = head.lastIndexOf('_');
        String positionText = positionSeparator < 0 ? head : head.substring(positionSeparator + 1);
        String currencyId = positionSeparator < 0 ? defaultId : head.substring(0, positionSeparator);

        Currency currency = plugin.currencies().get(currencyId);
        int position;
        try {
            position = Integer.parseInt(positionText);
        } catch (NumberFormatException exception) {
            return null;
        }
        if (currency == null || position < 1) {
            return null;
        }
        List<LeaderboardEntry> entries = leaderboards.getOrDefault(currency.getId(), List.of());
        if (position > entries.size()) {
            return "";
        }
        LeaderboardEntry entry = entries.get(position - 1);
        return switch (field) {
            case "name" -> entry.playerName();
            case "balance" -> currency.format(entry.balance());
            case "value" -> Currency.formatNumber(entry.balance(), currency.getDecimals());
            default -> null;
        };
    }

    @Nullable
    private Currency currencyArgument(String query, String prefix) {
        if (query.equals(prefix)) {
            return plugin.currencies().getDefault();
        }
        if (query.startsWith(prefix + "_")) {
            return plugin.currencies().get(query.substring(prefix.length() + 1));
        }
        return null;
    }

    private String balance(@Nullable OfflinePlayer player, Currency currency, BalanceStyle style) {
        double value = 0.0D;
        if (player != null) {
            value = plugin.economy().getCachedBalance(player.getUniqueId(), currency.getId()).orElse(0.0D);
        }
        return switch (style) {
            case PLAIN -> Currency.formatNumber(currency.round(value), currency.getDecimals());
            case FORMATTED -> currency.format(value);
            case SHORT -> Numbers.shortFormat(value, currency.getDecimals());
        };
    }
}
