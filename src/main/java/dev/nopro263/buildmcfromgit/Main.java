package dev.nopro263.buildmcfromgit;

import dev.nopro263.buildmcfromgit.command.GitBuild;
import dev.nopro263.buildmcfromgit.complete.GitBuildTab;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;

public final class Main extends JavaPlugin {
    private static Config config;

    private void fillDefaultFile(File file) throws IOException {
        OutputStream stream = Files.newOutputStream(file.toPath());
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(stream);
        try {
            outputStreamWriter.write("myPlugin:\n" +
                    "  build:\n" +
                    "    access_token: \"secret\"\n" +
                    "    organisation: \"MyOrg\"\n" +
                    "    repository: \"MyPlugin\"\n" +
                    "    branch: \"master\"\n" +
                    "  allow_op: false\n" +
                    "  allowed_players:\n" +
                    "    - Dinnerbone");
        }
        finally {
            outputStreamWriter.close();
            stream.close();
        }
    }

    private void loadConfig() throws IOException {
        if(!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        File f = new File(getDataFolder(), "plugin.yml");
        if(!f.exists()) {
            f.createNewFile();
            fillDefaultFile(f);
        }
        config = new Config(f, getDataFolder(), getPluginLoader());
    }

    public static Config getPluginConfig() {
        return config;
    }

    @Override
    public void onEnable() {
        try {
            loadConfig();
        } catch (IOException ex) {
            System.err.println("Error in loading plugins: " + ex);
        }

        getCommand("gitbuild").setExecutor(new GitBuild());
        getCommand("gitbuild").setTabCompleter(new GitBuildTab());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
