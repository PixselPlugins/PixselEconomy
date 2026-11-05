/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.config;

import com.pixselplugins.zaukz.pixseleconomy.PixselEconomy;
import java.util.Locale;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Immutable snapshot of {@code config.yml}. A reload builds a new instance, so every reader sees
 * either the old or the new values and never a half-applied mix.
 */
public final class PluginSettings {

    private final boolean debug;
    private final String defaultCurrencyId;
    private final boolean payEnabled;
    private final double payMinimum;
    private final double payTaxPercentage;
    private final boolean payAllowSelf;
    private final boolean withdrawEnabled;
    private final String withdrawItem;
    private final double withdrawMinimum;
    private final double withdrawMaximum;
    private final boolean withdrawProtectCrafting;
    private final boolean leaderboardEnabled;
    private final int leaderboardPageSize;
    private final boolean placeholdersEnabled;
    private final int placeholderLeaderboardDepth;
    private final int placeholderRefreshSeconds;
    private final int resetConfirmations;
    private final int resetWindowSeconds;
    private final boolean resetToStartingBalance;
    private final String storageType;
    private final String sqliteFile;
    private final String mysqlHost;
    private final int mysqlPort;
    private final String mysqlDatabase;
    private final String mysqlUsername;
    private final String mysqlPassword;
    private final int mysqlPoolSize;
    private final String mysqlProperties;
    private final boolean backupEnabled;
    private final int backupIntervalMinutes;
    private final String backupDirectory;
    private final int backupKeep;
    private final boolean placeholderApiIntegration;

    private PluginSettings(FileConfiguration config, Logger logger) {
        this.debug = config.getBoolean("settings.debug", false);
        this.defaultCurrencyId = config.getString("general.default-currency", "coins").toLowerCase(Locale.ROOT);

        this.payEnabled = config.getBoolean("general.pay.enabled", true);
        this.payMinimum = nonNegative(config, "general.pay.minimum-amount", 0.01D, logger);
        this.payTaxPercentage = percentage(config, "general.pay.tax-percentage", 0.0D, logger);
        this.payAllowSelf = config.getBoolean("general.pay.allow-self-payment", false);

        this.withdrawEnabled = config.getBoolean("features.withdraw.enabled", true);
        this.withdrawItem = config.getString("features.withdraw.item", "PAPER");
        this.withdrawMinimum = nonNegative(config, "features.withdraw.minimum-amount", 1.0D, logger);
        this.withdrawMaximum = nonNegative(config, "features.withdraw.maximum-amount", 100_000.0D, logger);
        this.withdrawProtectCrafting = config.getBoolean("features.withdraw.protect-from-crafting", true);

        this.leaderboardEnabled = config.getBoolean("features.leaderboard.enabled", true);
        this.leaderboardPageSize = atLeast(config, "features.leaderboard.entries-per-page", 10, 1, 50, logger);

        this.placeholdersEnabled = config.getBoolean("features.placeholders.enabled", true);
        this.placeholderLeaderboardDepth = atLeast(config, "features.placeholders.leaderboard-depth", 10, 1, 50, logger);
        this.placeholderRefreshSeconds = atLeast(
                config, "features.placeholders.leaderboard-refresh-seconds", 60, 10, 3600, logger);

        this.resetConfirmations = atLeast(config, "safety.reset-all.required-confirmations", 5, 1, 20, logger);
        this.resetWindowSeconds = atLeast(config, "safety.reset-all.confirmation-window-seconds", 30, 5, 600, logger);
        this.resetToStartingBalance = config.getBoolean("safety.reset-all.reset-to-starting-balance", false);

        this.storageType = config.getString("storage.type", "sqlite").toLowerCase(Locale.ROOT);
        this.sqliteFile = config.getString("storage.sqlite.file", "database.db");
        this.mysqlHost = config.getString("storage.mysql.host", "localhost");
        this.mysqlPort = atLeast(config, "storage.mysql.port", 3306, 1, 65535, logger);
        this.mysqlDatabase = config.getString("storage.mysql.database", "pixseleconomy");
        this.mysqlUsername = config.getString("storage.mysql.username", "username");
        this.mysqlPassword = config.getString("storage.mysql.password", "password");
        this.mysqlPoolSize = atLeast(config, "storage.mysql.pool-size", 4, 1, 32, logger);
        this.mysqlProperties = config.getString("storage.mysql.properties", "");

        this.backupEnabled = config.getBoolean("storage.backup.enabled", true);
        this.backupIntervalMinutes = atLeast(config, "storage.backup.interval-minutes", 60, 1, 1440, logger);
        this.backupDirectory = config.getString("storage.backup.directory", "backups");
        this.backupKeep = atLeast(config, "storage.backup.keep", 5, 0, 500, logger);

        this.placeholderApiIntegration = config.getBoolean("integrations.placeholderapi", true);

        if (!storageType.equals("sqlite") && !storageType.equals("mysql")) {
            logger.warning("Unknown storage type '" + storageType + "' in config.yml, falling back to sqlite.");
        }
    }

