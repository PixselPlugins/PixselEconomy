/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * Small connection pool for MySQL and MariaDB.
 *
 * <p>Idle connections are reused and validated before being handed out; a dead connection (for
 * example after a database restart) is discarded and replaced instead of failing the operation.
 * The pool is intentionally minimal: the plugin issues few, short-lived queries on a single thread,
 * so a full pooling library would add a dependency without a measurable benefit.</p>
 */
public final class MySqlConnectionProvider implements ConnectionProvider {

    private final String url;
    private final String username;
    private final String password;
    private final int poolSize;
    private final Logger logger;
    private final Deque<Connection> idle = new ArrayDeque<>();

    private boolean closed;

    public MySqlConnectionProvider(String host, int port, String database, String username, String password,
                                   int poolSize, String properties, Logger logger) {
        String base = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?characterEncoding=utf8&useUnicode=true&serverTimezone=UTC";
        this.url = properties == null || properties.isBlank() ? base : base + "&" + properties;
        this.username = username;
        this.password = password;
        this.poolSize = poolSize;
        this.logger = logger;
    }

    @Override
    public synchronized Connection acquire() throws SQLException {
        if (closed) {
            throw new SQLException("The MySQL connection pool is closed.");
        }
        Connection pooled;
        while ((pooled = idle.pollFirst()) != null) {
            if (isUsable(pooled)) {
                return pooled;
            }
            closeQuietly(pooled);
        }
        return DriverManager.getConnection(url, username, password);
    }

    @Override
    public synchronized void release(@Nullable Connection connection) {
        if (connection == null) {
            return;
        }
        if (closed || idle.size() >= poolSize || !isUsable(connection)) {
            closeQuietly(connection);
            return;
        }
        idle.addLast(connection);
    }

    private boolean isUsable(Connection connection) {
        try {
            return !connection.isClosed() && connection.isValid(2);
        } catch (SQLException exception) {
            return false;
        }
    }

    private void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (SQLException exception) {
            logger.log(Level.FINE, "Could not close a MySQL connection.", exception);
        }
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public String description() {
        int start = url.indexOf("//") + 2;
        int end = url.indexOf('?');
        return "MySQL (" + (end < 0 ? url.substring(start) : url.substring(start, end)) + ")";
    }

    @Override
    public synchronized void close() {
        closed = true;
        Connection connection;
        while ((connection = idle.pollFirst()) != null) {
            closeQuietly(connection);
        }
    }
}
