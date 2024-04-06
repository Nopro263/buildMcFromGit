package dev.nopro263.buildmcfromgit.command;

import dev.nopro263.buildmcfromgit.Config;
import dev.nopro263.buildmcfromgit.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;

public class GitBuild implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(strings.length == 0) return true;
        if(!Main.getPluginConfig().isPluginContained(strings[0])) {
            commandSender.sendMessage(ChatColor.RED + "plugin not found");
            return true;
        }
        Config.Plugin plugin = Main.getPluginConfig().getPlugin(strings[0]);

        boolean canBuild = commandSender instanceof ConsoleCommandSender;
        if(plugin.canBuild(commandSender.getName(), commandSender.isOp())) {
            canBuild = true;
        }

        if(canBuild) {
            try {
                plugin.build(commandSender);
                commandSender.sendMessage(ChatColor.GREEN + "Built '" + strings[0] + "' successfully");
            } catch (InvalidPluginException | InvalidDescriptionException | RuntimeException e) {
                commandSender.sendMessage(ChatColor.RED + "Building '" + strings[0] + "' failed:");
                commandSender.sendMessage(e.getMessage());
                e.printStackTrace();
            }
        } else {
            commandSender.sendMessage(ChatColor.RED + "You are not allowed to build this");
        }
        return false;
    }
}
