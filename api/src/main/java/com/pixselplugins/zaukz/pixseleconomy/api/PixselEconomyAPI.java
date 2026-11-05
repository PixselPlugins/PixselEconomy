/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.api;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The entry point for third-party plugins.
 *
 * <p>PixselEconomy registers its {@link EconomyService} when it enables and removes it when it
 * disables. Call {@link #getService()} lazily (inside an event handler, command or scheduled task)
 * instead of caching the result during your own {@code onEnable}, because plugin load order is not
 * guaranteed.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * double balance = PixselEconomyAPI.getService()
 *         .getCachedBalance(player.getUniqueId(), "coins")
 *         .orElse(0.0D);
 *
 * PixselEconomyAPI.addBalance(player, 250.0D, "coins")
 *         .thenAccept(result -> getLogger().info("Added money: " + result.getStatus()));
 * }</pre>
 *
 * <p>Declare {@code PixselEconomy} in {@code softdepend} unless your plugin cannot work without it,
 * and guard every call with {@link #isAvailable()}.</p>
 *
 * @since 1.0.0
 */
public final class PixselEconomyAPI {

    /**
     * The API version exposed by this artifact. Compare it against
     * {@link EconomyService#getApiVersion()} when you need a specific feature.
     */
    public static final String API_VERSION = "1.0.0";

    private static volatile EconomyService service;

    private PixselEconomyAPI() {
        throw new AssertionError("Utility class");
    }

    /**
     * Publishes the economy service. Called by PixselEconomy itself; third-party plugins must not
     * call this method.
     *
     * @param economyService the live service
     */
    public static void register(@NotNull EconomyService economyService) {
        service = Objects.requireNonNull(economyService, "economyService");
    }

    /**
     * Removes a previously registered service. Called by PixselEconomy when it disables.
     *
     * @param economyService the service being removed; ignored when it is no longer registered
     */
    public static void unregister(@Nullable EconomyService economyService) {
        if (economyService != null && service == economyService) {
            service = null;
        }
    }

    /**
     * Checks whether PixselEconomy is installed and enabled.
     *
     * @return {@code true} when the API can be used
     */
    public static boolean isAvailable() {
        return service != null;
    }

    /**
     * Returns the economy service.
     *
     * @return the live service
     * @throws IllegalStateException when PixselEconomy is not installed or not enabled
     */
    @NotNull
    public static EconomyService getService() {
        EconomyService current = service;
        if (current == null) {
            throw new IllegalStateException("PixselEconomy is not installed or not enabled.");
        }
        return current;
    }

    /**
     * Returns the economy service if it is currently available.
     *
     * @return the service, or {@code null} when PixselEconomy is unavailable
     */
    @Nullable
    public static EconomyService getServiceOrNull() {
        return service;
    }

    /**
     * Adds money to a player's account.
     *
     * @param playerId   the player UUID
     * @param amount     the amount to add
     * @param currencyId the currency identifier, for example {@code "coins"}
     * @return a future completing with the outcome of the operation
     */
    @NotNull
    public static CompletableFuture<TransactionResult> addBalance(@NotNull UUID playerId, double amount,
                                                                 @NotNull String currencyId) {
        return getService().deposit(playerId, currencyId, amount, BalanceChangeCause.API);
    }

    /**
     * Adds money to a player's account.
     *
     * @param player     the player
     * @param amount     the amount to add
     * @param currencyId the currency identifier, for example {@code "coins"}
     * @return a future completing with the outcome of the operation
     */
    @NotNull
    public static CompletableFuture<TransactionResult> addBalance(@NotNull org.bukkit.OfflinePlayer player,
                                                                 double amount, @NotNull String currencyId) {
        return addBalance(player.getUniqueId(), amount, currencyId);
    }

    /**
     * Removes money from a player's account.
     *
     * @param playerId   the player UUID
     * @param amount     the amount to remove
     * @param currencyId the currency identifier
     * @return a future completing with the outcome of the operation
     */
    @NotNull
    public static CompletableFuture<TransactionResult> removeBalance(@NotNull UUID playerId, double amount,
                                                                    @NotNull String currencyId) {
        return getService().withdraw(playerId, currencyId, amount, BalanceChangeCause.API);
    }

    /**
     * Removes money from a player's account.
     *
     * @param player     the player
     * @param amount     the amount to remove
     * @param currencyId the currency identifier
     * @return a future completing with the outcome of the operation
     */
    @NotNull
    public static CompletableFuture<TransactionResult> removeBalance(@NotNull org.bukkit.OfflinePlayer player,
                                                                    double amount, @NotNull String currencyId) {
        return removeBalance(player.getUniqueId(), amount, currencyId);
    }

    /**
     * Sets a player's balance to an exact value.
     *
     * @param playerId   the player UUID
     * @param amount     the new balance
     * @param currencyId the currency identifier
     * @return a future completing with the outcome of the operation
     */
    @NotNull
    public static CompletableFuture<TransactionResult> setBalance(@NotNull UUID playerId, double amount,
                                                                 @NotNull String currencyId) {
        return getService().setBalance(playerId, currencyId, amount, BalanceChangeCause.API);
    }

    /**
     * Reads a player's balance from the cache when possible, otherwise from storage.
     *
     * @param playerId   the player UUID
     * @param currencyId the currency identifier
     * @return a future completing with the balance
     */
    @NotNull
    public static CompletableFuture<Double> getBalance(@NotNull UUID playerId, @NotNull String currencyId) {
        return getService().getBalance(playerId, currencyId);
    }

    /**
     * Moves money between two accounts.
     *
     * @param fromPlayerId the paying account
     * @param toPlayerId   the receiving account
     * @param amount       the amount to transfer
     * @param currencyId   the currency identifier
     * @return a future completing with the outcome of the operation
     */
    @NotNull
    public static CompletableFuture<TransactionResult> transfer(@NotNull UUID fromPlayerId, @NotNull UUID toPlayerId,
                                                               double amount, @NotNull String currencyId) {
        return getService().transfer(fromPlayerId, toPlayerId, currencyId, amount, BalanceChangeCause.API);
    }
}
