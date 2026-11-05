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
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * JDBC implementation of {@link EconomyStorage} for SQLite and MySQL/MariaDB.
 *
 * <p>SQLite and MySQL differ in their upsert syntax and in whether an index may be declared inside
 * {@code CREATE TABLE}, so the few statements that are not portable are selected by dialect. Every
 * value is sent through a prepared statement.</p>
 */
public final class SqlEconomyStorage implements EconomyStorage {

    private static final String TABLE = "pixsel_economy_balances";

    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE + " ("
            + "player_uuid VARCHAR(36) NOT NULL,"
            + "currency_id VARCHAR(32) NOT NULL,"
            + "player_name VARCHAR(16) NOT NULL DEFAULT '',"
            + "balance DOUBLE NOT NULL DEFAULT 0,"
            + "updated_at BIGINT NOT NULL DEFAULT 0,"
            + "PRIMARY KEY (player_uuid, currency_id))";

    private static final String CREATE_TABLE_MYSQL = CREATE_TABLE + " ENGINE=InnoDB"
            + " DEFAULT CHARSET=utf8mb4";

    private static final String CREATE_TOP_INDEX_SQLITE =
            "CREATE INDEX IF NOT EXISTS pixsel_economy_balances_top ON " + TABLE + " (currency_id, balance)";

    private static final String CREATE_TOP_INDEX_MYSQL =
            "CREATE INDEX pixsel_economy_balances_top ON " + TABLE + " (currency_id, balance)";

    private static final String UPSERT_SQLITE = "INSERT INTO " + TABLE
            + " (player_uuid, currency_id, player_name, balance, updated_at) VALUES (?, ?, ?, ?, ?)"
            + " ON CONFLICT(player_uuid, currency_id) DO UPDATE SET"
            + " balance = excluded.balance,"
            + " updated_at = excluded.updated_at,"
            + " player_name = CASE WHEN excluded.player_name = '' THEN player_name"
            + " ELSE excluded.player_name END";

    private static final String UPSERT_MYSQL = "INSERT INTO " + TABLE
            + " (player_uuid, currency_id, player_name, balance, updated_at) VALUES (?, ?, ?, ?, ?)"
            + " ON DUPLICATE KEY UPDATE"
            + " balance = VALUES(balance),"
            + " updated_at = VALUES(updated_at),"
            + " player_name = IF(VALUES(player_name) = '', player_name, VALUES(player_name))";

    private final ConnectionProvider connections;
    private final boolean remote;

    public SqlEconomyStorage(ConnectionProvider connections) {
        this.connections = connections;
        this.remote = connections.isRemote();
    }

    @Override
    public void initialize() throws SQLException {
        Connection connection = connections.acquire();
        try (Statement statement = connection.createStatement()) {
            statement.execute(remote ? CREATE_TABLE_MYSQL : CREATE_TABLE);
            if (remote) {
                try {
                    statement.execute(CREATE_TOP_INDEX_MYSQL);
                } catch (SQLException exception) {
                    // MySQL reports an error when the index already exists; that is expected.
                    if (!isDuplicateIndex(exception)) {
                        throw exception;
                    }
                }
            } else {
                statement.execute(CREATE_TOP_INDEX_SQLITE);
            }
        } finally {
            connections.release(connection);
        }
    }

    private static boolean isDuplicateIndex(SQLException exception) {
        String message = exception.getMessage();
        return message != null && message.toLowerCase().contains("duplicate key name");
    }

