/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.managers;

import com.pixselplugins.zaukz.pixseleconomy.PixselEconomy;
import com.pixselplugins.zaukz.pixseleconomy.api.BalanceChangeCause;
import com.pixselplugins.zaukz.pixseleconomy.api.Currency;
import com.pixselplugins.zaukz.pixseleconomy.api.EconomyService;
import com.pixselplugins.zaukz.pixseleconomy.api.LeaderboardEntry;
import com.pixselplugins.zaukz.pixseleconomy.api.PixselBalanceChangeEvent;
import com.pixselplugins.zaukz.pixseleconomy.api.PixselEconomyAPI;
import com.pixselplugins.zaukz.pixseleconomy.api.TransactionResult;
import com.pixselplugins.zaukz.pixseleconomy.api.TransactionStatus;
import com.pixselplugins.zaukz.pixseleconomy.storage.EconomyStorage;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The economy engine and implementation of the public {@link EconomyService}.
 *
 * <p>Balances of tracked players live in a concurrent cache so reads and placeholders never touch
 * the database. Every write is validated, announced through
 * {@link PixselBalanceChangeEvent} and persisted on the plugin's single database thread; a change
 * that fails validation never reaches storage.</p>
 */
public final class EconomyManager implements EconomyService {

    private enum Mode {
        ADD,
        REMOVE,
        SET
    }

    private final PixselEconomy plugin;
    private final CurrencyManager currencies;
    private final EconomyStorage storage;
    private final ExecutorService executor;

    private final Map<UUID, Map<String, Double>> cache = new ConcurrentHashMap<>();
    private final Set<UUID> online = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();

    public EconomyManager(PixselEconomy plugin, CurrencyManager currencies, EconomyStorage storage,
                          ExecutorService executor) {
        this.plugin = plugin;
        this.currencies = currencies;
        this.storage = storage;
        this.executor = executor;
    }

    /**
     * Starts tracking a joining player: loads the account and creates missing currencies with their
     * starting balance.
     *
     * @param player the player who joined
     */
    public void handleJoin(@NotNull Player player) {
        UUID playerId = player.getUniqueId();
        String name = player.getName();
        names.put(playerId, name);
        online.add(playerId);
        executor.execute(() -> {
            try {
                accountFor(playerId, name);
            } catch (SQLException exception) {
                plugin.getLogger().log(Level.SEVERE, "Could not load the account of " + name + ".", exception);
            }
        });
    }

    /**
     * Stops tracking a player who left. Their balances are already persisted, so dropping the cache
     * entry only frees memory.
     *
     * @param playerId the player UUID
     */
    public void handleQuit(@NotNull UUID playerId) {
        online.remove(playerId);
        names.remove(playerId);
        cache.remove(playerId);
    }

    /**
     * Drops cached values of a currency that was deleted.
     *
     * @param currencyId the removed currency identifier
     */
    public void forgetCurrency(@NotNull String currencyId) {
        String key = currencyId.toLowerCase(Locale.ROOT);
        for (Map<String, Double> account : cache.values()) {
            account.remove(key);
        }
    }

    /**
     * Resets every balance, either of one currency or of all of them.
     *
     * <p>Runs entirely on the database thread. Cached accounts are updated in place so online
     * players see the reset immediately.</p>
     *
     * @param currencyId the currency to reset, or {@code null} for every currency
     * @return a future completing with the number of affected rows, or {@code -1} on failure
     */
    @NotNull
    public CompletableFuture<Integer> resetAll(@Nullable String currencyId) {
        return CompletableFuture.supplyAsync(() -> {
            List<Currency> targets = new ArrayList<>();
            if (currencyId == null) {
                targets.addAll(currencies.all());
            } else {
                Currency currency = currencies.get(currencyId);
                if (currency == null) {
                    return -1;
                }
                targets.add(currency);
            }
            boolean toStartingBalance = plugin.settings().resetToStartingBalance();
            int affected = 0;
            try {
                for (Currency currency : targets) {
                    double value = toStartingBalance ? currency.round(currency.getDefaultBalance()) : 0.0D;
                    affected += storage.resetBalances(currency.getId(), value);
                    for (Map<String, Double> account : cache.values()) {
                        account.put(currency.getId(), value);
                    }
                }
                return affected;
            } catch (SQLException exception) {
                plugin.getLogger().log(Level.SEVERE, "Could not reset balances.", exception);
                return -1;
            }
        }, executor);
    }

    @Override
    public String getApiVersion() {
        return PixselEconomyAPI.API_VERSION;
    }

    @Override
    public String getDefaultCurrencyId() {
        return currencies.defaultId();
    }

    @Override
    public Optional<Currency> getCurrency(String currencyId) {
        return Optional.ofNullable(currencies.get(currencyId));
    }

    @Override
    public Optional<Currency> getDefaultCurrency() {
        return Optional.ofNullable(currencies.get(currencies.defaultId()));
    }

    @Override
    public Collection<Currency> getCurrencies() {
        return currencies.all();
    }

