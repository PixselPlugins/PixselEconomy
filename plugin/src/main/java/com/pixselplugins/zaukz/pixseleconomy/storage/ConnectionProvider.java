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
import java.sql.SQLException;

/**
 * Supplies JDBC connections for the storage layer.
 *
 * <p>Every call is made from the plugin's single database thread, so implementations do not need
 * to be safe for concurrent use.</p>
 */
public interface ConnectionProvider extends AutoCloseable {

    /**
     * Returns a connection that is ready for use.
     *
     * @return an open connection
     * @throws SQLException when no connection can be established
     */
    Connection acquire() throws SQLException;

    /**
     * Returns a connection to the provider.
     *
     * @param connection the connection to give back, may be {@code null}
     */
    void release(Connection connection);

    /**
     * Returns whether the backend is reached over the network.
     *
     * @return {@code true} for remote databases
     */
    boolean isRemote();

    /**
     * Returns a short human-readable description for log output.
     *
     * @return the description, never {@code null}
     */
    String description();

    @Override
    void close();
}
