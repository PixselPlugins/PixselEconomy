# PixselEconomy v1.0.0

A lightweight and extensible economy plugin for Paper, featuring customizable currencies, flexible account and transaction systems, and a developer-friendly API for third-party integrations.

Everything in this plugin is managed through straightforward configuration files and in-game commands, with sensible defaults that let you get started without extensive setup.

## Features
- **Multi-Currency Economy:** Create and manage multiple currencies for your server. PixselEconomy supports any custom economy that your server requires.
- **Flexible Currency Settings:** Configure display names, symbols, starting balances, decimal precision, balance limits, payment rules, and more, all independently for each currency.
- **Player Transactions:** Give players built-in commands to check balances, send money, withdraw banknotes, and compete on economy leaderboards.
- **Physical Banknotes:** Allow players to withdraw currency as redeemable items. Configure banknote values, withdrawal limits, fees, names, and item descriptions.
- **Simple Administration:** Manage player balances, create and delete currencies, and control your economy either in-game or through config files.
- **Customizable Messages:** Adjust messages, formatting, banknote names, and item lore (support for Paper's built-in [MiniMessage](https://docs.papermc.io/adventure/minimessage/)).
- **PlaceholderAPI Integration:** Display balances, currency information, and leaderboard data in scoreboards, chat, holograms, and more with [PlaceholderAPI](https://wiki.placeholderapi.com/).
- **Flexible Storage:** Uses SQLite by default, with optional MySQL and MariaDB support for servers that need remote database storage.
- **Automatic Backups:** Protect economy data with scheduled backups, configurable intervals, and retention settings.
- **Developer API:** Includes a public Java API for developers who want to connect their own plugins to PixselEconomy.

## Requirements
| Component | Version | Required |
|---|---|---|
| Paper | 26.2 or 26.3 | Yes |
| Java | 25 | Yes |
| PlaceholderAPI | 2.12.x | Optional |
| MySQL / MariaDB | Any supported version | Optional |


## Installation
1. Download `PixselEconomy-1.0.0.jar`.
2. Place the JAR file in your server's `plugins` directory.
3. Start your server.
4. Done!

If you wish to fine-tune your plugin, feel free to edit your config files after the server has ran with the plugin.

## Getting Started
PixselEconomy includes a ready-to-use default configuration with three currencies: `coins`, `gems`, and `tokens`.
New player accounts start with 100 coins. The default currency uses `$` as its symbol and supports two decimal places.

Players can immediately use the following commands:
| Command | Description |
|---|---|
| `/balance` | Check your balance |
| `/pay <player> <amount>` | Send money to another player |
| `/withdraw <amount>` | Withdraw a banknote |
| `/leaderboard` | View the richest players |

Additional currencies can be created in-game using the administrative commands or configured directly in `currencies.yml`.

## Configuration
PixselEconomy keeps configuration organized across three primary files:
| File | Purpose |
|---|---|
| `config.yml` | General settings, storage, features, and backups |
| `currencies.yml` | Currency definitions and individual currency settings |
| `messages.yml` | Player-facing messages and banknote formatting |

Each currency can have its own settings, including:
- Display names and symbols.
- Decimal precision and number formatting.
- Starting and maximum balances.
- Payment restrictions.
- Banknote values and withdrawal fees.

Configuration changes can be applied using `/pixseleconomy reload` or restarting your server.

## Player Commands
Player commands are allowed to all players by default. To prevent them, you will need to manually remove them in your permissions management plugin.

| Command | Description | Permission |
|---|---|---|
| `/pay <player> <amount> [currency]` | Transfer funds to another player | `pixsel.economy.pay` |
| `/balance [player]` | Show your balance, or another player's | `pixsel.economy.balance` |
| `/withdraw <amount> [currency]` | Withdraw funds as a banknote | `pixsel.economy.withdraw` |
| `/leaderboard [currency] [page]` | View the richest accounts | `pixsel.economy.leaderboard` |

## Administration Commands
Admin commands are all handled through one parent command for ease of use.

| Command | Description | Permission | Default |
|---|---|---|---|
| `/pixseleconomy` | Display available commands | `pixsel.economy.use` | Everyone |
| `/pixseleconomy reload` | Reload configuration files | `pixsel.economy.reload` | Operators |
| `/pixseleconomy addcurrency <player> <amount> [currency]` | Add funds to a player's account | `pixsel.economy.add` | Operators |
| `/pixseleconomy takecurrency <player> <amount> [currency]` | Remove funds from a player's account | `pixsel.economy.take` | Operators |
| `/pixseleconomy setcurrency <player> <amount> [currency]` | Set a player's balance | `pixsel.economy.set` | Operators |
| `/pixseleconomy createcurrency <id> [display name]` | Create a new currency | `pixsel.economy.create` | Operators |
| `/pixseleconomy deletecurrency <id>` | Delete an existing currency | `pixsel.economy.delete` | Operators |
| `/pixseleconomy resetall [currency]` | Reset stored balances | `pixsel.economy.resetall` | Operators |
| `/pixseleconomy resetall cancel` | Cancel a pending reset | `pixsel.economy.resetall` | Operators |

The permissions `pixsel.economy.admin` and `pixsel.economy.*` grant access to all economy permissions.

## Banknotes
Players can withdraw physical banknotes representing a specified amount of a particular currency.
Banknotes can be redeemed by right-clicking them, and their currency and value are stored in item data.

Available settings include:
- Configurable minimum and maximum withdrawal amounts.
- Per-currency banknote settings.
- Optional flat or percentage-based withdrawal fees.
- Custom item names and lore.
- Crafting restrictions, enabled by default.

Built-in safeguards:
- If a player's inventory is full, the withdrawn banknote is dropped at their location. 
- Banknotes belonging to a deleted currency can no longer be redeemed.

## Storage & Backups
PixselEconomy supports SQLite, MySQL, and MariaDB.

**SQLite** is enabled by default and requires no additional setup (Data is stored locally).
**MySQL / MariaDB** can be configured for servers that prefer remote database storage or shared account data.

Automatic backups are included, with configurable intervals, retention limits, and backup directories.
**Changing the storage backend requires a server restart.**
- See `config.yml` for the available database and backup settings.

## PlaceholderAPI
PixselEconomy includes an optional PlaceholderAPI expansion, allowing economy information to be displayed throughout your server.

Supported placeholders include:
- Player balances and formatted currency amounts.
- Currency names and symbols.
- Affordability checks.
- Economy leaderboards.

Balance placeholders read directly from memory to limit database queries, and leaderboard data is cached and refreshed every 60 seconds.

See [`the placeholders documentation`](docs/PLACEHOLDERS.md) for the complete placeholder reference.

## Developer API
PixselEconomy also provides a public Java API for developers building integrations with other Paper plugins.

The API provides access to currency information, player balances, transactions, and relevant economy events.

It is included in the plugin JAR and can be added to another project's classpath as a compile-only dependency. No separate API download or shaded library is required.

See the developer documentation:
- [`API documentation`](docs/API.md)
- [`API examples documentation`](docs/API-EXAMPLES.md)

## Troubleshooting
| Issue | Possible cause |
|---|---|
| Plugin does not load | Verify your server has a compatible Paper version and Java runtime. |
| Placeholders are not resolving | Check that PlaceholderAPI is installed and enabled. |
| Balances differ after a restart | Verify the configured storage backend. |
| An account cannot be loaded | Check database connectivity and console output. |
| A banknote can no longer be redeemed | Its associated currency may have been deleted. |
| Configuration changes are not applied | Run `/pixseleconomy reload` and check the console for errors. |

## Upgrading Plugin Version
Configuration files are preserved during updates. Any required database migrations will be handled by the plugin and documented in future releases.

## Support
For bug reports, feature requests, and technical support, visit our [Issues page](../../issues).

When reporting an issue, include your Paper version, Java version, relevant console output, and steps to reproduce the problem.

---

**Published by:** Pixsel Plugins  
**Author:** zAukz  
**License:** MIT

## 💙 **Thank you for using PixselEconomy!** 💙
