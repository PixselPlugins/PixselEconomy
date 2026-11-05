# PixselEconomy

A lightweight multi-currency economy plugin for Paper servers, with unlimited separate economies, a
safe backup system, PlaceholderAPI support and a full developer API.

**Published by:** Pixsel Plugins  
**Authored by:** zAukz  
**Website:** https://pixselplugins.com

## Documentation

| Document | Contents |
|---|---|
| [docs/API.md](docs/API.md) | Full API reference: dependency setup, threading, results, events and versioning |
| [docs/API-EXAMPLES.md](docs/API-EXAMPLES.md) | Copy-ready examples: rewards, shops, taxes and event hooks |
| [docs/PLACEHOLDERS.md](docs/PLACEHOLDERS.md) | Every `%pixseleconomy_...%` placeholder, with formats and limits |

## Features

- **Multiple economies.** Create as many currencies as you want, each with its own display name,
  symbol, format, decimals, starting balance, maximum balance, payment rules and withdrawal settings.
  There is no fixed "money" currency; `coins`, `gems`, `tokens` and anything else you invent are all
  first-class.
- **Commands and configuration are equally capable.** Everything a command does can also be done by
  editing `currencies.yml` and running `/pixseleconomy reload`.
- **Per-feature permissions.** Every command has its own permission, plus `pixsel.economy.admin` and
  `pixsel.economy.*` shortcuts that grant all of them at once.
- **Guarded destructive command.** `/resetall` must be run several times in a row, inside a short
  window, before it wipes balances. Both values are configurable.
- **Withdrawable banknotes.** `/withdraw` turns a balance into a paper item that can be traded and
  redeemed. Optional flat or percentage fee, per-currency limits, and crafting protection.
- **Paginated leaderboards.** `/leaderboard`, `/topbal` and `/baltop` with clickable arrows that
  print the neighbouring page.
- **Placeholders everywhere.** `%pixseleconomy_...%` placeholders work in chat, scoreboards, boss
  bars, action bars and holograms through PlaceholderAPI, and can be disabled in the config.
- **Safe, optional backups.** A timer writes copies of the economy tables to a separate file, while
  the live economy keeps using the main database. Deleting a backup can never break anything.
- **Developer API.** Another plugin can read balances, move money and listen for changes through
  `PixselEconomyAPI`, with cancellable events and explicit async results.
- **Lightweight.** No per-tick tasks. Balances are cached in memory, all database work runs on one
  dedicated thread, and placeholders never query storage.

## Compatibility

| Requirement | Details |
|---|---|
| Server | Paper (or a Paper fork) |
| Minecraft | 26.2 and 26.3 |
| Java | 25 (the runtime Paper 26.2/26.3 use) |
| Optional | PlaceholderAPI 2.12.x |

The plugin is compiled against the Paper **26.2** API, which is the lower of the two mandatory
targets, so the same JAR loads on 26.2 and 26.3 without version-specific code paths. `api-version` is
declared as `26.2`.

## Installation

1. Download `PixselEconomy-1.0.0.jar`.
2. Place it in your server's `plugins` directory.
3. Start the server. `config.yml`, `currencies.yml` and `messages.yml` are created automatically, and
   `database.db` is created on first start.
4. Adjust the currency list and settings, then run `/pixseleconomy reload`.
5. Give players access with your permissions plugin. Defaults: `/pay`, `/balance`, `/withdraw` and
   `/leaderboard` are available to everyone; administration is operator-only.

Do **not** install anything else for storage. SQLite needs no setup, and the MySQL driver is bundled
inside the plugin JAR.

## Commands

Player commands are their own commands. Every administration command is a `/pixseleconomy`
subcommand instead, and `/eco` is an alias of the main command.

| Command | Description | Permission |
|---|---|---|
| `/pay <player> <amount> [currency]` | Send money to another player | `pixsel.economy.pay` |
| `/balance [player] [currency]` (`/bal`) | Show your balance, or another player's | `pixsel.economy.balance` |
| `/withdraw <amount> [currency]` | Turn a balance into a banknote | `pixsel.economy.withdraw` |
| `/leaderboard [currency] [page]` (`/topbal`, `/baltop`) | Richest accounts, clickable pages | `pixsel.economy.leaderboard` |

### Administration

Every administration command is a `/pixseleconomy` subcommand and occupies no top-level command
name, so the player commands above stay short.

