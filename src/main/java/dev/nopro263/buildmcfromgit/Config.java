package dev.nopro263.buildmcfromgit;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLoader;
import org.yaml.snakeyaml.reader.StreamReader;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.nopro263.buildmcfromgit.PluginUtils.ansiToChatColor;

public class Config {
    private YamlConfiguration configuration;
    private List<Plugin> plugin;
    private File data_folder;
    private PluginLoader loader;
    public Config(File file, File data_folder, PluginLoader loader) {
        this.configuration = YamlConfiguration.loadConfiguration(file);
        this.plugin = new ArrayList<>();
        this.data_folder = data_folder;
        this.loader = loader;

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

        return new Plugin(name, allow_op, allowed_players, access_token, organisation, repository, branch);
    }

    public List<Plugin> getPlugin() {
        return this.plugin;
    }

    public boolean isPluginContained(String name) {
        for (Plugin p:this.plugin) {
            if(Objects.equals(p.name, name)) {
                return true;
            }
        }
        return false;
    }

    public Plugin getPlugin(String name) {
        for (Plugin p:this.plugin) {
            if(Objects.equals(p.name, name)) {
                return p;
            }
        }
        return null;
    }

    public class Plugin {
        private boolean allow_op;
        private List<String> allowed_players;
        private String access_token;
        private String organisation;
        private String repo;
        private String branch;
        private String name;

        public Plugin(String name, boolean allow_op, List<String> allowed_players, String access_token, String organisation, String repo, String branch) {
            this.name = name;
            this.allow_op = allow_op;
            this.allowed_players = allowed_players;
            this.access_token = access_token;
            this.organisation = organisation;
            this.repo = repo;
            this.branch = branch;
        }

        private File _build(File result, CommandSender sender) throws IOException {
            String current_directory = "/tmp/buildMcFromGit";
            String compressed_file = current_directory + "/latest.tar.gz";

            File cd = new File(current_directory);
            if(!cd.exists()) {
                cd.mkdirs();
            }

            URL website = new URL(String.format("https://api.github.com/repos/%s/%s/tarball/%s", this.organisation, this.repo, this.branch));
            URLConnection urlConnection = website.openConnection();

            if(this.access_token != null && !this.access_token.isEmpty()) {
                urlConnection.setRequestProperty("Authorization", "token " + this.access_token);
            }

            ReadableByteChannel rbc = Channels.newChannel(urlConnection.getInputStream());
            if(((HttpURLConnection) urlConnection).getResponseCode() != 200) {
                FileUtils.deleteDirectory(cd);
                throw new RuntimeException("non 200 code received (" + ((HttpURLConnection) urlConnection).getResponseCode() + ")");
            }
            FileOutputStream fos = new FileOutputStream(compressed_file);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

            try {
                decompress(compressed_file, cd);
            } catch (InterruptedException e) {
                FileUtils.deleteDirectory(cd);
                throw new RuntimeException(e);
            }

            File[] files = cd.listFiles(new Filter());

            if(files == null || files.length == 0) {
                FileUtils.deleteDirectory(cd);
                throw new RuntimeException("No file found");
            }
            File file = files[0];

            InvocationRequest request = new DefaultInvocationRequest();
            request.setQuiet(true);
            request.setOutputHandler(s -> sender.sendMessage(ansiToChatColor(s)));
            request.setErrorHandler(s -> sender.sendMessage(ansiToChatColor(s)));
            request.setPomFile( new File( file.getAbsolutePath() + "/pom.xml" ) );
            request.setGoals( Arrays.asList( "package" ) );

            Invoker invoker = new DefaultInvoker();
            try {
                invoker.execute( request );
            } catch (MavenInvocationException e) {
                FileUtils.deleteDirectory(cd);
                throw new RuntimeException(e);
            }

            File target = new File(file, "target");
            files = target.listFiles(new ResultFilter());
            if(files == null || files.length == 0) {
                FileUtils.deleteDirectory(cd);
                throw new RuntimeException("No file found");
            }
            file = files[0];

            System.out.println(file.getAbsolutePath());
            Path resultCopy = Files.copy(file.toPath(), new File(result, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            FileUtils.deleteDirectory(cd);

            return resultCopy.toFile();
        }

        private void decompress(String compressed_file, File directory) throws InterruptedException, IOException {
            String[] args = {"tar", "xz", "-f", compressed_file};
            ProcessBuilder processBuilder = new ProcessBuilder(args);
            processBuilder.directory(directory);
            Process p = processBuilder.start();
            p.waitFor();
        }

        public void build(CommandSender sender) throws InvalidPluginException, InvalidDescriptionException {
            File data_dir = Config.this.data_folder;
            PluginLoader loader = Config.this.loader;
            File f = null;
            try {
                f = this._build(data_dir.getParentFile(), sender);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            String name = loader.getPluginDescription(f).getName();

            for(org.bukkit.plugin.Plugin p:Bukkit.getPluginManager().getPlugins()) {
                if(p.getName().equals(name)) {
                    Bukkit.getPluginManager().disablePlugin(p);
                    PluginUtils.unload(p);
                }
            }

            org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().loadPlugin(f);
            plugin.onLoad();
            Bukkit.getPluginManager().enablePlugin(plugin);
        }

        public boolean canBuild(String player, boolean isOp) {
            if(this.allowed_players.contains(player)) return true;
            if(this.allow_op && isOp) return true;

            return false;
        }

        public String getName() {
            return this.name;
        }

        private class Filter implements FilenameFilter {

            @Override
            public boolean accept(File file, String s) {
                Pattern pattern = Pattern.compile("^.+?-.+?-[0-9a-f]+$", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(s);
                return matcher.find();
            }
        }

        private class ResultFilter implements FilenameFilter {

            @Override
            public boolean accept(File file, String s) {
                if(new File(file, s).isDirectory()) return false;
                Pattern pattern = Pattern.compile("^(?!original).*$", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(s);
                return matcher.find();
            }
        }
    }
}
