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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Dispatches {@code /pixseleconomy <subcommand>} to the same handlers the standalone commands use.
 */
public final class EconomyCommand implements CommandExecutor, TabCompleter {

    private final PixselEconomy plugin;
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public EconomyCommand(PixselEconomy plugin, List<SubCommand> subCommands) {
        this.plugin = plugin;
        for (SubCommand subCommand : subCommands) {
            // Player commands are standalone only; keeping them out of the map makes
            // /eco pay ... an unknown subcommand instead of a hidden duplicate.
            if (subCommand.exposedAsSubcommand()) {
                this.subCommands.put(subCommand.name(), subCommand);
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 0) {
            return run(sender, subCommands.get("help"), new String[0]);
        }
        SubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null) {
            plugin.messages().send(sender, "general.unknown-subcommand", Map.of("input", args[0]));
            return true;
        }
        return run(sender, subCommand, Arrays.copyOfRange(args, 1, args.length));
    }

    private boolean run(CommandSender sender, SubCommand subCommand, String[] args) {
        if (subCommand == null) {
            plugin.messages().send(sender, "general.no-permission");
            return true;
        }
        if (!sender.hasPermission(subCommand.permission())) {
            plugin.messages().send(sender, "general.no-permission");
            return true;
        }
        if (subCommand.playerOnly() && !(sender instanceof Player)) {
            plugin.messages().send(sender, "general.player-only");
            return true;
        }
        subCommand.execute(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                                      @NotNull String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return subCommands.values().stream()
                    .filter(subCommand -> sender.hasPermission(subCommand.permission()))
                    .map(SubCommand::name)
                    .filter(name -> name.startsWith(prefix))
                    .toList();
        }
        SubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null || !sender.hasPermission(subCommand.permission())) {
            return List.of();
        }
        return subCommand.complete(sender, Arrays.copyOfRange(args, 1, args.length));
    }
}
