/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.storage;

import com.pixselplugins.zaukz.pixseleconomy.api.LeaderboardEntry;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Persistence contract for balances.
 *
 * <p>Every method performs blocking database work and must run on the plugin's database thread.
 * Implementations are free to keep state between calls; nothing is expected to be safe for
 * concurrent use.</p>
 */
public interface EconomyStorage extends AutoCloseable {

    /**
     * Creates the schema when it does not exist yet.
     *
     * @throws SQLException when the schema cannot be prepared
     */
    void initialize() throws SQLException;

    /**
     * Loads every stored balance of an account.
     *
     * @param playerId the account owner
     * @return balances keyed by currency identifier, missing currencies are absent from the map
     * @throws SQLException when the query fails
     */
    Map<String, Double> loadBalances(UUID playerId) throws SQLException;

    /**
     * Reads a single stored balance.
     *
     * @param playerId   the account owner
     * @param currencyId the currency identifier
     * @return the balance, or an empty optional when no row exists
     * @throws SQLException when the query fails
     */
    OptionalDouble findBalance(UUID playerId, String currencyId) throws SQLException;

    /**
     * Stores a balance, inserting the row when it does not exist yet.
     *
     * @param playerId   the account owner
     * @param playerName the last known name, an empty string keeps the stored name
     * @param currencyId the currency identifier
     * @param balance    the new balance
     * @throws SQLException when the write fails
     */
    void saveBalance(UUID playerId, String playerName, String currencyId, double balance) throws SQLException;

    /**
     * Stores two balances in one transaction so a transfer cannot be applied half-way.
     *
     * @param fromId         the paying account
     * @param fromName       the last known name of the paying account
     * @param fromBalance    the new balance of the paying account
     * @param toId           the receiving account
     * @param toName         the last known name of the receiving account
     * @param toBalance      the new balance of the receiving account
     * @param currencyId     the currency identifier
     * @throws SQLException when the transaction fails and was rolled back
     */
    void transferBalances(UUID fromId, String fromName, double fromBalance, UUID toId, String toName,
                          double toBalance, String currencyId) throws SQLException;

    /**
     * Overwrites every balance of one currency, or of all currencies.
     *
     * @param currencyId the currency identifier, or {@code null} for every currency
     * @param value      the value written to every matching row
     * @return the number of affected rows
     * @throws SQLException when the update fails
     */
    int resetBalances(@Nullable String currencyId, double value) throws SQLException;

    /**
     * Updates the stored name of an account.
     *
     * @param playerId   the account owner
     * @param playerName the new name
     * @throws SQLException when the update fails
     */
    void touchPlayerName(UUID playerId, String playerName) throws SQLException;

    /**
     * Reads one page of the richest accounts of a currency.
     *
     * @param currencyId the currency identifier
     * @param limit      the page size
     * @param offset     the number of rows to skip
     * @return the entries of the page, ordered by balance descending
     * @throws SQLException when the query fails
     */
    List<LeaderboardEntry> topBalances(String currencyId, int limit, int offset) throws SQLException;

    /**
     * Deletes every balance of a currency.
     *
     * @param currencyId the currency identifier
     * @return the number of deleted rows
     * @throws SQLException when the delete fails
     */
    int deleteCurrency(String currencyId) throws SQLException;

    /**
     * Writes a copy of the economy data to a file.
     *
     * <p>Backups are written only. Nothing in the plugin ever reads them, so deleting a backup file
     * can never break the live economy.</p>
     *
     * @param target the file to write
     * @return the number of rows written
     * @throws SQLException when the database cannot be read
     * @throws IOException  when the file cannot be written
     */
    int backup(Path target) throws SQLException, IOException;

    /**
     * Returns the file extension used for backups of this backend.
     *
     * @return {@code ".db"} for embedded databases, {@code ".sql"} for remote ones
     */
    String backupExtension();

    @Override
    void close();
}