| Command | Description | Permission |
|---|---|---|
| `/pixseleconomy` or `/eco` | Show the commands you may use | `pixsel.economy.use` |
| `/pixseleconomy help` | Show the commands you may use | `pixsel.economy.use` |
| `/pixseleconomy reload` | Reload all configuration files | `pixsel.economy.reload` |
| `/pixseleconomy addcurrency <player> <amount> [currency]` | Add money to an account | `pixsel.economy.add` |
| `/pixseleconomy takecurrency <player> <amount> [currency]` | Remove money from an account | `pixsel.economy.take` |
| `/pixseleconomy setcurrency <player> <amount> [currency]` | Set an exact balance | `pixsel.economy.set` |
| `/pixseleconomy createcurrency <id> [display name]` | Create a currency | `pixsel.economy.create` |
| `/pixseleconomy deletecurrency <id>` | Delete a currency and all balances in it | `pixsel.economy.delete` |
| `/pixseleconomy resetall [currency\|cancel]` | Reset balances after repeated confirmation | `pixsel.economy.resetall` |

`/balance` takes the player argument optionally: `/bal` shows your own balance, `/bal Notch` another
player's (needs `pixsel.economy.balance.others`), and `/bal Notch gems` a specific currency. A first
argument that is not a known player is read as a currency, so `/bal gems` works as well. Amounts
accept grouping commas (`1,500`).

## Permissions

| Permission | Default | Description |
|---|---|---|
| `pixsel.economy.*` | Operators | Every permission below |
| `pixsel.economy.admin` | Operators | Every permission below |
| `pixsel.economy.use` | Everyone | View the command list |
| `pixsel.economy.pay` | Everyone | Send money |
| `pixsel.economy.balance` | Everyone | Check your own balance |
| `pixsel.economy.balance.others` | Operators | Check another player's balance |
| `pixsel.economy.withdraw` | Everyone | Create banknotes |
| `pixsel.economy.leaderboard` | Everyone | View leaderboards |
| `pixsel.economy.add` | Operators | Add money |
| `pixsel.economy.take` | Operators | Remove money |
| `pixsel.economy.set` | Operators | Set a balance |
| `pixsel.economy.create` | Operators | Create currencies |
| `pixsel.economy.delete` | Operators | Delete currencies |
| `pixsel.economy.resetall` | Operators | Reset balances |
| `pixsel.economy.reload` | Operators | Reload configuration |

`pixsel.economy.admin` and `pixsel.economy.*` are ordinary Bukkit parent permissions, so granting one
node is enough, so no per-command setup is required. Features also check their permission: disabling a
feature in the config hides nothing from the permission system, and granting a permission does not
override a disabled feature.

## Configuration

| File | Purpose |
|---|---|
| `config.yml` | Behaviour: default currency, `/pay` rules, withdraw settings, leaderboard paging, placeholders, reset safety, storage and backups |
| `currencies.yml` | One section per currency: name, symbol, format, decimals, starting and maximum balance, payment and withdrawal rules |
| `messages.yml` | Every player-facing string in MiniMessage format, including banknote name and lore |
| `database.db` | SQLite database, created automatically when `storage.type: sqlite` |

All three YAML files are read once on startup and can be reloaded with `/pixseleconomy reload`.
Missing or invalid values fall back to safe defaults and are reported in the console, so a broken
value never prevents the plugin from starting. Reloads never rewrite your files.

Currency creation writes a complete section into `currencies.yml`, so anything you create in game is
visible and editable in the file, so the two ways of managing currencies stay in sync.

## Storage

### SQLite (default)

```yaml
storage:
  type: sqlite
  sqlite:
    file: database.db
```

The file lives in `plugins/PixselEconomy/`. Write-ahead logging is enabled, and every database call
runs on the plugin's single database thread, so the server thread never blocks on disk.

### MySQL / MariaDB

```yaml
storage:
  type: mysql
  mysql:
    host: localhost
    port: 3306
    database: pixseleconomy
    username: username
    password: password
    pool-size: 4
    properties: "useSSL=false&allowPublicKeyRetrieval=true"
```

The MySQL driver is shaded into the plugin JAR, so no extra library is needed on the server. The
plugin keeps a small validated connection pool and creates its own table. Switching storage type
requires a restart; the plugin warns if you reload after changing it.

### Backups

```yaml
storage:
  backup:
    enabled: true
    interval-minutes: 60
    directory: backups
    keep: 5
```

