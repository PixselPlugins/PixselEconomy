# PixselEconomy API Examples

Every example assumes `softdepend: [PixselEconomy]` and a `JavaPlugin` subclass called `MyPlugin`.
All code is written against API `1.0.0`.

## 1. Reward a player for breaking a block

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

`addBalance` never blocks the server thread, and the message is sent back on the server thread.

## 2. Charge a player before opening a menu

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

Checking first gives a nicer message; the withdrawal itself still fails safely if the balance
changed in between, so no double spend is possible.

## 3. Pay a reward and tell the player their new balance

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

## 4. Read a balance instantly for a menu

```java
// Safe on the server thread, in a render loop, or from a scheduled task.
OptionalDouble balance = PixselEconomyAPI.getService()
        .getCachedBalance(player.getUniqueId(), "coins");
double coins = balance.orElse(0.0D);
```

Use `getCachedBalance` (never blocks) for displays, and `getBalance(...)` (future) when the player
may be offline or the value must be fresh from storage.

## 5. Move money between two players

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

The transfer is a single database transaction, so a failure leaves both accounts untouched.

## 6. React to balance changes

```java
public final class BalanceWatcher implements Listener {

    @EventHandler
    public void onBalanceChange(PixselBalanceChangeEvent event) {
        // Handlers may run off the server thread; re-schedule before touching Bukkit.
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

## 7. Veto a change

```java
@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
public void onBalanceChange(PixselBalanceChangeEvent event) {
    if (jailManager.isJailed(event.getPlayerId())) {
        event.setCancelled(true);
    }
}
```

The operation then completes with `TransactionStatus.CANCELLED` and no balance is written.

## 8. React to currencies being created or deleted

```java
@EventHandler
public void onCurrencyChange(PixselCurrencyChangeEvent event) {
    if (event.getType() == PixselCurrencyChangeEvent.Type.CREATE) {
        getLogger().info("New currency available: " + event.getCurrency().getId());
    }
}
```

## 9. Render a leaderboard on a hologram or sign

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

For anything that refreshes every tick, prefer the `%pixseleconomy_top_<position>_name%`
placeholders with PlaceholderAPI: they are cached and cost nothing per render.

## 10. Format amounts exactly like the plugin

```java
EconomyService economy = PixselEconomyAPI.getService();
String formatted = economy.format(1234.5D, "coins");        // "$1,234.50"
String plain = Currency.formatNumber(1234.5D, 2);           // "1,234.50"

economy.getCurrency("gems").ifPresent(currency -> {
    String gems = currency.format(42.0D);                    // "42 Gems"
    int decimals = currency.getDecimals();                  // 0
});
```

## 11. Optional dependency handling

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
