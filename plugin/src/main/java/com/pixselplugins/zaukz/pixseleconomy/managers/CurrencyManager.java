/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.managers;

import com.pixselplugins.zaukz.pixseleconomy.PixselEconomy;
import com.pixselplugins.zaukz.pixseleconomy.api.Currency;
import com.pixselplugins.zaukz.pixseleconomy.api.PixselCurrencyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Loads, validates and persists currency definitions from {@code currencies.yml}.
 *
 * <p>The published view is an immutable map that is swapped as a whole, so lookups from the server
 * thread, the database thread and placeholder requests never observe a partially reloaded set.</p>
 */
public final class CurrencyManager {

    private static final String FILE_NAME = "currencies.yml";
    private static final Pattern VALID_ID = Pattern.compile("^[a-z0-9_-]{1,32}$");

    private final PixselEconomy plugin;
    private final List<Currency> ordered = new ArrayList<>();

    private volatile Map<String, Currency> currencies = Map.of();
    private volatile String defaultId = "";
    private YamlConfiguration file = new YamlConfiguration();

    public CurrencyManager(PixselEconomy plugin) {
        this.plugin = plugin;
    }

    /**
     * Reads {@code currencies.yml}, creating it from the bundled default when missing.
     */
    public void load() {
        File configFile = new File(plugin.getDataFolder(), FILE_NAME);
        if (!configFile.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
        file = YamlConfiguration.loadConfiguration(configFile);

        Map<String, Currency> loaded = new LinkedHashMap<>();
        ConfigurationSection section = file.getConfigurationSection("currencies");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String id = key.toLowerCase(Locale.ROOT);
                ConfigurationSection currencySection = section.getConfigurationSection(key);
                if (currencySection == null) {
                    plugin.getLogger().warning("Currency '" + key + "' in currencies.yml is not a section, skipping it.");
                    continue;
                }
                if (!VALID_ID.matcher(id).matches()) {
                    plugin.getLogger().warning("Currency id '" + key + "' is not valid (lowercase letters, digits, "
                            + "'-' and '_' only), skipping it.");
                    continue;
                }
                loaded.put(id, read(id, currencySection));
            }
        }

        if (loaded.isEmpty()) {
            plugin.getLogger().warning("No usable currencies found in currencies.yml, creating a default 'coins' entry.");
            Currency fallback = defaultCurrency("coins");
            loaded.put(fallback.getId(), fallback);
            persist(fallback);
        }

        String configuredDefault = plugin.settings().defaultCurrencyId();
        if (loaded.containsKey(configuredDefault)) {
            this.defaultId = configuredDefault;
        } else {
            String first = loaded.keySet().iterator().next();
            plugin.getLogger().warning("Default currency '" + configuredDefault
                    + "' does not exist in currencies.yml, using '" + first + "' instead.");
            this.defaultId = first;
        }

