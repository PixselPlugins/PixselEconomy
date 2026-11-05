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
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Adapts a {@link SubCommand} to a standalone Bukkit command such as {@code /pay}.
 */
public final class DirectCommand implements CommandExecutor, TabCompleter {

    private final PixselEconomy plugin;
    private final SubCommand subCommand;

    public DirectCommand(PixselEconomy plugin, SubCommand subCommand) {
        this.plugin = plugin;
        this.subCommand = subCommand;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
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
        if (!sender.hasPermission(subCommand.permission())) {
            return List.of();
        }
        return subCommand.complete(sender, args);
    }
}
