/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy;

import com.pixselplugins.zaukz.pixseleconomy.api.PixselEconomyAPI;
import com.pixselplugins.zaukz.pixseleconomy.commands.CommandRegistry;
import com.pixselplugins.zaukz.pixseleconomy.config.MessageService;
import com.pixselplugins.zaukz.pixseleconomy.config.PluginSettings;
import com.pixselplugins.zaukz.pixseleconomy.integrations.PixselPlaceholders;
import com.pixselplugins.zaukz.pixseleconomy.integrations.PlaceholderApiBridge;
import com.pixselplugins.zaukz.pixseleconomy.listeners.BanknoteListener;
import com.pixselplugins.zaukz.pixseleconomy.listeners.PlayerConnectionListener;
import com.pixselplugins.zaukz.pixseleconomy.managers.BackupManager;
import com.pixselplugins.zaukz.pixseleconomy.managers.BanknoteManager;
import com.pixselplugins.zaukz.pixseleconomy.managers.CurrencyManager;
import com.pixselplugins.zaukz.pixseleconomy.managers.EconomyManager;
import com.pixselplugins.zaukz.pixseleconomy.storage.ConnectionProvider;
import com.pixselplugins.zaukz.pixseleconomy.storage.EconomyStorage;
import com.pixselplugins.zaukz.pixseleconomy.storage.MySqlConnectionProvider;
import com.pixselplugins.zaukz.pixseleconomy.storage.SqlEconomyStorage;
import com.pixselplugins.zaukz.pixseleconomy.storage.SqliteConnectionProvider;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PixselEconomy: a lightweight multi-currency economy with a public developer API.
 *
 * <p>Published by Pixsel Plugins, authored by zAukz.</p>
 */
public final class PixselEconomy extends JavaPlugin {

    private volatile PluginSettings settings;
    private MessageService messages;
    private CurrencyManager currencyManager;
    private ConnectionProvider connections;
    private EconomyStorage storage;
    private EconomyManager economyManager;
    private BanknoteManager banknoteManager;
    private BackupManager backupManager;
    private PixselPlaceholders placeholders;
    private ExecutorService databaseExecutor;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = PluginSettings.load(this);

        messages = new MessageService(this);
        messages.load();
        currencyManager = new CurrencyManager(this);

        databaseExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "PixselEconomy-Database");
            thread.setDaemon(true);
            return thread;
        });

        if (!initializeStorage()) {
            return;
        }

        currencyManager.load();
        economyManager = new EconomyManager(this, currencyManager, storage, databaseExecutor);
        banknoteManager = new BanknoteManager(this);
        PixselEconomyAPI.register(economyManager);

        new CommandRegistry(this).register();
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new BanknoteListener(this), this);

        backupManager = new BackupManager(this);
        backupManager.restart();
        registerPlaceholders();

        getLogger().info("Enabled PixselEconomy " + getPluginMeta().getVersion() + " with "
                + currencyManager.all().size() + " currency/currencies using " + connections.description() + ".");
    }

    @Override
    public void onDisable() {
        if (placeholders != null) {
            placeholders.stop();
            placeholders = null;
        }
        if (backupManager != null) {
            backupManager.stop();
        }
        PixselEconomyAPI.unregister(economyManager);
        if (databaseExecutor != null) {
            databaseExecutor.shutdown();
            try {
                if (!databaseExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    databaseExecutor.shutdownNow();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        if (storage != null) {
            storage.close();
        }
        getLogger().info("Disabled PixselEconomy.");
    }

    private boolean initializeStorage() {
        try {
            if (settings.storageType().equals("mysql")) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connections = new MySqlConnectionProvider(settings.mysqlHost(), settings.mysqlPort(),
                        settings.mysqlDatabase(), settings.mysqlUsername(), settings.mysqlPassword(),
                        settings.mysqlPoolSize(), settings.mysqlProperties(), getLogger());
            } else {
                connections = new SqliteConnectionProvider(new File(getDataFolder(), settings.sqliteFile()),
                        getLogger());
            }
            storage = new SqlEconomyStorage(connections);
            // Creating the schema is the only storage call made on the server thread: it must be
            // ready before players can join and it is a single statement.
            storage.initialize();
            debug("Storage ready: " + connections.description());
            return true;
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Could not initialise the storage backend; disabling PixselEconomy.",
                    exception);
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
    }

    /**
     * Re-reads every configuration file without restarting the server.
     */
    public void reloadPlugin() {
        PluginSettings previous = settings;
        reloadConfig();
        PluginSettings updated = PluginSettings.load(this);
        if (previous != null && !previous.usesSameStorage(updated)) {
            getLogger().warning("Storage settings changed; restart the server so the new database is used.");
        }
        settings = updated;
        messages.load();
        currencyManager.load();
        if (backupManager != null) {
            backupManager.restart();
        }
        registerPlaceholders();
    }

    private void registerPlaceholders() {
        if (placeholders != null) {
            placeholders.stop();
            placeholders = null;
        }
        if (!settings.placeholdersEnabled()) {
            return;
        }
        if (!PlaceholderApiBridge.isAvailable()) {
            debug("PlaceholderAPI is not installed, placeholders are unavailable.");
            return;
        }
        PixselPlaceholders expansion = new PixselPlaceholders(this);
        if (expansion.register()) {
            expansion.startRefreshTask();
            placeholders = expansion;
            debug("Registered the 'pixseleconomy' placeholder expansion.");
        } else {
            getLogger().warning("PlaceholderAPI rejected the PixselEconomy expansion.");
        }
    }

    /**
     * Writes a debug line when {@code settings.debug} is enabled.
     *
     * @param message the message to log
     */
    public void debug(@NotNull String message) {
        PluginSettings current = settings;
        if (current != null && current.debug()) {
            getLogger().info("[debug] " + message);
        }
    }

    /**
     * Returns the current settings snapshot.
     *
     * @return the settings, never {@code null} once the plugin is enabled
     */
    @NotNull
    public PluginSettings settings() {
        return settings;
    }

    /**
     * Returns the message service.
     *
     * @return the message service
     */
    @NotNull
    public MessageService messages() {
        return messages;
    }

    /**
     * Returns the currency registry.
     *
     * @return the currency manager
     */
    @NotNull
    public CurrencyManager currencies() {
        return currencyManager;
    }

    /**
     * Returns the economy engine.
     *
     * @return the economy manager
     */
    @NotNull
    public EconomyManager economy() {
        return economyManager;
    }

    /**
     * Returns the banknote manager.
     *
     * @return the banknote manager
     */
    @NotNull
    public BanknoteManager banknotes() {
        return banknoteManager;
    }

    /**
     * Returns the storage backend.
     *
     * @return the storage, or {@code null} before initialisation
     */
    @Nullable
    public EconomyStorage storage() {
        return storage;
    }

    /**
     * Returns the thread that owns every database call.
     *
     * <p>Anything that touches {@link #storage()} must be submitted here so SQLite never sees two
     * writers at once.</p>
     *
     * @return the database executor
     */
    @NotNull
    public ExecutorService databaseExecutor() {
        return databaseExecutor;
    }
}
