/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.utils;

import com.pixselplugins.zaukz.pixseleconomy.api.Currency;
import com.pixselplugins.zaukz.pixseleconomy.api.TransactionResult;
import com.pixselplugins.zaukz.pixseleconomy.api.TransactionStatus;
import com.pixselplugins.zaukz.pixseleconomy.config.MessageService;
import com.pixselplugins.zaukz.pixseleconomy.managers.CurrencyManager;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Translates transaction outcomes into the matching message keys so every command reports failures
 * the same way.
 */
public final class StatusMessages {

    private StatusMessages() {
    }

    /**
     * Maps a status to a message key.
     *
     * @param status the transaction status
     * @return the message key, or {@code null} for a successful operation
     */
    @Nullable
    public static String keyFor(@NotNull TransactionStatus status) {
        return switch (status) {
            case SUCCESS -> null;
            case INVALID_AMOUNT -> "general.invalid-amount";
            case UNKNOWN_CURRENCY -> "general.unknown-currency";
            case INSUFFICIENT_FUNDS -> "general.insufficient-funds";
            case ACCOUNT_UNAVAILABLE -> "general.account-unavailable";
            case LIMIT_EXCEEDED -> "general.limit-exceeded";
            case NOT_PAYABLE -> "general.currency-not-payable";
            case CANCELLED -> "general.cancelled";
            case STORAGE_ERROR -> "general.storage-error";
        };
    }

    /**
     * Sends the message that belongs to a failed transaction.
     *
     * @param messages     the message service
     * @param currencies   the currency registry, used to resolve a display name
     * @param sender       the receiver
     * @param result       the failed result
     * @param placeholders extra placeholders, may be empty
     * @return {@code true} when a message was sent
     */
    public static boolean send(MessageService messages, CurrencyManager currencies, CommandSender sender,
                               TransactionResult result, Map<String, String> placeholders) {
        String key = keyFor(result.getStatus());
        if (key == null) {
            return false;
        }
        Map<String, String> values = new HashMap<>(placeholders);
        Currency currency = currencies.get(result.getCurrencyId());
        values.putIfAbsent("currency", currency == null ? result.getCurrencyId() : currency.getDisplayName());
        messages.send(sender, key, values);
        return true;
    }
}
