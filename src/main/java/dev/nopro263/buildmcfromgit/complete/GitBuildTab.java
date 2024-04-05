package dev.nopro263.buildmcfromgit.complete;

import dev.nopro263.buildmcfromgit.Config;
import dev.nopro263.buildmcfromgit.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class GitBuildTab implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if(strings.length > 1) return null;
        List<String> list = new ArrayList<>();
        for (Config.Plugin p:Main.getPluginConfig().getPlugin()) {
            if(strings.length == 0 || p.getName().startsWith(strings[0])) {
                list.add(p.getName());
            }
        }
        return list;
    }
}
