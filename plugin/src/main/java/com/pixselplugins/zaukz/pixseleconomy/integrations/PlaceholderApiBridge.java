/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.integrations;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Isolates every reference to PlaceholderAPI so the plugin still starts when PlaceholderAPI is not
 * installed. Callers must check {@link #isAvailable()} first, which never touches the optional
 * classes.
 */
public final class PlaceholderApiBridge {

    private PlaceholderApiBridge() {
    }

    /**
     * Checks whether PlaceholderAPI is installed and enabled.
     *
     * @return {@code true} when placeholders can be resolved
     */
    public static boolean isAvailable() {
        Plugin placeholderApi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        return placeholderApi != null && placeholderApi.isEnabled();
    }

    /**
     * Resolves PlaceholderAPI placeholders in a string for a player.
     *
     * @param player the player context
     * @param text   the text containing placeholders
     * @return the resolved text
     */
    @NotNull
    public static String apply(@NotNull Player player, @NotNull String text) {
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
    }
}
