# PixselEconomy API Examples

This document provides practical examples of how to integrate PixselEconomy into your own Paper plugins using the public Java API.

All examples are written against API version `1.0.0` and assume your plugin uses `softdepend: [PixselEconomy]` with a `JavaPlugin` subclass named `MyPlugin`.

## 1. Reward Players for Breaking Blocks

This example rewards players with 5 coins whenever they break diamond ore.

```java
public final class MiningRewardListener implements Listener {

    private final MyPlugin plugin;

    public MiningRewardListener(MyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.DIAMOND_ORE) {
            return;
        }
        if (!PixselEconomyAPI.isAvailable()) {
            return;
        }
        PixselEconomyAPI.addBalance(event.getPlayer(), 5.0D, "coins")
                .thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (result.isSuccessful()) {
                        event.getPlayer().sendActionBar(Component.text("+5 coins"));
                    }
                }));
    }
}
```

`addBalance()` runs asynchronously and does not block the server thread. The reward message is scheduled back onto the server thread after the transaction completes.

## 2. Charge Players Before Opening a Menu

You can check whether a player has enough funds before allowing them to access a shop or menu.

```java
public void openShop(Player player) {
    EconomyService economy = PixselEconomyAPI.getService();
    double price = 250.0D;

    economy.hasBalance(player.getUniqueId(), "coins", price).thenAccept(hasFunds ->
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!hasFunds) {
                    player.sendMessage("You cannot afford that.");
                    return;
                }
                economy.withdraw(player.getUniqueId(), "coins", price, BalanceChangeCause.PLUGIN)
                        .thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> {
                            if (result.isSuccessful()) {
                                openShopMenu(player);
                            } else {
                                player.sendMessage("Purchase failed: " + result.getStatus());
                            }
                        }));
            }));
}
```

Checking the balance first allows you to display a more helpful message when a player cannot afford something.

The withdrawal operation still validates the balance when it executes, so if the player's balance changes between the check and the withdrawal, the transaction will fail safely.

## 3. Reward Players and Display Their New Balance

This example deposits gems into a player's account and displays their updated balance.

```java
public void reward(Player player, double amount) {
    PixselEconomyAPI.getService().deposit(player.getUniqueId(), "gems", amount, BalanceChangeCause.PLUGIN)
            .thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (result.isSuccessful()) {
                    player.sendMessage("+" + amount + " gems, you now have " + result.getNewBalance());
                } else {
                    plugin.getLogger().warning("Could not reward gems: " + result.getStatus());
                }
            }));
}
```

The `TransactionResult` provides the player's balance before and after a successful transaction, allowing you to display updated economy information without making another balance request.

## 4. Read Player Balances Instantly

For menus, scoreboards, and other frequently updated displays, you can use the cached balance instead of requesting data from storage.

```java
// Safe on the server thread, in a render loop, or from a scheduled task.
OptionalDouble balance = PixselEconomyAPI.getService()
        .getCachedBalance(player.getUniqueId(), "coins");
double coins = balance.orElse(0.0D);
```

`getCachedBalance()` is instant and never blocks the caller, making it suitable for frequently updated displays.

Use `getBalance()` instead when the player may be offline or when you need a fresh value from storage. It returns a `CompletableFuture`.

## 5. Transfer Money Between Players

The API allows you to transfer funds directly between two player accounts.

```java
PixselEconomyAPI.getService()
        .transfer(buyerId, sellerId, "coins", 500.0D, BalanceChangeCause.PLUGIN)
        .thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (result.isSuccessful()) {
                buyer.sendMessage("Paid the seller.");
            } else {
                buyer.sendMessage("Trade failed: " + result.getStatus());
            }
        }));
```

Transfers are processed as a single database transaction. If the operation fails, neither account is modified.

This makes the method suitable for player trading, shops, and other systems that need to move funds safely between accounts.

## 6. Monitor Player Balance Changes

You can listen for balance changes made through commands, API operations, and banknotes using `PixselBalanceChangeEvent`.