    @Override
    public Map<String, Double> loadBalances(UUID playerId) throws SQLException {
        Connection connection = connections.acquire();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT currency_id, balance FROM " + TABLE + " WHERE player_uuid = ?")) {
            statement.setString(1, playerId.toString());
            Map<String, Double> balances = new HashMap<>();
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    balances.put(result.getString("currency_id"), result.getDouble("balance"));
                }
            }
            return balances;
        } finally {
            connections.release(connection);
        }
    }

    @Override
    public OptionalDouble findBalance(UUID playerId, String currencyId) throws SQLException {
        Connection connection = connections.acquire();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT balance FROM " + TABLE + " WHERE player_uuid = ? AND currency_id = ?")) {
            statement.setString(1, playerId.toString());
            statement.setString(2, currencyId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return OptionalDouble.of(result.getDouble("balance"));
                }
            }
            return OptionalDouble.empty();
        } finally {
            connections.release(connection);
        }
    }

    @Override
    public void saveBalance(UUID playerId, String playerName, String currencyId, double balance)
            throws SQLException {
        Connection connection = connections.acquire();
        try (PreparedStatement statement = connection.prepareStatement(remote ? UPSERT_MYSQL : UPSERT_SQLITE)) {
            bindUpsert(statement, playerId, playerName, currencyId, balance);
            statement.executeUpdate();
        } finally {
            connections.release(connection);
        }
    }

    private static void bindUpsert(PreparedStatement statement, UUID playerId, String playerName,
                                   String currencyId, double balance) throws SQLException {
        statement.setString(1, playerId.toString());
        statement.setString(2, currencyId);
        statement.setString(3, playerName == null ? "" : playerName);
        statement.setDouble(4, balance);
        statement.setLong(5, System.currentTimeMillis());
    }

    @Override
    public void transferBalances(UUID fromId, String fromName, double fromBalance, UUID toId, String toName,
                                 double toBalance, String currencyId) throws SQLException {
        Connection connection = connections.acquire();
        boolean previousAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(remote ? UPSERT_MYSQL : UPSERT_SQLITE)) {
                bindUpsert(statement, fromId, fromName, currencyId, fromBalance);
                statement.executeUpdate();
                bindUpsert(statement, toId, toName, currencyId, toBalance);
                statement.executeUpdate();
            }
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
            connections.release(connection);
        }
    }

    @Override
    public int resetBalances(@Nullable String currencyId, double value) throws SQLException {
        Connection connection = connections.acquire();
        String sql = currencyId == null
                ? "UPDATE " + TABLE + " SET balance = ?, updated_at = ?"
                : "UPDATE " + TABLE + " SET balance = ?, updated_at = ? WHERE currency_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, value);
            statement.setLong(2, System.currentTimeMillis());
            if (currencyId != null) {
                statement.setString(3, currencyId);
            }
            return statement.executeUpdate();
        } finally {
            connections.release(connection);
        }
    }

    @Override
    public void touchPlayerName(UUID playerId, String playerName) throws SQLException {
        Connection connection = connections.acquire();
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE " + TABLE + " SET player_name = ? WHERE player_uuid = ?")) {
            statement.setString(1, playerName);
            statement.setString(2, playerId.toString());
            statement.executeUpdate();
        } finally {
            connections.release(connection);
        }
    }

    @Override
    public List<LeaderboardEntry> topBalances(String currencyId, int limit, int offset) throws SQLException {
        Connection connection = connections.acquire();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT player_uuid, player_name, balance FROM " + TABLE
                        + " WHERE currency_id = ? ORDER BY balance DESC, player_uuid ASC LIMIT ? OFFSET ?")) {
            statement.setString(1, currencyId);
            statement.setInt(2, limit);
            statement.setInt(3, offset);
            List<LeaderboardEntry> entries = new ArrayList<>(limit);
            try (ResultSet result = statement.executeQuery()) {
                int position = offset;
                while (result.next()) {
                    position++;
                    String name = result.getString("player_name");
                    entries.add(new LeaderboardEntry(
                            UUID.fromString(result.getString("player_uuid")),
                            name == null || name.isBlank() ? "Unknown" : name,
                            result.getDouble("balance"),
                            position));
                }
            }
            return entries;
        } finally {
            connections.release(connection);
        }
    }

    @Override
    public int deleteCurrency(String currencyId) throws SQLException {
        Connection connection = connections.acquire();
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM " + TABLE + " WHERE currency_id = ?")) {
            statement.setString(1, currencyId);
            return statement.executeUpdate();
        } finally {
            connections.release(connection);
        }
    }

    @Override
    public int backup(Path target) throws SQLException, IOException {
        return remote ? dumpToFile(target) : copyToAttachedDatabase(target);
    }

    /**
     * SQLite can copy table contents straight into another database file, which produces a real
     * database the owner can open or swap in.
     */
    private int copyToAttachedDatabase(Path target) throws SQLException {
        Connection connection = connections.acquire();
        try (PreparedStatement attach = connection.prepareStatement("ATTACH DATABASE ? AS pixsel_backup")) {
            attach.setString(1, target.toAbsolutePath().toString());
            attach.execute();
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS pixsel_backup." + TABLE);
            statement.execute("CREATE TABLE pixsel_backup." + TABLE + " AS SELECT * FROM main." + TABLE);
            int rows = countRows(connection, TABLE);
            statement.execute("DETACH DATABASE pixsel_backup");
            return rows;
        } catch (SQLException exception) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("DETACH DATABASE pixsel_backup");
            } catch (SQLException ignored) {
                // The detach is best effort; the original failure is more useful.
            }
            throw exception;
        } finally {
            connections.release(connection);
        }
    }

    private int countRows(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    /**
     * Remote databases are exported as portable SQL so the dump can be replayed anywhere.
     */
    private int dumpToFile(Path target) throws SQLException, IOException {
        Connection connection = connections.acquire();
        int rows = 0;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT player_uuid, currency_id, player_name, balance, updated_at FROM " + TABLE);
             ResultSet result = statement.executeQuery();
             BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            writer.write("-- PixselEconomy backup generated " + java.time.Instant.now());
            writer.newLine();
            writer.write(CREATE_TABLE_MYSQL + ";");
            writer.newLine();
            while (result.next()) {
                writer.write("INSERT INTO " + TABLE
                        + " (player_uuid, currency_id, player_name, balance, updated_at) VALUES ('");
                writer.write(escape(result.getString("player_uuid")));
                writer.write("', '");
                writer.write(escape(result.getString("currency_id")));
                writer.write("', '");
                writer.write(escape(result.getString("player_name")));
                writer.write("', ");
                writer.write(Double.toString(result.getDouble("balance")));
                writer.write(", ");
                writer.write(Long.toString(result.getLong("updated_at")));
                writer.write(");");
                writer.newLine();
                rows++;
            }
        } finally {
            connections.release(connection);
        }
        return rows;
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    @Override
    public String backupExtension() {
        return remote ? ".sql" : ".db";
    }

    @Override
    public void close() {
        connections.close();
    }
}
