/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.config;

import com.pixselplugins.zaukz.pixseleconomy.PixselEconomy;
import com.pixselplugins.zaukz.pixseleconomy.integrations.PlaceholderApiBridge;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Loads {@code messages.yml} and renders MiniMessage text with the Pixsel prefix.
 */
public final class MessageService {

    private static final String FILE_NAME = "messages.yml";

    private final PixselEconomy plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private YamlConfiguration messages = new YamlConfiguration();
    private String prefix = "";

    public MessageService(PixselEconomy plugin) {
        this.plugin = plugin;
    }

    /**
     * Reads {@code messages.yml}, creating it from the bundled default when missing.
     */
    public void load() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
        prefix = messages.getString("prefix", "");
    }

    /**
     * Returns a raw, unprefixed template.
     *
     * @param key the message key
     * @return the template, or a visible placeholder when the key is missing
     */
    @NotNull
    public String raw(@NotNull String key) {
        String value = messages.getString(key);
        if (value == null) {
            plugin.getLogger().log(Level.WARNING, "Missing message key ''{0}'' in messages.yml.", key);
            return "<#FF6666>Missing message: " + key + "</#FF6666>";
        }
        return value;
    }

    /**
     * Returns the prefix used by every plugin message.
     *
     * @return the MiniMessage prefix
     */
    @NotNull
    public String prefix() {
        return prefix;
    }

    /**
     * Builds a prefixed message and replaces {@code {token}} placeholders.
     *
     * @param key          the message key
     * @param placeholders values keyed by token name, without braces
     * @return the prefixed MiniMessage string
     */
    @NotNull
    public String format(@NotNull String key, @NotNull Map<String, String> placeholders) {
        String text = raw(key);
        if (!placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                text = text.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return prefix + text;
    }

    /**
     * Builds a prefixed message without placeholder substitution.
     *
     * @param key the message key
     * @return the prefixed MiniMessage string
     */
    @NotNull
    public String format(@NotNull String key) {
        return format(key, Map.of());
    }

    /**
     * Builds an unprefixed message and replaces {@code {token}} placeholders. Used for item names,
     * lore and other text that must not carry the chat prefix.
     *
     * @param key          the message key
     * @param placeholders values keyed by token name, without braces
     * @return the rendered MiniMessage string
     */
    @NotNull
    public String rawFormat(@NotNull String key, @NotNull Map<String, String> placeholders) {
        String text = raw(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return text;
    }

    /**
     * Reads a message list and replaces {@code {token}} placeholders in every line.
     *
     * @param key          the message key
     * @param placeholders values keyed by token name, without braces
     * @return the rendered lines, empty when the key is missing or not a list
     */
    @NotNull
    public List<String> rawList(@NotNull String key, @NotNull Map<String, String> placeholders) {
        List<String> lines = messages.getStringList(key);
        if (lines.isEmpty()) {
            plugin.getLogger().log(Level.WARNING, "Missing message list ''{0}'' in messages.yml.", key);
            return List.of();
        }
        List<String> rendered = new ArrayList<>(lines.size());
        for (String line : lines) {
            String text = line;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                text = text.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            rendered.add(text);
        }
        return rendered;
    }

    /**
     * Renders MiniMessage text without resolving PlaceholderAPI placeholders.
     *
     * @param text the MiniMessage text
     * @return the component
     */
    @NotNull
    public Component parse(@NotNull String text) {
        return miniMessage.deserialize(text);
    }

    /**
     * Sends a prefixed message to a sender.
     *
     * @param sender       the receiver
     * @param key          the message key
     * @param placeholders values keyed by token name, without braces
     */
    public void send(@NotNull CommandSender sender, @NotNull String key, @NotNull Map<String, String> placeholders) {
        sendText(sender, format(key, placeholders));
    }

    /**
     * Sends a prefixed message to a sender.
     *
     * @param sender the receiver
     * @param key    the message key
     */
    public void send(@NotNull CommandSender sender, @NotNull String key) {
        send(sender, key, Map.of());
    }

    /**
     * Sends already-formatted MiniMessage text, applying PlaceholderAPI placeholders for players
     * when the integration is enabled.
     *
     * @param sender the receiver
     * @param text   the MiniMessage text
     */
    public void sendText(@NotNull CommandSender sender, @NotNull String text) {
        sender.sendMessage(render(sender, text));
    }

    /**
     * Renders MiniMessage text into a component, resolving PlaceholderAPI placeholders for players.
     *
     * @param sender the receiver, used to resolve player placeholders
     * @param text   the MiniMessage text
     * @return the rendered component
     */
    @NotNull
    public Component render(@NotNull CommandSender sender, @NotNull String text) {
        String resolved = text;
        if (sender instanceof Player player && plugin.settings().placeholderApiIntegration()
                && PlaceholderApiBridge.isAvailable()) {
            resolved = PlaceholderApiBridge.apply(player, resolved);
        }
        return miniMessage.deserialize(resolved);
    }

    /**
     * Applies PlaceholderAPI placeholders to a string for a player.
     *
     * @param player the player context
     * @param text   the text to resolve
     * @return the resolved text, or the input when the integration is unavailable
     */
    @NotNull
    public String applyPlaceholderApi(@NotNull Player player, @NotNull String text) {
        if (plugin.settings().placeholderApiIntegration() && PlaceholderApiBridge.isAvailable()) {
            return PlaceholderApiBridge.apply(player, text);
        }
        return text;
    }
}
