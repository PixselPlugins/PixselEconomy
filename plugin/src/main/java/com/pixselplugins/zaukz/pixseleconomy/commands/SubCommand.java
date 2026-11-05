/*
 * ╔══════════════════════════════════════════╗
 * ║              PIXSEL PLUGINS              ║
 * ║             Authored by zAukz            ║
 * ║                                          ║
 * ║           PixselEconomy v1.0.0           ║
 * ╚══════════════════════════════════════════╝
 */
package com.pixselplugins.zaukz.pixseleconomy.commands;

import com.pixselplugins.zaukz.pixseleconomy.PixselEconomy;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * One feature of the plugin.
 *
 * <p>Player features are registered as their own command such as {@code /pay} and are not reachable
 * through the main command. Administration features are the opposite: they are only reachable as
 * {@code /pixseleconomy <name>}, so they never occupy a top-level command name.</p>
 *
 * <p>Implementations only contain argument parsing and messages; permission checks are performed by
 * the dispatchers so both entry points behave identically.</p>
 */
public interface SubCommand {

    /**
     * Returns the command name, for example {@code "pay"}.
     *
     * @return the lowercase name
     */
    @NotNull
    String name();

    /**
     * Returns the permission required to run the feature.
     *
     * @return the permission node
     */
    @NotNull
    String permission();

    /**
     * Returns the usage line without a leading slash.
     *
     * @return the usage, for example {@code "pay <player> <amount> [currency]"}
     */
    @NotNull
    String usage();

    /**
     * Returns a short description shown by {@code /pixseleconomy help}.
     *
     * @return the description
     */
    @NotNull
    String description();

    /**
     * Returns whether the feature is also registered as its own command.
     *
     * @return {@code true} when a command entry exists in plugin.yml
     */
    default boolean standalone() {
        return true;
    }

    /**
     * Returns whether the feature is reachable as {@code /pixseleconomy <name>}.
     *
     * @return {@code true} when the main command dispatches to it
     */
    default boolean exposedAsSubcommand() {
        return true;
    }

    /**
     * Returns whether only players may run the feature.
     *
     * @return {@code true} when the sender must be a player
     */
    default boolean playerOnly() {
        return false;
    }

    /**
     * Runs the feature.
     *
     * @param sender the sender, already permission checked
     * @param args   the arguments after the feature name
     */
    void execute(@NotNull CommandSender sender, @NotNull String[] args);

    /**
     * Offers tab completions for the arguments after the feature name.
     *
     * @param sender the sender, already permission checked
     * @param args   the arguments so far
     * @return the suggestions, empty when there is nothing to suggest
     */
    @NotNull
    default List<String> complete(@NotNull CommandSender sender, @NotNull String[] args) {
        return List.of();
    }

    /**
     * Sends the usage message of this feature.
     *
     * @param plugin the plugin providing the message service
     * @param sender the sender
     */
    default void sendUsage(@NotNull PixselEconomy plugin, @NotNull CommandSender sender) {
        plugin.messages().send(sender, "general.invalid-usage", Map.of("usage", "/" + usage()));
    }
}