        ordered.clear();
        ordered.addAll(loaded.values());
        this.currencies = Map.copyOf(loaded);
    }

    private Currency read(String id, ConfigurationSection section) {
        return Currency.builder(id)
                .displayName(section.getString("display-name", id))
                .symbol(section.getString("symbol", ""))
                .format(section.getString("format", "{symbol}{amount}"))
                .decimals(section.getInt("decimals", 2))
                .defaultBalance(section.getDouble("starting-balance", 0.0D))
                .maxBalance(section.getDouble("max-balance", 0.0D))
                .payable(section.getBoolean("payable", true))
                .withdrawable(section.getBoolean("withdrawable", true))
                .withdrawMinimum(section.getDouble("withdraw.minimum", plugin.settings().withdrawMinimum()))
                .withdrawMaximum(section.getDouble("withdraw.maximum", plugin.settings().withdrawMaximum()))
                .withdrawFee(section.getDouble("withdraw.fee", 0.0D))
                .withdrawFeePercentage(section.getDouble("withdraw.fee-percentage", 0.0D))
                .build();
    }

    /**
     * Builds the currency used when the configuration contains no usable entry.
     *
     * @param id the identifier
     * @return a currency with the plugin defaults
     */
    @NotNull
    public Currency defaultCurrency(@NotNull String id) {
        return Currency.builder(id)
                .displayName("Coins")
                .symbol("$")
                .defaultBalance(100.0D)
                .withdrawMinimum(plugin.settings().withdrawMinimum())
                .withdrawMaximum(plugin.settings().withdrawMaximum())
                .build();
    }

    /**
     * Adds a currency to {@code currencies.yml} and to the live set.
     *
     * @param currency the currency to add
     * @return {@code false} when the identifier is already taken
     */
    public boolean create(@NotNull Currency currency) {
        String id = currency.getId();
        if (currencies.containsKey(id)) {
            return false;
        }
        persist(currency);
        Map<String, Currency> updated = new LinkedHashMap<>(currencies);
        updated.put(id, currency);

        List<Currency> updatedOrder = new ArrayList<>(ordered);
        updatedOrder.add(currency);
        this.ordered.clear();
        this.ordered.addAll(updatedOrder);
        this.currencies = Map.copyOf(updated);
        return true;
    }

    /**
     * Removes a currency from {@code currencies.yml} and from the live set.
     *
     * @param id the currency identifier
     * @return the removed currency, or {@code null} when it did not exist
     */
    @Nullable
    public Currency remove(@NotNull String id) {
        String key = id.toLowerCase(Locale.ROOT);
        Currency removed = currencies.get(key);
        if (removed == null) {
            return null;
        }
        Map<String, Currency> updated = new LinkedHashMap<>(currencies);
        updated.remove(key);
        this.currencies = Map.copyOf(updated);

        List<Currency> updatedOrder = new ArrayList<>(ordered);
        updatedOrder.removeIf(currency -> currency.getId().equals(key));
        this.ordered.clear();
        this.ordered.addAll(updatedOrder);

        file.set("currencies." + key, null);
        saveFile();
        return removed;
    }

    private void persist(Currency currency) {
        String path = "currencies." + currency.getId() + ".";
        file.set(path + "display-name", currency.getDisplayName());
        file.set(path + "symbol", currency.getSymbol());
        file.set(path + "format", currency.getFormat());
        file.set(path + "decimals", currency.getDecimals());
        file.set(path + "starting-balance", currency.getDefaultBalance());
        // 0 means unlimited; writing Double.MAX_VALUE would be noise in the file.
        file.set(path + "max-balance", currency.getMaxBalance() >= Double.MAX_VALUE ? 0 : currency.getMaxBalance());
        file.set(path + "payable", currency.isPayable());
        file.set(path + "withdrawable", currency.isWithdrawable());
        file.set(path + "withdraw.minimum", currency.getWithdrawMinimum());
        file.set(path + "withdraw.maximum", currency.getWithdrawMaximum());
        file.set(path + "withdraw.fee", currency.getWithdrawFee());
        file.set(path + "withdraw.fee-percentage", currency.getWithdrawFeePercentage());
        saveFile();
    }

    private void saveFile() {
        try {
            file.save(new File(plugin.getDataFolder(), FILE_NAME));
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save currencies.yml.", exception);
        }
    }

    /**
     * Looks up a currency by identifier, ignoring case.
     *
     * @param id the currency identifier
     * @return the currency, or {@code null} when it does not exist
     */
    @Nullable
    public Currency get(@Nullable String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return currencies.get(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns the default currency. A currency always exists after loading.
     *
     * @return the default currency
     */
    @NotNull
    public Currency getDefault() {
        Currency currency = currencies.get(defaultId);
        if (currency == null) {
            // Loading guarantees at least one entry, so this only happens during shutdown races.
            throw new IllegalStateException("No default currency is loaded.");
        }
        return currency;
    }

    /**
     * Returns the identifier of the default currency.
     *
     * @return the identifier, never {@code null}
     */
    @NotNull
    public String defaultId() {
        return defaultId;
    }

    /**
     * Returns every currency in configuration order.
     *
     * @return an immutable view of all currencies
     */
    @NotNull
    public Collection<Currency> all() {
        return ordered;
    }

    /**
     * Returns the identifiers of all currencies, in configuration order.
     *
     * @return the identifiers
     */
    @NotNull
    public List<String> ids() {
        List<String> identifiers = new ArrayList<>(ordered.size());
        for (Currency currency : ordered) {
            identifiers.add(currency.getId());
        }
        return identifiers;
    }

    /**
     * Checks whether a currency identifier is already in use.
     *
     * @param id the identifier
     * @return {@code true} when the currency exists
     */
    public boolean exists(@NotNull String id) {
        return currencies.containsKey(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Checks whether an identifier may be used for a new currency.
     *
     * @param id the identifier
     * @return {@code true} for lowercase letters, digits, hyphens and underscores up to 32 chars
     */
    public boolean isValidId(@NotNull String id) {
        return VALID_ID.matcher(id).matches();
    }

    /**
     * Fires the currency lifecycle event for external listeners.
     *
     * @param type     what happened
     * @param currency the affected currency
     */
    public void fireChange(@NotNull PixselCurrencyChangeEvent.Type type, @NotNull Currency currency) {
        plugin.getServer().getPluginManager().callEvent(new PixselCurrencyChangeEvent(type, currency));
    }
}