```java
public final class BalanceWatcher implements Listener {

    @EventHandler
    public void onBalanceChange(PixselBalanceChangeEvent event) {
        // Handlers may run off the server thread.
        // Re-schedule before accessing Bukkit API features.

        if (event.getCause() == BalanceChangeCause.API) {
            return;
        }
        Player player = Bukkit.getPlayer(event.getPlayerId());
        if (player == null) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.sendActionBar(Component.text(Currency.formatNumber(event.getNewBalance(), 2)));
        });
    }
}
```

The event provides information about the player, currency, previous balance, new balance, transaction amount, and cause.

**Important:** Balance change events may run asynchronously. Always schedule Bukkit API operations back onto the server thread when necessary.

## 7. Prevent Balance Changes

Balance change events are cancellable, allowing your plugin to prevent transactions under certain conditions.

For example, you can prevent jailed players from modifying their balances.

```java
@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
public void onBalanceChange(PixselBalanceChangeEvent event) {
    if (jailManager.isJailed(event.getPlayerId())) {
        event.setCancelled(true);
    }
}
```

Cancelling the event prevents the balance change from being written. The operation completes with `TransactionStatus.CANCELLED`.

## 8. Monitor Currency Creation and Deletion

You can listen for currency changes using `PixselCurrencyChangeEvent`.

```java
@EventHandler
public void onCurrencyChange(PixselCurrencyChangeEvent event) {
    if (event.getType() == PixselCurrencyChangeEvent.Type.CREATE) {
        getLogger().info("New currency available: " + event.getCurrency().getId());
    }
}
```

The event is fired when a currency is created or deleted, including changes made through administrative commands or configuration reloads.

It provides the change type and the affected `Currency` object.

## 9. Display Economy Leaderboards

The API can retrieve leaderboard entries for use in holograms, signs, menus, or other server displays.

```java
EconomyService economy = PixselEconomyAPI.getService();
economy.getLeaderboard("coins", 1, 5).thenAccept(entries -> {
    List<String> lines = new ArrayList<>();
    for (LeaderboardEntry entry : entries) {
        lines.add(entry.position() + ". " + entry.playerName() + " - " + entry.balance());
    }
    // Apply the lines on the server thread, or use the top_* placeholders instead.
    Bukkit.getScheduler().runTask(plugin, () -> hologram.setText(lines));
});
```

For displays that refresh frequently, consider using the `%pixseleconomy_top_<position>_name%` PlaceholderAPI placeholders instead.

Leaderboard placeholder data is cached, making it more suitable for frequently updated displays.

## 10. Format Currency Amounts

PixselEconomy provides built-in formatting methods so your plugin can display amounts consistently with the server's economy settings.

```java
EconomyService economy = PixselEconomyAPI.getService();
String formatted = economy.format(1234.5D, "coins");        // "$1,234.50"
String plain = Currency.formatNumber(1234.5D, 2);           // "1,234.50"

economy.getCurrency("gems").ifPresent(currency -> {
    String gems = currency.format(42.0D);                    // "42 Gems"
    int decimals = currency.getDecimals();                  // 0
});
```

Use `economy.format()` or `Currency#format()` when you want to follow the configured formatting for a particular currency.

Use `Currency.formatNumber()` when you only need a grouped number without the currency's display formatting.

## 11. Handle Optional Dependencies

If PixselEconomy is an optional integration, your plugin should check whether it is available before attempting to use its API.

**Example:**

```java
@Override
public void onEnable() {
    if (Bukkit.getPluginManager().getPlugin("PixselEconomy") != null) {
        getLogger().info("PixselEconomy detected, economy features enabled.");
    } else {
        getLogger().warning("PixselEconomy not found, economy features disabled.");
    }
}

private void paySomething(Player player) {
    EconomyService economy = PixselEconomyAPI.getServiceOrNull();
    if (economy == null) {
        return;
    }
    economy.deposit(player.getUniqueId(), "coins", 10.0D, BalanceChangeCause.PLUGIN);
}
```

`PixselEconomyAPI.getServiceOrNull()` returns `null` when PixselEconomy is unavailable instead of throwing an exception.

This is useful for plugins that support PixselEconomy integrations without requiring it to be installed.

---

**Published by:** Pixsel Plugins  
**Author:** zAukz  
**API Version:** `1.0.0`

## 💙 **Thank you for integrating with PixselEconomy!** 💙
