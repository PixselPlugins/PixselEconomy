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
import com.pixselplugins.zaukz.pixseleconomy.config.PluginSettings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

/**
 * Writes periodic copies of the economy data.
 *
 * <p>The live economy always reads and writes the main database; backups are only ever written.
 * Deleting a backup file, or the whole backup folder, therefore cannot break anything, because the next
 * run simply creates a new file.</p>
 */
public final class BackupManager {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final PixselEconomy plugin;
    @Nullable
    private BukkitTask task;

    public BackupManager(PixselEconomy plugin) {
        this.plugin = plugin;
    }

    /**
     * Schedules backups according to the current settings, replacing any previous schedule.
     */
    public void restart() {
        stop();
        PluginSettings settings = plugin.settings();
        if (!settings.backupEnabled()) {
            return;
        }
        long intervalTicks = 20L * 60L * settings.backupIntervalMinutes();
        task = plugin.getServer().getScheduler()
                .runTaskTimerAsynchronously(plugin, this::scheduleBackup, intervalTicks, intervalTicks);
    }

    /**
     * Cancels the scheduled task. An already running backup finishes on its own.
     */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void scheduleBackup() {
        // The backup must run on the database thread: SQLite allows a single writer.
        plugin.databaseExecutor().execute(this::runBackup);
    }

    private void runBackup() {
        PluginSettings settings = plugin.settings();
        Path directory = plugin.getDataFolder().toPath().resolve(settings.backupDirectory());
        try {
            Files.createDirectories(directory);
            Path target = directory.resolve("backup-" + TIMESTAMP.format(LocalDateTime.now())
                    + plugin.storage().backupExtension());
            int rows = plugin.storage().backup(target);
            int removed = prune(directory, settings.backupKeep());
            plugin.getLogger().info("Backup " + target.getFileName() + " written (" + rows + " rows)"
                    + (removed > 0 ? ", " + removed + " old backup(s) removed" : "") + ".");
        } catch (IOException | java.sql.SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not write a database backup.", exception);
        }
    }

    private int prune(Path directory, int keep) {
        if (keep <= 0) {
            return 0;
        }
        String extension = plugin.storage().backupExtension();
        try (Stream<Path> files = Files.list(directory)) {
            List<Path> backups = files
                    .filter(path -> path.getFileName().toString().startsWith("backup-"))
                    .filter(path -> path.getFileName().toString().endsWith(extension))
                    .sorted(Comparator.comparingLong(BackupManager::lastModified).reversed())
                    .toList();
            int removed = 0;
            for (int index = keep; index < backups.size(); index++) {
                Files.deleteIfExists(backups.get(index));
                removed++;
            }
            return removed;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not clean up old database backups.", exception);
            return 0;
        }
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            return 0L;
        }
    }
}