    @Override
    public OptionalDouble getCachedBalance(UUID playerId, String currencyId) {
        Map<String, Double> account = cache.get(playerId);
        if (account == null) {
            return OptionalDouble.empty();
        }
        Double value = account.get(currencyId.toLowerCase(Locale.ROOT));
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    @Override
    public CompletableFuture<Double> getBalance(UUID playerId, String currencyId) {
        String resolved = resolveCurrencyId(currencyId);
        Currency currency = currencies.get(resolved);
        if (currency == null) {
            return CompletableFuture.completedFuture(0.0D);
        }
        Map<String, Double> account = cache.get(playerId);
        if (account != null) {
            Double cached = account.get(currency.getId());
            if (cached != null) {
                return CompletableFuture.completedFuture(cached);
            }
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Double> loaded = accountFor(playerId, nameOf(playerId));
                return loaded.getOrDefault(currency.getId(), currency.getDefaultBalance());
            } catch (SQLException exception) {
                plugin.getLogger().log(Level.SEVERE,
                        "Could not read the balance of " + playerId + " in " + currency.getId() + ".", exception);
                return currency.getDefaultBalance();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> hasBalance(UUID playerId, String currencyId, double amount) {
        return getBalance(playerId, currencyId).thenApply(balance -> balance >= amount);
    }

    @Override
    public CompletableFuture<TransactionResult> deposit(UUID playerId, String currencyId, double amount,
                                                       BalanceChangeCause cause) {
        return change(playerId, currencyId, amount, cause, Mode.ADD);
    }

    @Override
    public CompletableFuture<TransactionResult> withdraw(UUID playerId, String currencyId, double amount,
                                                        BalanceChangeCause cause) {
        return change(playerId, currencyId, amount, cause, Mode.REMOVE);
    }

    @Override
    public CompletableFuture<TransactionResult> setBalance(UUID playerId, String currencyId, double amount,
                                                           BalanceChangeCause cause) {
        return change(playerId, currencyId, amount, cause, Mode.SET);
    }

    @Override
    public CompletableFuture<TransactionResult> transfer(UUID fromPlayerId, UUID toPlayerId, String currencyId,
                                                        double amount, BalanceChangeCause cause) {
        String resolved = resolveCurrencyId(currencyId);
        return CompletableFuture.supplyAsync(() -> {
            Currency currency = currencies.get(resolved);
            if (currency == null) {
                return TransactionResult.failure(TransactionStatus.UNKNOWN_CURRENCY, resolved, amount);
            }
            if (!Double.isFinite(amount)) {
                return TransactionResult.failure(TransactionStatus.INVALID_AMOUNT, currency.getId(), amount);
            }
            double requested = currency.round(amount);
            if (requested <= 0) {
                return TransactionResult.failure(TransactionStatus.INVALID_AMOUNT, currency.getId(), amount);
            }
            try {
                String fromName = nameOf(fromPlayerId);
                String toName = nameOf(toPlayerId);
                Map<String, Double> fromAccount = accountFor(fromPlayerId, fromName);
                Map<String, Double> toAccount = accountFor(toPlayerId, toName);

                double fromCurrent = fromAccount.getOrDefault(currency.getId(), currency.getDefaultBalance());
                double toCurrent = toAccount.getOrDefault(currency.getId(), currency.getDefaultBalance());
                double fromNext = currency.round(fromCurrent - requested);
                if (fromNext < 0) {
                    return TransactionResult.failure(TransactionStatus.INSUFFICIENT_FUNDS, currency.getId(), requested);
                }
                double toNext = currency.round(toCurrent + requested);
                if (toNext > currency.getMaxBalance()) {
                    return TransactionResult.failure(TransactionStatus.LIMIT_EXCEEDED, currency.getId(), requested);
                }
                if (fromCurrent == fromNext && toCurrent == toNext) {
                    return TransactionResult.success(currency.getId(), requested, fromCurrent, fromNext);
                }
                if (!fireChange(fromPlayerId, currency, fromCurrent, fromNext, requested, cause)
                        || !fireChange(toPlayerId, currency, toCurrent, toNext, requested, cause)) {
                    return TransactionResult.failure(TransactionStatus.CANCELLED, currency.getId(), requested);
                }
                storage.transferBalances(fromPlayerId, fromName, fromNext, toPlayerId, toName, toNext, currency.getId());
                fromAccount.put(currency.getId(), fromNext);
                toAccount.put(currency.getId(), toNext);
                return TransactionResult.success(currency.getId(), requested, fromCurrent, fromNext);
            } catch (SQLException exception) {
                plugin.getLogger().log(Level.SEVERE, "Could not transfer " + requested + " " + resolved + ".", exception);
                return TransactionResult.failure(TransactionStatus.STORAGE_ERROR, currency.getId(), requested);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<LeaderboardEntry>> getLeaderboard(String currencyId, int page, int pageSize) {
        String resolved = resolveCurrencyId(currencyId);
        int safePage = Math.max(1, page);
        int safeSize = Math.clamp(pageSize, 1, 100);
        return CompletableFuture.supplyAsync(() -> {
            if (currencies.get(resolved) == null) {
                return List.of();
            }
            try {
                return storage.topBalances(resolved, safeSize, (safePage - 1) * safeSize);
            } catch (SQLException exception) {
                plugin.getLogger().log(Level.SEVERE, "Could not read the leaderboard of " + resolved + ".", exception);
                return List.of();
            }
        }, executor);
    }

    @Override
    public String format(double amount, String currencyId) {
        Currency currency = currencies.get(resolveCurrencyId(currencyId));
        return currency == null ? Currency.formatNumber(amount, 2) : currency.format(amount);
    }

    @Override
    public boolean isPlayerLoaded(UUID playerId) {
        return cache.containsKey(playerId);
    }

    private CompletableFuture<TransactionResult> change(UUID playerId, String currencyId, double amount,
                                                        BalanceChangeCause cause, Mode mode) {
        String resolved = resolveCurrencyId(currencyId);
        return CompletableFuture.supplyAsync(() -> {
            Currency currency = currencies.get(resolved);
            if (currency == null) {
                return TransactionResult.failure(TransactionStatus.UNKNOWN_CURRENCY, resolved, amount);
            }
            if (!Double.isFinite(amount)) {
                return TransactionResult.failure(TransactionStatus.INVALID_AMOUNT, currency.getId(), amount);
            }
            double requested = currency.round(amount);
            boolean invalid = mode == Mode.SET ? requested < 0 : requested <= 0;
            if (invalid) {
                return TransactionResult.failure(TransactionStatus.INVALID_AMOUNT, currency.getId(), amount);
            }
            try {
                String name = nameOf(playerId);
                Map<String, Double> account = accountFor(playerId, name);
                double current = account.getOrDefault(currency.getId(), currency.getDefaultBalance());
                double target = switch (mode) {
                    case ADD -> current + requested;
                    case REMOVE -> current - requested;
                    case SET -> requested;
                };
                if (mode == Mode.REMOVE && target < 0) {
                    return TransactionResult.failure(TransactionStatus.INSUFFICIENT_FUNDS, currency.getId(), requested);
                }
                if (target > currency.getMaxBalance()) {
                    return TransactionResult.failure(TransactionStatus.LIMIT_EXCEEDED, currency.getId(), requested);
                }
                double next = currency.round(Math.max(target, 0.0D));
                if (next == current) {
                    return TransactionResult.success(currency.getId(), requested, current, current);
                }
                if (!fireChange(playerId, currency, current, next, requested, cause)) {
                    return TransactionResult.failure(TransactionStatus.CANCELLED, currency.getId(), requested);
                }
                storage.saveBalance(playerId, name, currency.getId(), next);
                account.put(currency.getId(), next);
                return TransactionResult.success(currency.getId(), requested, current, next);
            } catch (SQLException exception) {
                plugin.getLogger().log(Level.SEVERE,
                        "Could not change the balance of " + playerId + " in " + resolved + ".", exception);
                return TransactionResult.failure(TransactionStatus.STORAGE_ERROR, currency.getId(), requested);
            }
        }, executor);
    }

    /**
     * Fires the cancellable change event on the current thread and reports whether it survived.
     * Storage only writes after this returns {@code true}, so listeners can veto a change.
     */
    private boolean fireChange(UUID playerId, Currency currency, double previous, double next, double amount,
                               BalanceChangeCause cause) {
        PixselBalanceChangeEvent event = new PixselBalanceChangeEvent(
                playerId, currency.getId(), previous, next, amount, cause, !Bukkit.isPrimaryThread());
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    private Map<String, Double> accountFor(UUID playerId, String name) throws SQLException {
        Map<String, Double> cached = cache.get(playerId);
        if (cached != null) {
            return cached;
        }
        Map<String, Double> stored = storage.loadBalances(playerId);
        Map<String, Double> account = new ConcurrentHashMap<>();
        String storedName = name == null ? "" : name;
        for (Currency currency : currencies.all()) {
            Double existing = stored.get(currency.getId());
            if (existing == null) {
                double starting = currency.round(currency.getDefaultBalance());
                storage.saveBalance(playerId, storedName, currency.getId(), starting);
                account.put(currency.getId(), starting);
            } else {
                account.put(currency.getId(), existing);
            }
        }
        if (!storedName.isEmpty()) {
            storage.touchPlayerName(playerId, storedName);
        }
        if (online.contains(playerId)) {
            cache.put(playerId, account);
        }
        return account;
    }

    private String nameOf(UUID playerId) {
        String name = names.get(playerId);
        return name == null ? "" : name;
    }

    private String resolveCurrencyId(@Nullable String currencyId) {
        if (currencyId == null || currencyId.isBlank()) {
            return currencies.defaultId();
        }
        return currencyId.toLowerCase(Locale.ROOT);
    }
}
