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
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Redeems banknotes on right click and keeps them out of crafting recipes.
 */
public final class BanknoteListener implements Listener {

    private final PixselEconomy plugin;

    public BanknoteListener(PixselEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getHand() == null) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || plugin.banknotes().currencyOf(item) == null) {
            return;
        }
        event.setCancelled(true);
        plugin.banknotes().redeem(event.getPlayer(), item, event.getHand());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(@NotNull PrepareItemCraftEvent event) {
        if (!plugin.settings().withdrawProtectCrafting()) {
            return;
        }
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && plugin.banknotes().currencyOf(item) != null) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }
}
