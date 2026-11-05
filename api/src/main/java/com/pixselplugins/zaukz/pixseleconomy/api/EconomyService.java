/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Read and write access to the currency system of PixselEconomy.
 *
 * <p>Obtain the live instance through {@link PixselEconomyAPI#getService()}. The interface is
 * implemented by the plugin; never implement it yourself.</p>
 *
 * <h2>Threading</h2>
 * <p>Every method that returns a {@link CompletableFuture} is non-blocking and safe to call from
 * any thread, including the server thread. The work is performed on a dedicated database thread
 * and the returned future completes on that thread, so Bukkit API access inside a
 * {@code thenAccept} callback must be re-scheduled onto the server thread.</p>
 *
 * <p>{@link #getCachedBalance(UUID, String)} and {@link #isPlayerLoaded(UUID)} only read a
 * concurrent cache and never touch storage. They may be called as often as needed, including from
 * placeholders and render loops.</p>
 *
 * <h2>Events</h2>
 * <p>Every balance change fires a cancellable {@link PixselBalanceChangeEvent} before it is
 * persisted; see that class for its threading contract.</p>
 *
 * @since 1.0.0
 */
public interface EconomyService {

    /**
     * Returns the API version exposed by the installed plugin.
     *
     * @return the API version, for example {@code "1.0.0"}
     */
    String getApiVersion();

    /**
     * Returns the identifier of the currency used when no currency is specified.
     *
     * @return the default currency identifier, never {@code null}
     */
    String getDefaultCurrencyId();

    /**
     * Looks up a currency by its identifier. Identifiers are case-insensitive.
     *
     * @param currencyId the currency identifier
     * @return the currency, or an empty optional if no such currency exists
     */
    Optional<Currency> getCurrency(String currencyId);

    /**
     * Returns the default currency.
     *
     * @return the default currency, or an empty optional if none is configured
     */
    Optional<Currency> getDefaultCurrency();

    /**
     * Returns every configured currency. The returned collection is an immutable snapshot.
     *
     * @return all currencies, never {@code null}
     */
    Collection<Currency> getCurrencies();

    /**
     * Returns the cached balance of a player without touching storage.
     *
     * <p>Only players currently tracked by the plugin (online players, or players with an
     * in-flight operation) have cached balances. Use {@link #getBalance(UUID, String)} when the
     * player may be offline.</p>
     *
     * @param playerId   the player UUID
     * @param currencyId the currency identifier
     * @return the cached balance, or an empty optional if it is not cached
     */
    OptionalDouble getCachedBalance(UUID playerId, String currencyId);

    /**
     * Reads a balance from storage, using the cache when it is already loaded.
     *
     * @param playerId   the player UUID
     * @param currencyId the currency identifier
     * @return a future completing with the balance, or with {@code 0} if the currency is unknown
     */
    CompletableFuture<Double> getBalance(UUID playerId, String currencyId);

    /**
     * Checks whether an account holds at least the given amount.
     *
     * @param playerId   the player UUID
     * @param currencyId the currency identifier
     * @param amount     the amount to check for
     * @return a future completing with {@code true} when the balance is sufficient
     */
    CompletableFuture<Boolean> hasBalance(UUID playerId, String currencyId, double amount);

    /**
     * Adds money to an account.
     *
     * @param playerId   the player UUID
     * @param currencyId the currency identifier
     * @param amount     the amount to add, must be greater than zero
     * @param cause      the reason shown to event listeners
     * @return a future completing with the outcome of the operation
     */
    CompletableFuture<TransactionResult> deposit(UUID playerId, String currencyId, double amount,
                                                BalanceChangeCause cause);

    /**
     * Removes money from an account. The balance never drops below zero.
     *
     * @param playerId   the player UUID
     * @param currencyId the currency identifier
     * @param amount     the amount to remove, must be greater than zero
     * @param cause      the reason shown to event listeners
     * @return a future completing with the outcome of the operation
     */
    CompletableFuture<TransactionResult> withdraw(UUID playerId, String currencyId, double amount,
                                                 BalanceChangeCause cause);

    /**
     * Sets an account balance to an exact value.
     *
     * @param playerId   the player UUID
     * @param currencyId the currency identifier
     * @param amount     the new balance, zero or greater
     * @param cause      the reason shown to event listeners
     * @return a future completing with the outcome of the operation
     */
    CompletableFuture<TransactionResult> setBalance(UUID playerId, String currencyId, double amount,
                                                    BalanceChangeCause cause);

    /**
     * Moves money between two accounts in a single operation.
     *
     * <p>The transfer is all-or-nothing: either both accounts are updated or neither is.</p>
     *
     * @param fromPlayerId the paying account
     * @param toPlayerId   the receiving account
     * @param currencyId   the currency identifier
     * @param amount       the amount to transfer, must be greater than zero
     * @param cause        the reason shown to event listeners
     * @return a future completing with the outcome of the operation
     */
    CompletableFuture<TransactionResult> transfer(UUID fromPlayerId, UUID toPlayerId, String currencyId,
                                                  double amount, BalanceChangeCause cause);

    /**
     * Returns one page of the richest accounts for a currency.
     *
     * <p>Pages are one-based. Entries are ordered from the highest balance down.</p>
     *
     * @param currencyId the currency identifier
     * @param page       the one-based page number
     * @param pageSize   the number of entries per page
     * @return a future completing with the entries of the requested page
     */
    CompletableFuture<List<LeaderboardEntry>> getLeaderboard(String currencyId, int page, int pageSize);

    /**
     * Formats an amount using the currency's configured decimals, symbol and format template.
     *
     * @param amount     the amount to format
     * @param currencyId the currency identifier
     * @return the formatted amount, or the plain number if the currency is unknown
     */
    String format(double amount, String currencyId);

    /**
     * Checks whether the plugin currently tracks an account for a player.
     *
     * <p>Accounts are cached while a player is online and after an operation on their account.
     * This method never touches storage.</p>
     *
     * @param playerId the player UUID
     * @return {@code true} when the account is cached
     */
    boolean isPlayerLoaded(UUID playerId);
}
