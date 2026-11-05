/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.commands;

import com.pixselplugins.zaukz.pixseleconomy.PixselEconomy;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /pixseleconomy reload}: re-reads {@code config.yml}, {@code messages.yml} and
 * {@code currencies.yml} without a server restart.
 *
 * <p>Changing the storage backend requires a restart, because existing connections cannot be
 * migrated while the server is running; the plugin warns when that happens.</p>
 */
public final class ReloadSubCommand implements SubCommand {

    private final PixselEconomy plugin;

    public ReloadSubCommand(PixselEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String name() {
        return "reload";
    }

    @Override
    public @NotNull String permission() {
        return "pixsel.economy.reload";
    }

    @Override
    public @NotNull String usage() {
        return "pixseleconomy reload";
    }

    @Override
    public @NotNull String description() {
        return "Reload the configuration files.";
    }

    @Override
    public boolean standalone() {
        return false;
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        long start = System.nanoTime();
        plugin.reloadPlugin();
        long elapsed = (System.nanoTime() - start) / 1_000_000L;
        plugin.messages().send(sender, "general.reloaded", Map.of("time", Long.toString(elapsed)));
    }
}