Backups are **write-only copies**: the live economy always reads and writes the main database.
Deleting a backup file, or the whole `backups` folder, cannot break anything, because the next run creates a
new file.

- SQLite backups are complete `.db` files (`backup-20261104-234500.db`) produced inside the database
  engine, so they are consistent and can be opened directly or swapped in during maintenance.
- MySQL backups are portable `.sql` dumps containing the table definition and every row.
- `keep` prunes the oldest files automatically; `0` keeps everything.

## Placeholders

PixselEconomy registers `%pixseleconomy_...%` placeholders with PlaceholderAPI, so they work in any
plugin that parses placeholders: chat formats, scoreboards, boss bars, action bars, tab lists and
holograms. Disable them with `features.placeholders.enabled: false`.

```
%pixseleconomy_balance%                  -> 1,234.50        plain number, default currency
%pixseleconomy_balance_formatted%        -> $1,234.50       with symbol and grouping
%pixseleconomy_balance_short%            -> 1.23K           compact form
%pixseleconomy_balance_formatted_gems%   -> 42 Gems         any currency
%pixseleconomy_currency_name%            -> Coins
%pixseleconomy_top_1_name%               -> zAukz           leaderboard snapshot
%pixseleconomy_top_coins_2_balance%      -> $12,000.00
```

Balance placeholders read the in-memory cache, so they cost nothing per render. The full list,
including affordability checks and leaderboard positions, is in
[docs/PLACEHOLDERS.md](docs/PLACEHOLDERS.md).

## API & Developer Integration

Another plugin can use PixselEconomy without touching its database or internals:

```java
PixselEconomyAPI.addBalance(player, 250.0D, "coins")
        .thenAccept(result -> {
            if (result.isSuccessful()) {
                plugin.getLogger().info("New balance: " + result.getNewBalance());
            } else {
                plugin.getLogger().warning("Failed: " + result.getStatus());
            }
        });

double coins = PixselEconomyAPI.getService()
        .getCachedBalance(player.getUniqueId(), "coins")
        .orElse(0.0D);
```

- The API classes are bundled inside `PixselEconomy-1.0.0.jar`. Add that JAR from Releases to your
  project as a compile-only library and never shade it. See
  [docs/API.md](docs/API.md#adding-the-dependency) for the setup steps.
- The plugin itself must be installed on the server. The API has no separate download.
- Declare `softdepend: [PixselEconomy]` and check `PixselEconomyAPI.isAvailable()`.
- Mutating calls return a `CompletableFuture<TransactionResult>`; reads that must not block
  (`getCachedBalance`) are instant. Futures complete off the server thread.
- `PixselBalanceChangeEvent` is cancellable and fires before every write;
  `PixselCurrencyChangeEvent` reports currencies being created or deleted.

Full documentation: [docs/API.md](docs/API.md). Ready-to-adapt examples:
[docs/API-EXAMPLES.md](docs/API-EXAMPLES.md).

## Dependencies

| Dependency | Required | Notes |
|---|---|---|
| Paper API 26.2+ | Yes | Provided by the server |
| PlaceholderAPI 2.12.x | No | Only needed for `%pixseleconomy_...%` placeholders; the plugin loads fine without it |
| SQLite JDBC | No | Bundled with Paper itself |
| MySQL Connector/J 9.5.0 | No | Bundled in the plugin JAR, used only when `storage.type: mysql` |

No other Pixsel plugin is required, and no Pixsel plugin depends on this one.

## Source and Releases

This repository publishes the plugin source and documentation for reading, review and reference. The
finished, ready-to-install JAR is distributed through [Releases](../../releases) and is
self-contained: the API classes and the MySQL driver are bundled inside it and signature files are
stripped, so nothing else is needed on the server. Every release is published by the author.

### Development verification

The `1.0.0` source targets the Paper 26.2 API on Java 25, and the storage layer was exercised
against a real SQLite database (schema creation, upserts, transactional transfers, leaderboard
paging, scoped and global resets, backup creation, backup deletion not affecting the live data,
currency deletion and repeated initialisation). A live server smoke test on Paper 26.2/26.3 is still
recommended after you deploy.

## Contributing

Issues, bug reports and suggestions are welcome. Please keep the conventions used here: the Pixsel prefix and
colour palette, `pixsel.economy.*` permission names, MiniMessage messages, prepared statements, and
no blocking work on the server thread. Public API methods are treated as supported interfaces, so
changes that break them require a major version bump.

## License

MIT. See [LICENSE](LICENSE).
