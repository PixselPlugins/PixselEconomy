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
import com.pixselplugins.zaukz.pixseleconomy.api.BalanceChangeCause;
import com.pixselplugins.zaukz.pixseleconomy.api.Currency;
import com.pixselplugins.zaukz.pixseleconomy.api.TransactionResult;
import com.pixselplugins.zaukz.pixseleconomy.utils.StatusMessages;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Creates and redeems withdrawal banknotes.
 *
 * <p>A banknote is an ordinary item carrying its currency and face value in its persistent data
 * container, so the value survives restarts, server transfers and item frames. Redeeming deposits
 * the face value through the economy service and only removes the item when the deposit succeeded.</p>
 */
public final class BanknoteManager {

    private final PixselEconomy plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final NamespacedKey currencyKey;
    private final NamespacedKey amountKey;

    public BanknoteManager(PixselEconomy plugin) {
        this.plugin = plugin;
        this.currencyKey = new NamespacedKey(plugin, "banknote_currency");
        this.amountKey = new NamespacedKey(plugin, "banknote_amount");
    }

    /**
     * Returns the currency a stack represents.
     *
     * @param item the item to inspect, may be {@code null}
     * @return the currency, or {@code null} when the item is not a banknote
     */
    @Nullable
    public Currency currencyOf(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        String id = meta.getPersistentDataContainer().get(currencyKey, PersistentDataType.STRING);
        return id == null ? null : plugin.currencies().get(id);
    }

    /**
     * Returns the face value of a banknote.
     *
     * @param item the item to inspect, may be {@code null}
     * @return the face value, or {@code 0} when the item is not a banknote
     */
    public double amountOf(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0.0D;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0.0D;
        }
        Double amount = meta.getPersistentDataContainer().get(amountKey, PersistentDataType.DOUBLE);
        return amount == null ? 0.0D : amount;
    }

    /**
     * Turns part of a balance into a banknote. Validation is immediate; the withdrawal, item
     * creation and messages happen once the balance change completes.
     *
     * @param player   the player withdrawing
     * @param currency the currency to withdraw
     * @param input    the requested face value
     */
    public void withdraw(@NotNull Player player, @NotNull Currency currency, double input) {
        if (!plugin.settings().withdrawEnabled() || !currency.isWithdrawable()) {
            plugin.messages().send(player, "withdraw.disabled");
            return;
        }
        double face = currency.round(input);
        if (!Double.isFinite(input) || face <= 0) {
            plugin.messages().send(player, "general.invalid-amount",
                    Map.of("input", Double.toString(input), "currency", currency.getDisplayName()));
            return;
        }
        if (face < currency.getWithdrawMinimum()) {
            plugin.messages().send(player, "withdraw.below-minimum",
                    Map.of("minimum", currency.format(currency.getWithdrawMinimum()),
                            "currency", currency.getDisplayName()));
            return;
        }
        if (face > currency.getWithdrawMaximum()) {
            plugin.messages().send(player, "withdraw.above-maximum",
                    Map.of("maximum", currency.format(currency.getWithdrawMaximum()),
                            "currency", currency.getDisplayName()));
            return;
        }

        double fee = currency.round(currency.getWithdrawFee()
                + face * currency.getWithdrawFeePercentage() / 100.0D);
        double total = currency.round(face + fee);

        plugin.economy().withdraw(player.getUniqueId(), currency.getId(), total, BalanceChangeCause.WITHDRAW_ISSUE)
                .thenAccept(result -> plugin.getServer().getScheduler().runTask(plugin,
                        () -> completeWithdraw(player, currency, face, fee, total, result)));
    }

    private void completeWithdraw(Player player, Currency currency, double face, double fee, double total,
                                  TransactionResult result) {
        if (!result.isSuccessful()) {
            StatusMessages.send(plugin.messages(), plugin.currencies(), player, result,
                    Map.of("amount", currency.format(total)));
            return;
        }
        ItemStack banknote = createBanknote(currency, face);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(banknote);
        if (!leftovers.isEmpty()) {
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
            plugin.messages().send(player, "withdraw.inventory-full");
        }
        plugin.messages().send(player, "withdraw.success", Map.of(
                "amount", currency.format(face),
                "fee", currency.format(fee),
                "total", currency.format(total),
                "balance", currency.format(result.getNewBalance()),
                "currency", currency.getDisplayName()));
    }

    /**
     * Builds the banknote item for a currency and face value.
     *
     * @param currency the currency the note is worth
     * @param face     the face value
     * @return a new banknote item
     */
    @NotNull
    public ItemStack createBanknote(@NotNull Currency currency, double face) {
        Material material = Material.matchMaterial(plugin.settings().withdrawItem());
        if (material == null || !material.isItem()) {
            material = Material.PAPER;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("currency", currency.getDisplayName());
        placeholders.put("currency_id", currency.getId());
        placeholders.put("symbol", currency.getSymbol());
        placeholders.put("amount", currency.formatNumber(face, currency.getDecimals()));
        placeholders.put("value", currency.format(face));

        meta.displayName(miniMessage.deserialize(plugin.messages().rawFormat("withdraw.item-name", placeholders)));
        List<Component> lore = new ArrayList<>();
        for (String line : plugin.messages().rawList("withdraw.item-lore", placeholders)) {
            lore.add(miniMessage.deserialize(line));
        }
        meta.lore(lore);
        meta.getPersistentDataContainer().set(currencyKey, PersistentDataType.STRING, currency.getId());
        meta.getPersistentDataContainer().set(amountKey, PersistentDataType.DOUBLE, face);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Redeems a banknote held by a player, removing exactly one item on success.
     *
     * @param player the player
     * @param item   the held stack
     * @param hand   the hand holding it
     */
    public void redeem(@NotNull Player player, @NotNull ItemStack item, @NotNull EquipmentSlot hand) {
        Currency currency = currencyOf(item);
        double face = amountOf(item);
        if (currency == null || face <= 0) {
            plugin.messages().send(player, "withdraw.invalid-item");
            return;
        }
        plugin.economy().deposit(player.getUniqueId(), currency.getId(), face, BalanceChangeCause.WITHDRAW_REDEEM)
                .thenAccept(result -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!result.isSuccessful()) {
                        StatusMessages.send(plugin.messages(), plugin.currencies(), player, result,
                                Map.of("amount", currency.format(face)));
                        return;
                    }
                    removeOne(player, hand);
                    plugin.messages().send(player, "withdraw.redeemed", Map.of(
                            "amount", currency.format(face),
                            "balance", currency.format(result.getNewBalance()),
                            "currency", currency.getDisplayName()));
                }));
    }

    private void removeOne(Player player, EquipmentSlot hand) {
        if (hand == EquipmentSlot.OFF_HAND) {
            ItemStack held = player.getInventory().getItemInOffHand();
            held.setAmount(held.getAmount() - 1);
            player.getInventory().setItemInOffHand(held);
        } else {
            ItemStack held = player.getInventory().getItemInMainHand();
            held.setAmount(held.getAmount() - 1);
            player.getInventory().setItemInMainHand(held);
        }
    }
}
