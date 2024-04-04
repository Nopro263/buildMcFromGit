package dev.nopro263.buildmcfromgit;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogRecord;
import java.util.logging.Level;

public class Config {
    private YamlConfiguration configuration;
    private List<Plugin> plugin;
    public Config(File file) {
        this.configuration = YamlConfiguration.loadConfiguration(file);
        this.plugin = new ArrayList<>();

        for(String plugin:this.configuration.getKeys(false)) {
            try {
                this.plugin.add(this.load(plugin));
            } catch (NoSuchFieldException ex) {
                Bukkit.getLogger().log(new LogRecord(Level.WARNING, ex.getMessage()));
            }
        }
    }

    private Plugin load(String name) throws NoSuchFieldException{
        if(!this.configuration.contains(name)) throw new NoSuchFieldException("Section '" + name + "' not found");
        ConfigurationSection plugin = this.configuration.getConfigurationSection(name);

        if(!plugin.contains("allow_op")) throw new NoSuchFieldException("Boolean 'allow_op' not found");
        boolean allow_op = plugin.getBoolean("allow_op");

        if(!plugin.contains("allowed_players")) throw new NoSuchFieldException("List<String> 'allowed_players' not found");
        List<String> allowed_players = plugin.getStringList("allowed_players");

        if(!plugin.contains("build")) throw new NoSuchFieldException("Section 'build' not found");
        ConfigurationSection build = plugin.getConfigurationSection("build");

        String access_token = build.getString("access_token", null);

        if(!build.contains("organisation")) throw new NoSuchFieldException("String 'organisation' not found");
        String organisation = build.getString("organisation");

        if(!build.contains("repository")) throw new NoSuchFieldException("String 'repository' not found");
        String repository = build.getString("repository");

        if(!build.contains("branch")) throw new NoSuchFieldException("String 'branch' not found");
        String branch = build.getString("branch");

        return new Plugin(allow_op, allowed_players, access_token, organisation, repository, branch);
    }

    public List<Plugin> getPlugin() {
        return this.plugin;
    }

    public class Plugin {
        private boolean allow_op;
        private List<String> allowed_players;
        private String access_token;
        private String organisation;
        private String repo;
        private String branch;

        public Plugin(boolean allow_op, List<String> allowed_players, String access_token, String organisation, String repo, String branch) {
            this.allow_op = allow_op;
            this.allowed_players = allowed_players;
            this.access_token = access_token;
            this.organisation = organisation;
            this.repo = repo;
            this.branch = branch;
        }

        public void build() {

        }

        public boolean canBuild(String player, boolean isOp) {
            if(this.allowed_players.contains(player)) return true;
            if(this.allow_op && isOp) return true;

            return false;
        }
    }
}
