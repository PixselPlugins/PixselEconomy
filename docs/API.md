# PixselEconomy API

The PixselEconomy API allows other Paper plugins to access economy features directly, including player balances, transactions, currency information, and economy events.

It provides a straightforward way to integrate with PixselEconomy without accessing its database, configuration files, or internal implementation.

- **API Version:** `1.0.0` (`PixselEconomyAPI.API_VERSION`)
- **Plugin Version:** `1.0.0`
- **Java Package:** `com.pixselplugins.zaukz.pixseleconomy.api`
- **Runtime:** API classes are bundled inside `PixselEconomy-1.0.0.jar`, which must be installed on the server. No separate runtime library is required.

## Adding the Dependency

The API is included directly in `PixselEconomy-1.0.0.jar`. Download the plugin JAR from the Releases page and add it to your project as a compile-only dependency.

**Gradle:**

```groovy
dependencies {
    compileOnly(files("libs/PixselEconomy-1.0.0.jar"))
}
```

Other development environments can also use the JAR, provided it is available on the compile classpath and is not bundled into your plugin.

- **IntelliJ IDEA:** Go to File > Project Structure > Libraries > Add, select the JAR, and set its scope to Provided.
- **Eclipse:** Go to Project > Properties > Java Build Path > Libraries > Add External JARs.

**Important:** Do not shade or bundle the API into your plugin. PixselEconomy already provides these classes at runtime, and duplicating them can interfere with event delivery.

## Declaring the Dependency

Add PixselEconomy as a soft dependency in your `plugin.yml`:

```yaml
softdepend: [PixselEconomy]
```

Use `depend` instead if your plugin cannot function without PixselEconomy.

## Obtaining the Economy Service

The main API entry point is `EconomyService`:

```java
EconomyService economy = PixselEconomyAPI.getService();
```

`getService()` throws an `IllegalStateException` if PixselEconomy is missing or disabled.

For optional integrations, use `PixselEconomyAPI.isAvailable()` or `PixselEconomyAPI.getServiceOrNull()` to check whether the API is available before accessing it.

Resolve the service when it is needed, such as inside a command, event handler, or scheduled task. Avoid caching it during your own `onEnable()` method, as plugin load order is not guaranteed.

The API also provides static convenience methods for common economy operations:

```java
PixselEconomyAPI.addBalance(player, 250.0D, "coins");
PixselEconomyAPI.removeBalance(player.getUniqueId(), 25.0D, "coins");
PixselEconomyAPI.setBalance(player.getUniqueId(), 1000.0D, "coins");
PixselEconomyAPI.transfer(fromId, toId, 50.0D, "coins");
PixselEconomyAPI.getBalance(player.getUniqueId(), "coins");
```

## Threading

API methods use different threading behaviours depending on the operation.

| Method Group | Thread Behaviour |
|---|---|
| `getCachedBalance`, `isPlayerLoaded`, `getCurrency`, `getCurrencies`, `getDefaultCurrency`, `getDefaultCurrencyId`, `format` | Instant, non-blocking, and safe to call from any thread, including render loops |
| `getBalance`, `hasBalance`, `getLeaderboard` | Returns a `CompletableFuture` without blocking the caller |
| `deposit`, `withdraw`, `setBalance`, `transfer` | Returns a `CompletableFuture` without blocking the caller |

Futures complete on PixselEconomy's database thread. Any operation that accesses the Bukkit API must be scheduled back onto the server thread.

**Example:**

```java
economy.deposit(player.getUniqueId(), "coins", 100.0D, BalanceChangeCause.PLUGIN)
        .thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (result.isSuccessful()) {
                player.sendMessage("You earned 100 coins!");
            }
        }));
```

Never call `.join()` or `.get()` on a future from the server thread, as this can cause server thread blocking.

## Currencies

The `Currency` class is an immutable value object containing the configuration and formatting information for a currency.

Each currency provides:

- Identifier and display name.
- Currency symbol.
- Number format template.
- Decimal precision.
- Starting balance.
- Maximum balance.
- Payment availability.
- Withdrawal settings.

`Currency#format(double)` formats an amount using the same formatting rules as PixselEconomy.

`Currency#formatNumber(double, int)` formats a plain grouped number using the specified decimal precision.

Currency definitions are stored in `currencies.yml` and can be managed using `/pixseleconomy createcurrency` and `/pixseleconomy deletecurrency`.

The API intentionally does not provide methods for creating currencies. Currency definitions remain under the control of the server owner.

## Balance Operations

The API provides four methods for modifying player balances. Each method accepts a `BalanceChangeCause`, allowing event listeners to identify where a balance change originated.

