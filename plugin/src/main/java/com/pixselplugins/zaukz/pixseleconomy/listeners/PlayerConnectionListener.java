/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.listeners;

import com.pixselplugins.zaukz.pixseleconomy.PixselEconomy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Loads an account when a player joins and drops the cache entry when they leave.
 *
 * <p>Loading happens on the database thread, so joining players never wait on storage.</p>
 */
public final class PlayerConnectionListener implements Listener {

    private final PixselEconomy plugin;

    public PlayerConnectionListener(PixselEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        plugin.economy().handleJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        plugin.economy().handleQuit(event.getPlayer().getUniqueId());
    }
}
