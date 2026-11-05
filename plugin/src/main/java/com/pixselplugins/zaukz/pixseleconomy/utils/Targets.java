/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves player names to accounts.
 *
 * <p>Only online players and players the server has already seen are accepted. That keeps every
 * command instant: resolving an unknown name would otherwise require a blocking web request.</p>
 */
public final class Targets {

    private Targets() {
    }

    /**
     * Finds a player by exact name.
     *
     * @param name the name entered in a command
     * @return the player, or {@code null} when they are not online and not cached
     */
    @Nullable
    public static OfflinePlayer find(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    /**
     * Returns a display name for an account.
     *
     * @param player the account
     * @return the last known name, or the UUID when the name is unknown
     */
    @NotNull
    public static String name(@NotNull OfflinePlayer player) {
        String name = player.getName();
        return name == null ? player.getUniqueId().toString() : name;
    }
}