    /**
     * Reads {@code config.yml} and validates every value, falling back to safe defaults.
     *
     * @param plugin the plugin providing the configuration and logger
     * @return an immutable settings snapshot
     */
    public static PluginSettings load(PixselEconomy plugin) {
        return new PluginSettings(plugin.getConfig(), plugin.getLogger());
    }

    private static double nonNegative(FileConfiguration config, String path, double fallback, Logger logger) {
        double value = config.getDouble(path, fallback);
        if (!Double.isFinite(value) || value < 0) {
            logger.warning("Invalid value for " + path + " in config.yml, using " + fallback + ".");
            return fallback;
        }
        return value;
    }

    private static double percentage(FileConfiguration config, String path, double fallback, Logger logger) {
        double value = nonNegative(config, path, fallback, logger);
        if (value > 100) {
            logger.warning("Value for " + path + " in config.yml exceeds 100, using 100.");
            return 100;
        }
        return value;
    }

    private static int atLeast(FileConfiguration config, String path, int fallback, int minimum, int maximum,
                               Logger logger) {
        int value = config.getInt(path, fallback);
        if (value < minimum || value > maximum) {
            logger.warning("Value for " + path + " in config.yml must be between " + minimum + " and "
                    + maximum + ", using " + fallback + ".");
            return fallback;
        }
        return value;
    }

    /**
     * Checks whether the storage configuration still matches, so a reload can warn that switching
     * backends requires a restart.
     *
     * @param other the newer settings
     * @return {@code true} when the same backend with the same target is configured
     */
    public boolean usesSameStorage(PluginSettings other) {
        if (!storageType.equals(other.storageType)) {
            return false;
        }
        if (storageType.equals("mysql")) {
            return mysqlHost.equals(other.mysqlHost) && mysqlPort == other.mysqlPort
                    && mysqlDatabase.equals(other.mysqlDatabase) && mysqlUsername.equals(other.mysqlUsername);
        }
        return sqliteFile.equals(other.sqliteFile);
    }

    public boolean debug() {
        return debug;
    }

    public String defaultCurrencyId() {
        return defaultCurrencyId;
    }

    public boolean payEnabled() {
        return payEnabled;
    }

    public double payMinimum() {
        return payMinimum;
    }

    public double payTaxPercentage() {
        return payTaxPercentage;
    }

    public boolean payAllowSelf() {
        return payAllowSelf;
    }

    public boolean withdrawEnabled() {
        return withdrawEnabled;
    }

    public String withdrawItem() {
        return withdrawItem;
    }

    public double withdrawMinimum() {
        return withdrawMinimum;
    }

    public double withdrawMaximum() {
        return withdrawMaximum;
    }

    public boolean withdrawProtectCrafting() {
        return withdrawProtectCrafting;
    }

    public boolean leaderboardEnabled() {
        return leaderboardEnabled;
    }

    public int leaderboardPageSize() {
        return leaderboardPageSize;
    }

    public boolean placeholdersEnabled() {
        return placeholdersEnabled;
    }

    public int placeholderLeaderboardDepth() {
        return placeholderLeaderboardDepth;
    }

    public int placeholderRefreshSeconds() {
        return placeholderRefreshSeconds;
    }

    public int resetConfirmations() {
        return resetConfirmations;
    }

    public int resetWindowSeconds() {
        return resetWindowSeconds;
    }

    public boolean resetToStartingBalance() {
        return resetToStartingBalance;
    }

    public String storageType() {
        return storageType;
    }

    public String sqliteFile() {
        return sqliteFile;
    }

    public String mysqlHost() {
        return mysqlHost;
    }

    public int mysqlPort() {
        return mysqlPort;
    }

    public String mysqlDatabase() {
        return mysqlDatabase;
    }

    public String mysqlUsername() {
        return mysqlUsername;
    }

    public String mysqlPassword() {
        return mysqlPassword;
    }

    public int mysqlPoolSize() {
        return mysqlPoolSize;
    }

    public String mysqlProperties() {
        return mysqlProperties;
    }

    public boolean backupEnabled() {
        return backupEnabled;
    }

    public int backupIntervalMinutes() {
        return backupIntervalMinutes;
    }

    public String backupDirectory() {
        return backupDirectory;
    }

    public int backupKeep() {
        return backupKeep;
    }

    public boolean placeholderApiIntegration() {
        return placeholderApiIntegration;
    }
}
