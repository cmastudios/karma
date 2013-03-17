package com.tommytony.karma;

import com.google.common.collect.ImmutableList;
import com.tommytony.karma.commands.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

public class KarmaPlugin extends JavaPlugin implements KarmaAPI {

    protected Karma karma;

    //Called when bukkit disables plugin
    public void onDisable() {
        this.getServer().getScheduler().cancelTasks(this);
        karma.db.putAll();
        karma.players.clear();
    }

    //Caled when bukkit enables plugin
    public void onEnable() {
        karma = new Karma();
        karma.server = getServer();
        karma.log = getLogger();

        PluginManager manager = this.getServer().getPluginManager();

        // Setup config
        saveDefaultConfig();
        karma.config = getConfig();

        try {
            karma.config.load(new File("plugins/Karma/config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
            karma.log.severe("Your computer cannot read the config! Disabling..");
            manager.disablePlugin(this);
            return;
        } catch (InvalidConfigurationException e) {
            karma.log.severe("Bad configuration! Disabling..");
            manager.disablePlugin(this);
            return;
        }
        // Init data
        karma.players = new HashMap<String, KarmaPlayer>();
        karma.db = new Database(karma);
        karma.messages = ResourceBundle.getBundle("messages");
        File externalMessagesFile = new File(this.getDataFolder() + File.separator + "messages.properties");
        if (externalMessagesFile.exists()) {
            Properties prop = new Properties();
            InputStream in = null;
            OutputStream os = null;
            try {
                in = new FileInputStream(externalMessagesFile);
                prop.load(in);
                in.close();
                boolean missingkey = false;
                for (String key : karma.messages.keySet()) {
                    if (!prop.containsKey(key)) {
                        if (!missingkey) {
                            missingkey = true;
                            karma.log.info("Updating keys in external messages.properties file.");
                        }
                        prop.setProperty(key, karma.messages.getString(key));
                    }
                }
                os = new FileOutputStream(externalMessagesFile);
                prop.store(os, null);
                os.close();
                in = new FileInputStream(externalMessagesFile);
                karma.messages = new PropertyResourceBundle(in);
            } catch (IOException ex) {
                karma.log.warning("Cannot load external messages file, defaulting to internal messages.");
                karma.messages = ResourceBundle.getBundle("messages");
            } catch (MissingResourceException ex) {
                karma.log.warning("Cannot load external messages file, defaulting to internal messages.");
                karma.messages = ResourceBundle.getBundle("messages");
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                    if (os != null) {
                        os.close();
                    }
                } catch (IOException ex) {
                    karma.log.log(Level.SEVERE, null, ex);
                }
            }
        }


        // Load karma groups
        karma.loadTracks();

        // Register events

        KarmaWorldListener worldListener = new KarmaWorldListener(karma);
        manager.registerEvents(worldListener, this);

        KarmaPlayerListener playerListener = new KarmaPlayerListener(karma);
        playerListener.pingOnPlayerChat = karma.config.getBoolean("afk.triggers.chat", true);
        playerListener.pingOnPlayerCommand = karma.config.getBoolean("afk.triggers.command", true);
        playerListener.pingOnPlayerBuild = karma.config.getBoolean("afk.triggers.build", true);
        playerListener.pingOnPlayerMove = karma.config.getBoolean("afk.triggers.move", false);
        playerListener.pingOnPlayerDamage = karma.config.getBoolean("afk.triggers.damage", true);
        manager.registerEvents(playerListener, this);

        // Load online players
        this.getServer().getScheduler().runTask(this, new LoadPlayers(karma));

        // Check for war and enable bonuses if enabled in config
        try {
            Class.forName("com.tommytony.war.War");
            if (karma.config.getBoolean("war.bonus", false)) {
                karma.warEnabled = true;
            }
        } catch (ClassNotFoundException e) {
            karma.warEnabled = false;
            if (karma.config.getBoolean("war.bonus", false)) {
                karma.log.warning("war.bonus enabled in configuration but the War plugin was not found! Bonuses have been disabled.");
            }
        }

        // Launch karma party train!!
        this.getServer().getScheduler().runTaskLater(this, new KarmaParty(karma),
                karma.getNextRandomKarmaPartyDelay());
    }
    private final List<String> commands = ImmutableList.of("ranks", "help", "gift", "promote", "set", "add", "track");
    //called when a command is recieved by the server
    @Override
    public boolean onCommand(CommandSender sender, Command cmd,
            String alias, String[] args) {
        try {
            if (cmd.getName().equals("karma")
                    || cmd.getName().equals("k")) {
                if (args.length == 0) {
                    // Check their own karma
                    return new KarmaBaseCommand(karma).onCommand(sender, cmd, alias, args);
                }

                if (args[0].equalsIgnoreCase("ranks")) {
                    return new RanksCommand(karma).onCommand(sender, cmd, alias, args);
                }
                if (args[0].equalsIgnoreCase("help")) {
                    return new HelpCommand(karma).onCommand(sender, cmd, alias, args);
                }
                if (args[0].equalsIgnoreCase("gift")) {
                    return new GiftCommand(karma).onCommand(sender, cmd, alias, args);
                }
                if (args[0].equalsIgnoreCase("promote") || args[0].equalsIgnoreCase("promo")) {
                    return new PromoteCommand(karma).onCommand(sender, cmd, alias, args);
                }
                if (args[0].equalsIgnoreCase("set")) {
                    return new SetKarmaCommand(karma).onCommand(sender, cmd, alias, args);
                }
                if (args[0].equalsIgnoreCase("add")) {
                    return new AddKarmaCommand(karma).onCommand(sender, cmd, alias, args);
                }
                if (args[0].equalsIgnoreCase("track")) {
                    return new ChangeTrackCommand(karma).onCommand(sender, cmd, alias, args);
                }
                if (args.length == 1) {
                    return new CheckKarmaCommand(karma).onCommand(sender, cmd, alias, args);
                }

                karma.msg(sender, karma.config.getString("errors.unknowncommand"));
                return true;

            }

        } catch (Exception e) {
            karma.msg(sender, karma.config.getString("errors.commandexception").replace("<exception>", e.toString()));
            e.printStackTrace();
            karma.log.warning("Error encountered.");
        }

        return true;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], commands, new ArrayList<String>(commands.size()));
        } else if (args.length > 1) {
            if (args[0].equalsIgnoreCase("gift")) {
                return new GiftCommand(karma).onTabComplete(sender, cmd, alias, args);
            }
            if (args[0].equalsIgnoreCase("promote") || args[1].equalsIgnoreCase("promo")) {
                return new PromoteCommand(karma).onTabComplete(sender, cmd, alias, args);
            }
            if (args[0].equalsIgnoreCase("set")) {
                return new SetKarmaCommand(karma).onTabComplete(sender, cmd, alias, args);
            }
            if (args[0].equalsIgnoreCase("add")) {
                return new AddKarmaCommand(karma).onTabComplete(sender, cmd, alias, args);
            }
            if (args[0].equalsIgnoreCase("track")) {
                return new ChangeTrackCommand(karma).onTabComplete(sender, cmd, alias, args);
            }
        }
        return ImmutableList.of();
    }

    public Map<String, KarmaPlayer> getPlayers() {
        return karma.players;
    }

    public KarmaPlayer getPlayer(String player) {
        return karma.getPlayer(player);
    }

    public List<KarmaTrack> getTracks() {
        return karma.tracks;
    }

    public KarmaTrack getDefaultTrack() {
        return karma.getDefaultTrack();
    }

    public KarmaTrack getTrack(String name) {
        return karma.getTrack(name);
    }

    public KarmaTrack getTrack(long hash) {
        return karma.getTrack(hash);
    }

    public void reloadPlayers() {
        karma.players = new HashMap<String, KarmaPlayer>();
        new LoadPlayers(karma).run();
    }

    public void reloadTracks() {
        karma.loadTracks();
    }

}
