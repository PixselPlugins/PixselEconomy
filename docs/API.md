# PixselEconomy API

The public API lets another Paper plugin read balances, move money, inspect currencies and listen for
changes without touching PixselEconomy's database, configuration or internal classes.

- API version: `1.0.0` (`PixselEconomyAPI.API_VERSION`)
- Plugin version: `1.0.0`
- Java package: `com.pixselplugins.zaukz.pixseleconomy.api`
- Runtime: the API classes are bundled **inside** `PixselEconomy-1.0.0.jar`, which must be installed
  on the server. There is no separate runtime library to install.

## Adding the dependency

The API classes are bundled inside `PixselEconomy-1.0.0.jar`, so that single JAR is all you need to
compile against them. Download it from the plugin's Releases page and add it to your project as a
compile-only library, because the plugin supplies the classes at runtime.

Gradle:

```groovy
dependencies {
    compileOnly(files("libs/PixselEconomy-1.0.0.jar"))
}
```

Other setups work too, as long as the JAR is on the compile classpath and is not bundled into your
output:

- IntelliJ IDEA: File > Project Structure > Libraries > Add, pick the JAR, and set its scope to
  Provided.
- Eclipse: Project > Properties > Java Build Path > Libraries > Add External JARs.

Do **not** shade the API into your JAR and do not bundle it as a library: the plugin already provides
those classes, and duplicating them breaks event delivery.

## Declaring the dependency in plugin.yml

```yaml
softdepend: [PixselEconomy]
```

Use `depend` only if your plugin genuinely cannot function without PixselEconomy.

## Obtaining the service

```java
EconomyService economy = PixselEconomyAPI.getService();
```

`getService()` throws `IllegalStateException` when PixselEconomy is missing or disabled. Resolve it
lazily, inside a command, event handler or scheduled task, rather than caching it in your own
`onEnable`, because plugin load order is not guaranteed. Use `PixselEconomyAPI.isAvailable()` or
`PixselEconomyAPI.getServiceOrNull()` when the integration is optional.

Static convenience methods mirror the service for the most common operations:

```java
PixselEconomyAPI.addBalance(player, 250.0D, "coins");
PixselEconomyAPI.removeBalance(player.getUniqueId(), 25.0D, "coins");
PixselEconomyAPI.setBalance(player.getUniqueId(), 1000.0D, "coins");
PixselEconomyAPI.transfer(fromId, toId, 50.0D, "coins");
PixselEconomyAPI.getBalance(player.getUniqueId(), "coins");
```

## Threading

| Method group | Thread behaviour |
|---|---|
| `getCachedBalance`, `isPlayerLoaded`, `getCurrency`, `getCurrencies`, `getDefaultCurrency`, `getDefaultCurrencyId`, `format` | Instant, non-blocking, safe on any thread and in render loops |
| `getBalance`, `hasBalance`, `getLeaderboard` | Returns a `CompletableFuture`, never blocks the caller |
| `deposit`, `withdraw`, `setBalance`, `transfer` | Returns a `CompletableFuture`, never blocks the caller |

Futures complete on PixselEconomy's database thread. Anything that touches the Bukkit API must be
re-scheduled onto the server thread:

```java
economy.deposit(player.getUniqueId(), "coins", 100.0D, BalanceChangeCause.PLUGIN)
        .thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (result.isSuccessful()) {
                player.sendMessage("You earned 100 coins!");
            }
        }));
```

Never call `.join()` or `.get()` on a future from the server thread.

## Currencies

`Currency` is an immutable value object: identifier, display name, symbol, format template, decimals,
starting balance, maximum balance, whether it is payable, and its withdrawal settings.
`Currency#format(double)` renders an amount exactly like the plugin does, and
`Currency#formatNumber(double, int)` renders a plain grouped number.

Currency definitions live in `currencies.yml` and are managed with `/pixseleconomy createcurrency`
and `/pixseleconomy deletecurrency`. The API intentionally cannot create currencies; that keeps
economy structure a server-owner decision.

## Balance operations

