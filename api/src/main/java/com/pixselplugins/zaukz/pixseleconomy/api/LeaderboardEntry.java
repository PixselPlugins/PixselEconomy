/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.api;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * One row of a currency leaderboard.
 *
 * @param playerId   the account owner
 * @param playerName the last known name of the account owner
 * @param balance    the balance of the account
 * @param position   the one-based rank of the account within the currency
 * @since 1.0.0
 */
public record LeaderboardEntry(@NotNull UUID playerId, @NotNull String playerName, double balance, int position) {
}
