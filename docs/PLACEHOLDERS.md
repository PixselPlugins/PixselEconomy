# PixselEconomy Placeholders

PixselEconomy registers its placeholders with [PlaceholderAPI](https://wiki.placeholderapi.com/), so
they work anywhere PlaceholderAPI resolves them: chat formats, scoreboards, boss bars, action bars,
tab lists, holograms and signs.

- Identifier: `pixseleconomy`
- Format: `%pixseleconomy_<placeholder>%`
- Requirement: PlaceholderAPI installed, and `features.placeholders.enabled: true` (default)
- PlaceholderAPI is **optional**; the plugin works without it, only the placeholders disappear.

`<currency>` is a currency identifier from `currencies.yml`, for example `coins`. When a placeholder
has both forms, the form without a currency uses `general.default-currency`.

## Balances

Balance placeholders read the in-memory cache of online players, so they are safe in per-tick
contexts such as scoreboards and boss bars and cause no database queries.

| Placeholder | Example output | Meaning |
|---|---|---|
| `%pixseleconomy_balance%` | `1,234.50` | Balance in the default currency, plain number |
| `%pixseleconomy_balance_<currency>%` | `42` | Balance in another currency, plain number |
| `%pixseleconomy_balance_formatted%` | `$1,234.50` | Balance using the currency's symbol and format |
| `%pixseleconomy_balance_formatted_<currency>%` | `42 Gems` | Same, for another currency |
| `%pixseleconomy_balance_short%` | `1.23K` | Compact balance with K/M/B/T suffixes |
| `%pixseleconomy_balance_short_<currency>%` | `1.5M` | Compact balance for another currency |
| `%pixseleconomy_can_afford_<amount>%` | `true` | Whether the player holds that much of the default currency |
| `%pixseleconomy_can_afford_<currency>_<amount>%` | `false` | Whether the player holds that much of a currency |

Plain numbers use the currency's configured decimals and `,` grouping, which makes them safe for
comparisons and for tabular displays; formatted variants are meant for humans.

## Currency metadata

| Placeholder | Example output | Meaning |
|---|---|---|
| `%pixseleconomy_currency%` | `coins` | Identifier of the default currency |
| `%pixseleconomy_currency_count%` | `3` | Number of configured currencies |
| `%pixseleconomy_currencies%` | `coins, gems, tokens` | Every currency identifier |
| `%pixseleconomy_currency_name%` | `Coins` | Display name of the default currency |
| `%pixseleconomy_currency_name_<currency>%` | `Gems` | Display name of a currency |
| `%pixseleconomy_currency_symbol%` | `$` | Symbol of the default currency |
| `%pixseleconomy_currency_symbol_<currency>%` | `` (empty) | Symbol of a currency |

## Leaderboard

Leaderboard placeholders read a snapshot that is refreshed every
`features.placeholders.leaderboard-refresh-seconds` seconds (default 60). They never query the
database while a placeholder is being resolved, which keeps holograms cheap. Positions outside the
snapshot resolve to an empty string; `features.placeholders.leaderboard-depth` sets how many
positions are tracked (default 10).

| Placeholder | Example output | Meaning |
|---|---|---|
| `%pixseleconomy_top_<position>_name%` | `zAukz` | Name at that position for the default currency |
| `%pixseleconomy_top_<position>_balance%` | `$12,000.00` | Formatted balance at that position |
| `%pixseleconomy_top_<position>_value%` | `12,000.00` | Plain balance at that position |
| `%pixseleconomy_top_<currency>_<position>_name%` | `zAukz` | Name at that position for a currency |
| `%pixseleconomy_top_<currency>_<position>_balance%` | `500 Gems` | Formatted balance at that position |
| `%pixseleconomy_top_<currency>_<position>_value%` | `500` | Plain balance at that position |

Examples:

```text
%pixseleconomy_top_1_name%        -> zAukz
%pixseleconomy_top_coins_2_name%  -> Notch
%pixseleconomy_top_gems_1_value%  -> 500
```

## Notes

- Currency identifiers may contain `_`; the parser splits leaderboard and affordability placeholders
  from the right, so `%pixseleconomy_top_hard_currency_3_name%` resolves `hard_currency` correctly.
- Balance placeholders answer `0` for players the plugin has not loaded yet, such as a player who
  just left.
- Disabling `features.placeholders.enabled` unregisters the expansion entirely and stops its refresh
  task; no placeholder work happens at all.
