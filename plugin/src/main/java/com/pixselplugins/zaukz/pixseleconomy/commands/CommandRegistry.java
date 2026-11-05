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
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.PluginCommand;

/**
 * Builds every command handler once and wires it to both the main command and its standalone
 * command entry.
 */
public final class CommandRegistry {

    private final PixselEconomy plugin;

    public CommandRegistry(PixselEconomy plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers the executors and tab completers declared in plugin.yml.
     */
    public void register() {
        List<SubCommand> subCommands = new ArrayList<>();
        // The help handler keeps a reference to the list so it can render the other entries.
        subCommands.add(new HelpSubCommand(plugin, subCommands));
        subCommands.add(new PaySubCommand(plugin));
        subCommands.add(new BalanceSubCommand(plugin));
        subCommands.add(new WithdrawSubCommand(plugin));
        subCommands.add(new LeaderboardSubCommand(plugin));
        subCommands.add(new AddCurrencySubCommand(plugin));
        subCommands.add(new TakeCurrencySubCommand(plugin));
        subCommands.add(new SetCurrencySubCommand(plugin));
        subCommands.add(new CreateCurrencySubCommand(plugin));
        subCommands.add(new DeleteCurrencySubCommand(plugin));
        subCommands.add(new ResetAllSubCommand(plugin));
        subCommands.add(new ReloadSubCommand(plugin));

        EconomyCommand main = new EconomyCommand(plugin, subCommands);
        registerCommand("pixseleconomy", main);

        for (SubCommand subCommand : subCommands) {
            if (subCommand.standalone()) {
                registerCommand(subCommand.name(), new DirectCommand(plugin, subCommand));
            }
        }
    }

    private void registerCommand(String name, DirectCommand executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            plugin.getLogger().warning("Command '" + name + "' is missing from plugin.yml; skipping it.");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void registerCommand(String name, EconomyCommand executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            plugin.getLogger().warning("Command '" + name + "' is missing from plugin.yml; skipping it.");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
