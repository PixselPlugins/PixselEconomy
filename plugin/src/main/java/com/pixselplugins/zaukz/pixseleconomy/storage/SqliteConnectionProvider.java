/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keeps a single SQLite connection open for the plugin's lifetime.
 *
 * <p>SQLite allows one writer at a time; because every statement runs on the plugin's database
 * thread, one long-lived connection is both the fastest and the safest option. Write-ahead logging
 * keeps reads out of the way of writes and reduces the chance of a corrupt file after a crash.</p>
 */
public final class SqliteConnectionProvider implements ConnectionProvider {

    private final File file;
    private final Logger logger;
    private Connection connection;

    public SqliteConnectionProvider(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    @Override
    public Connection acquire() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return connection;
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            logger.warning("Could not create the directory for the SQLite database at " + parent.getPath() + ".");
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA synchronous=NORMAL");
            statement.execute("PRAGMA busy_timeout=5000");
        }
        return connection;
    }

    @Override
    public void release(Connection connection) {
        // The single connection stays open until the plugin disables.
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public String description() {
        return "SQLite (" + file.getName() + ")";
    }

    @Override
    public void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException exception) {
            logger.log(Level.WARNING, "Could not close the SQLite connection cleanly.", exception);
        } finally {
            connection = null;
        }
    }
}
