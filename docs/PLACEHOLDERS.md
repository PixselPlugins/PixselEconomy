# PixselEconomy Placeholders

PixselEconomy includes an optional PlaceholderAPI integration that allows economy information to be displayed throughout your server.

Placeholders can be used anywhere PlaceholderAPI is supported, including chat formats, scoreboards, boss bars, action bars, tab lists, holograms, and signs.

- **Identifier:** `pixseleconomy`
- **Format:** `%pixseleconomy_<placeholder>%`
- **Requirement:** PlaceholderAPI must be installed, and `features.placeholders.enabled: true` must be enabled.
- **Optional Integration:** PlaceholderAPI is not required for PixselEconomy to function. Without it, the economy continues working normally, but its placeholders will not be available.

The `<currency>` parameter represents a currency identifier defined in `currencies.yml`, such as `coins`.

When a placeholder supports both a default and currency-specific version, the version without a currency identifier uses the currency configured under `general.default-currency`.

## Player Balances

Balance placeholders retrieve data from the in-memory cache of online players.

This makes them safe to use in frequently updated displays, such as scoreboards and boss bars, without generating additional database queries.

| Placeholder | Example Output | Description |
|---|---|---|
| `%pixseleconomy_balance%` | `1,234.50` | Balance in the default currency, displayed as a plain number |
| `%pixseleconomy_balance_<currency>%` | `42` | Balance in another currency, displayed as a plain number |
| `%pixseleconomy_balance_formatted%` | `$1,234.50` | Balance formatted using the currency's symbol and format |
| `%pixseleconomy_balance_formatted_<currency>%` | `42 Gems` | Formatted balance for another currency |
| `%pixseleconomy_balance_short%` | `1.23K` | Compact balance using K/M/B/T suffixes |
| `%pixseleconomy_balance_short_<currency>%` | `1.5M` | Compact balance for another currency |
| `%pixseleconomy_can_afford_<amount>%` | `true` | Checks whether the player can afford an amount in the default currency |
| `%pixseleconomy_can_afford_<currency>_<amount>%` | `false` | Checks whether the player can afford an amount in a specific currency |

Plain number placeholders use the currency's configured decimal precision and comma-based number grouping.

Formatted placeholders include the currency's configured display formatting and are intended for player-facing displays.

## Currency Information

Currency placeholders provide information about the server's configured currencies, including identifiers, display names, and symbols.

| Placeholder | Example Output | Description |
|---|---|---|
| `%pixseleconomy_currency%` | `coins` | Identifier of the default currency |
| `%pixseleconomy_currency_count%` | `3` | Total number of configured currencies |
| `%pixseleconomy_currencies%` | `coins, gems, tokens` | List of all configured currency identifiers |
| `%pixseleconomy_currency_name%` | `Coins` | Display name of the default currency |
| `%pixseleconomy_currency_name_<currency>%` | `Gems` | Display name of a specific currency |
| `%pixseleconomy_currency_symbol%` | `$` | Symbol of the default currency |
| `%pixseleconomy_currency_symbol_<currency>%` | *(Empty)* | Symbol of a specific currency |

## Economy Leaderboards

Leaderboard placeholders allow you to display the richest players on your server without repeatedly querying the database.

Leaderboard data is stored in a cached snapshot and refreshed according to `features.placeholders.leaderboard-refresh-seconds`.

The default refresh interval is 60 seconds.

Positions outside the cached leaderboard snapshot return an empty string. The number of positions tracked is controlled by `features.placeholders.leaderboard-depth`, which defaults to 10.

| Placeholder | Example Output | Description |
|---|---|---|
| `%pixseleconomy_top_<position>_name%` | `zAukz` | Player name at the specified position in the default currency leaderboard |
| `%pixseleconomy_top_<position>_balance%` | `$12,000.00` | Formatted balance at the specified position |
| `%pixseleconomy_top_<position>_value%` | `12,000.00` | Plain balance at the specified position |
| `%pixseleconomy_top_<currency>_<position>_name%` | `zAukz` | Player name at the specified position in a currency leaderboard |
| `%pixseleconomy_top_<currency>_<position>_balance%` | `500 Gems` | Formatted balance for a specific currency |
| `%pixseleconomy_top_<currency>_<position>_value%` | `500` | Plain balance for a specific currency |

**Examples:**

```text
%pixseleconomy_top_1_name%        -> zAukz
%pixseleconomy_top_coins_2_name%  -> Notch
%pixseleconomy_top_gems_1_value%  -> 500
```

## Additional Notes

- **Currency Identifiers:** Currency identifiers can contain underscores (`_`). The placeholder parser splits leaderboard and affordability placeholders from the right, allowing identifiers such as `hard_currency` to work correctly.
- **Unloaded Player Accounts:** Balance placeholders return `0` for players whose accounts have not been loaded by the plugin, such as players who have just left the server.
- **Disabling Placeholders:** Setting `features.placeholders.enabled` to `false` completely unregisters the PlaceholderAPI expansion and stops its leaderboard refresh task. No placeholder processing or refresh work is performed while disabled.

---

**Published by:** Pixsel Plugins  
**Author:** zAukz  
**Plugin:** PixselEconomy  
**PlaceholderAPI Identifier:** `pixseleconomy`

## 💙 **Thank you for using PixselEconomy!** 💙