| Method | Description |
|---|---|
| `deposit(playerId, currencyId, amount, cause)` | Adds the specified amount to an account. Fails with `LIMIT_EXCEEDED` if the currency's maximum balance would be exceeded. |
| `withdraw(playerId, currencyId, amount, cause)` | Removes the specified amount. Fails with `INSUFFICIENT_FUNDS` if the account does not have enough funds. |
| `setBalance(playerId, currencyId, amount, cause)` | Sets an account to an exact balance. The value must be zero or greater. |
| `transfer(fromId, toId, currencyId, amount, cause)` | Transfers funds between two accounts in a single database transaction. The operation is all or nothing. |

Amounts are rounded to the currency's configured decimal precision before being applied.

For example, an amount of `0.005` with two decimal places is rounded according to the currency's rounding rules.

Amounts that are not finite, or are not greater than zero where required, fail with `INVALID_AMOUNT` and do not modify any balances.

## Transaction Results

Every balance operation completes with a `TransactionResult`, which provides information about whether the operation succeeded and the resulting balances.

**Example:**

```java
TransactionResult result = /* completed future result */;

if (result.isSuccessful()) {
    double before = result.getPreviousBalance();
    double after = result.getNewBalance();
} else {
    switch (result.getStatus()) {
        case INSUFFICIENT_FUNDS -> { }
        case UNKNOWN_CURRENCY -> { }
        case CANCELLED -> { }         // A listener vetoed the change
        case STORAGE_ERROR -> { }     // Nothing was written
        default -> { }
    }
}
```

Available `TransactionStatus` values:

- `SUCCESS`
- `INVALID_AMOUNT`
- `UNKNOWN_CURRENCY`
- `INSUFFICIENT_FUNDS`
- `ACCOUNT_UNAVAILABLE`
- `LIMIT_EXCEEDED`
- `NOT_PAYABLE`
- `CANCELLED`
- `STORAGE_ERROR`

Failed results contain zero balances. Treat these results as operations where nothing changed, and use the transaction status to determine the reason for failure rather than formatting the returned balance values.

## Leaderboards

The API provides access to economy leaderboards through `getLeaderboard()`.

**Example:**

```java
economy.getLeaderboard("coins", 1, 10).thenAccept(entries -> {
    for (LeaderboardEntry entry : entries) {
        System.out.println(entry.position() + ". " + entry.playerName()
                + " - " + entry.balance());
    }
});
```

Leaderboard pages are one-based.

Each `LeaderboardEntry` contains:

- Account UUID.
- Last known player name.
- Account balance.
- Global leaderboard position.

For example, requesting page two with a page size of ten starts at position `11`.

## Events

PixselEconomy provides Bukkit events that allow other plugins to monitor economy changes.

Both events can be registered using standard Bukkit event listeners.

### `PixselBalanceChangeEvent`

Fired **before** a balance change is written to storage. This applies to changes made through commands, API operations, and banknotes.

Event details:

- **Cancellable:** Yes. Cancelling the event aborts the operation. The transaction completes with `TransactionStatus.CANCELLED`, and nothing is written.
- **Thread:** The thread performing the balance change. API-driven changes run on the database thread, so `isAsynchronous()` is usually `true`. Always check rather than assuming.
- **Performance:** Event handlers should execute quickly because the storage operation waits for them to finish.

Available accessors:

- `getPlayerId()`
- `getCurrencyId()`
- `getPreviousBalance()`
- `getNewBalance()`
- `getDifference()`
- `getAmount()`
- `getCause()`

**Example:**

```java
@EventHandler
public void onBalanceChange(PixselBalanceChangeEvent event) {
    // Inspect the balance change and its cause.
    // Use event.setCancelled(true) to prevent the change when appropriate.
}
```

### `PixselCurrencyChangeEvent`

Fired on the server thread whenever a currency is created or deleted.

This includes changes made through administrative commands or by editing `currencies.yml` and reloading the configuration.

Event details:

- **Cancellable:** No.
- **Thread:** Always runs on the server thread.
- **Types:** `CREATE` and `DELETE`.

Available accessors:

- `getType()`
- `getCurrency()`

When a currency is deleted, its `Currency` object remains fully readable through the event.

## API Versioning

The API follows semantic versioning independently of PixselEconomy's internal implementation.

- **Minor version:** Used for backwards-compatible additions.
- **Major version:** Used for changes that break existing integrations.

Use `EconomyService#getApiVersion()` and compare it against `PixselEconomyAPI.API_VERSION` if your plugin requires a specific API feature.

Check the API version before calling newer methods to ensure compatibility with the installed PixselEconomy version.

## Further Documentation

- [`API examples documentation`](API-EXAMPLES.md): Complete examples that can be adapted for your own plugins.
- [`Placeholders documentation`](PLACEHOLDERS.md): Placeholder reference for integrations that do not require Java development.

---

**Published by:** Pixsel Plugins  
**Author:** zAukz  
**API Version:** `1.0.0`

## 💙 **Thank you for integrating with PixselEconomy!** 💙
