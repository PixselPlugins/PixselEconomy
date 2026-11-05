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
 * {@code /pixseleconomy help}: lists every feature the sender may use.
 */
public final class HelpSubCommand implements SubCommand {

    private final PixselEconomy plugin;
    private final List<SubCommand> subCommands;

    public HelpSubCommand(PixselEconomy plugin, List<SubCommand> subCommands) {
        this.plugin = plugin;
        this.subCommands = subCommands;
    }

    @Override
    public @NotNull String name() {
        return "help";
    }

    @Override
    public @NotNull String permission() {
        return "pixsel.economy.use";
    }

    @Override
    public @NotNull String usage() {
        return "pixseleconomy help";
    }

    @Override
    public @NotNull String description() {
        return "Show this list of commands.";
    }

    @Override
    public boolean standalone() {
        return false;
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        plugin.messages().send(sender, "help.header");
        for (SubCommand subCommand : subCommands) {
            if (subCommand instanceof HelpSubCommand || !sender.hasPermission(subCommand.permission())) {
                continue;
            }
            plugin.messages().send(sender, "help.entry", Map.of(
                    "usage", subCommand.usage(),
                    "description", subCommand.description()));
        }
        plugin.messages().send(sender, "help.footer");
    }
}