All four mutating methods take a `BalanceChangeCause` so listeners can tell an API deposit from a
command or a banknote.

| Method | Behaviour |
|---|---|
| `deposit(playerId, currencyId, amount, cause)` | Adds `amount`; fails with `LIMIT_EXCEEDED` above the currency maximum |
| `withdraw(playerId, currencyId, amount, cause)` | Removes `amount`; fails with `INSUFFICIENT_FUNDS` instead of going negative |
| `setBalance(playerId, currencyId, amount, cause)` | Sets an exact value; zero or greater |
| `transfer(fromId, toId, currencyId, amount, cause)` | Moves money in one database transaction; all or nothing |

Amounts are rounded to the currency's decimals before they are applied, so `0.005` with two decimals
becomes `0.01` or `0.00` as the currency dictates. Amounts that are not finite, or that are not
greater than zero where required, fail with `INVALID_AMOUNT` and change nothing.

## Results

Every operation completes with a `TransactionResult`:

```java
TransactionResult result = /* future */;
if (result.isSuccessful()) {
    double before = result.getPreviousBalance();
    double after = result.getNewBalance();
} else {
    switch (result.getStatus()) {
        case INSUFFICIENT_FUNDS -> { }
        case UNKNOWN_CURRENCY -> { }
        case CANCELLED -> { }         // a listener vetoed the change
        case STORAGE_ERROR -> { }     // nothing was written
        default -> { }
    }
}
```

`TransactionStatus` values: `SUCCESS`, `INVALID_AMOUNT`, `UNKNOWN_CURRENCY`, `INSUFFICIENT_FUNDS`,
`ACCOUNT_UNAVAILABLE`, `LIMIT_EXCEEDED`, `NOT_PAYABLE`, `CANCELLED`, `STORAGE_ERROR`.

A failed result carries zero balances; treat it as "nothing changed" and report the status rather
than formatting the numbers.

## Leaderboards

```java
economy.getLeaderboard("coins", 1, 10).thenAccept(entries -> {
    for (LeaderboardEntry entry : entries) {
        System.out.println(entry.position() + ". " + entry.playerName()
                + " - " + entry.balance());
    }
});
```

Pages are one-based. `LeaderboardEntry` carries the account UUID, the last known player name, the
balance and the global position, so page two starts at position `11` for a page size of `10`.

## Events

Both events are normal Bukkit events; register them like any other listener.

### `PixselBalanceChangeEvent`

Fired **before** a change is written, for commands, API calls and banknotes alike.

- Cancellable: yes, cancelling aborts the operation, which then completes with
  `TransactionStatus.CANCELLED` and writes nothing.
- Thread: the thread performing the change. API-driven changes run on the database thread, so
  `isAsynchronous()` is usually `true`; check it instead of assuming. Handlers must be quick,
  because storage waits for them.
- Accessors: `getPlayerId()`, `getCurrencyId()`, `getPreviousBalance()`, `getNewBalance()`,
  `getDifference()`, `getAmount()`, `getCause()`.

```java
@EventHandler
public void onBalanceChange(PixselBalanceChangeEvent event) {
    if (event.getCause() == BalanceChangeCause.DISPLAY, skip...) { }
}
```

### `PixselCurrencyChangeEvent`

Fired on the server thread when a currency is created or deleted, from commands or from editing
`currencies.yml` followed by a reload. Not cancellable. Provides `getType()` (`CREATE` or `DELETE`)
and `getCurrency()`; on deletion the currency object is still fully readable.

## Versioning

The API follows semantic versioning independently of plugin internals: additions bump the minor
version, and any change that would break existing integrations bumps the major version. Compare
`EconomyService#getApiVersion()` against `PixselEconomyAPI.API_VERSION` if your plugin needs a
specific feature, and guard newer calls behind that check.

## Further reading

- [API-EXAMPLES.md](API-EXAMPLES.md): complete, adaptable examples
- [PLACEHOLDERS.md](PLACEHOLDERS.md): placeholder reference for non-developer integration
