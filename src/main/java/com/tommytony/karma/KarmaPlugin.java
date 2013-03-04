package com.tommytony.karma;

import com.tommytony.karma.commands.*;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

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

        // Load karma groups
        karma.loadTracks();

        // Register events

        KarmaWorldListener worldListener = new KarmaWorldListener(karma);
        manager.registerEvents(worldListener, this);

        KarmaPlayerListener playerListener = new KarmaPlayerListener(karma);
        playerListener.pingOnPlayerChat = karma.config.getBoolean("afk.triggers.chat");
        playerListener.pingOnPlayerCommand = karma.config.getBoolean("afk.triggers.command");
        playerListener.pingOnPlayerBuild = karma.config.getBoolean("afk.triggers.build");
        playerListener.pingOnPlayerMove = karma.config.getBoolean("afk.triggers.move");
        manager.registerEvents(playerListener, this);

        // Load online players
        this.getServer().getScheduler().runTask(this, new LoadPlayers(karma));

        // Check for war and enable bonuses if enabled in config
        try {
            Class.forName("com.tommytony.war.War");
            if (karma.config.getBoolean("war.bonus")) {
                karma.warEnabled = true;
            }
        } catch (ClassNotFoundException e) {
            karma.warEnabled = false;
            if (karma.config.getBoolean("war.bonus")) {
                karma.log.warning("war.bonus enabled in configuration but the War plugin was not found! Bonuses have been disabled.");
            }
        }

        // Launch karma party train!!
        this.getServer().getScheduler().runTaskLater(this, new KarmaParty(karma),
                karma.getNextRandomKarmaPartyDelay());
    }
    
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
