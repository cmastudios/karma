package com.tommytony.karma;

import java.util.*;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class Karma {

    public Server server;
    public Map<String, KarmaPlayer> players;
    public Database db;
    public FileConfiguration config;
    public Random random;
    public boolean warEnabled;
    public Logger log;
    public List<KarmaTrack> tracks;

    public Karma() {
        random = new Random();
        warEnabled = false;
        tracks = new ArrayList<KarmaTrack>();
    }

    public boolean setAmount(List<Player> matches, int amount) {
        Player playerTarget = matches.get(0);
        return this.setAmount(playerTarget, amount);
    }

    public boolean setAmount(Player playerTarget, int amount) {
        KarmaPlayer karmaTarget = this.getPlayers().get(playerTarget.getName());
        if (karmaTarget != null && amount != karmaTarget.getKarmaPoints()) {
            int before = karmaTarget.getKarmaPoints();
            if (amount > karmaTarget.getKarmaPoints()) {
                karmaTarget.addKarma(amount - karmaTarget.getKarmaPoints());
            } else {
                karmaTarget.removeKarma(karmaTarget.getKarmaPoints() - amount);
            }
            this.msg(playerTarget, "Your karma was set to " + ChatColor.GREEN
                    + karmaTarget.getKarmaPoints() + ChatColor.GRAY + ".");
            StringBuilder message = new StringBuilder();
            message.append(playerTarget.getName()).append(" karma set to ").append(karmaTarget.getKarmaPoints()).append(" from ").append(before);
            log.info(message.toString());

            return true;
        }
        return false;
    }

    public void loadOrCreateKarmaPlayer(Player player) {
        String playerName = player.getName();
        if (db.exists(playerName)) {
            // existing player
            KarmaPlayer karmaPlayer = db.get(playerName);
            if (karmaPlayer != null) {
                players.put(playerName, karmaPlayer);

                // check if player needs a promo, in case perms got wiped
                // BONUS: this lets you change perms systems without having
                // to migrate - ex: just change the GM commands to bPerms
                KarmaGroup currentGroup = karmaPlayer.getTrack().getFirstGroup();
                while (currentGroup != null) {
                    if (karmaPlayer.getKarmaPoints() >= currentGroup.getKarmaPoints()
                            && !player.hasPermission("karma." + currentGroup.getGroupName())
                            && !(karmaPlayer.getTrack().getNextGroup(currentGroup) != null && karmaPlayer.getKarmaPoints() >= karmaPlayer.getTrack().getNextGroup(currentGroup).getKarmaPoints())) {
                        // either doesn't have a next rank or can't beat the
                        // next rank's k points, we found the right rank
                        for (String cmd : config.getStringList("promotion.commands")) {
                            this.runCommand(cmd.replace("<player>", playerName).replace("<group>", currentGroup.getGroupName()));
                        }
                        for (Player playerOnline : server.getOnlinePlayers()) {
                            this.msg(
                                    playerOnline,
                                    config.getString("promotion.message").replace("<player>", playerName).replace("<group>", currentGroup.getGroupName()).replace("<groupcolor>", currentGroup.getChatColor().toString()));
                        }//end for

                    }//end if

                    currentGroup = karmaPlayer.getTrack().getNextGroup(currentGroup);
                }//end while	
                // Check if a player has enough karma points for his rank, if not, add them
                // This allows easy installation of karma: no having to change preexisting users' karma to their rank's karma	
                currentGroup = karmaPlayer.getTrack().getFirstGroup();
                while (currentGroup != null) {
                    if (karmaPlayer.getKarmaPoints() < currentGroup.getKarmaPoints()
                            && player.hasPermission("karma." + currentGroup.getGroupName())) {

                        this.setAmount(player, currentGroup.getKarmaPoints());


                    }//end if

                    currentGroup = karmaPlayer.getTrack().getNextGroup(currentGroup);
                }//end while

                // check for last activity, remove one karma point per day off
                long gone = System.currentTimeMillis()
                        - karmaPlayer.getLastActivityTime();
                int howManyDays = (int) Math.floor(gone / 86400000L);

                if (howManyDays > 0) {
                    int before = karmaPlayer.getKarmaPoints();
                    karmaPlayer.removeKarmaAutomatic(howManyDays);
                    StringBuilder message = new StringBuilder();
                    message.append(player.getName()).append(" lost ").append(before - karmaPlayer.getKarmaPoints()).append(" karma points.");
                    log.info(message.toString());
                }

                // update last activity
                karmaPlayer.ping();
                db.put(karmaPlayer);
            }
        } else {
            // create player
            int initialKarma = this.getInitialKarma(player);
            KarmaPlayer karmaPlayer = new KarmaPlayer(this, player.getName(),
                    initialKarma, System.currentTimeMillis(), 0, getTrack("default"));
            players.put(player.getName(), karmaPlayer);
            db.put(karmaPlayer);

            this.msg(player, config.getString("newplayer.message"));
            log.info("Karma> " + player.getName() + " created with "
                    + initialKarma + " karma points");
        }
    }

    private int getInitialKarma(Player player) {
        KarmaGroup group = getTrack("default").getFirstGroup();
        int initialKarma = 0;
        int karmaToNext = 0;
        while (group != null) {
            String perm = "karma." + group.getGroupName();
            if (player.hasPermission(perm)) {
                initialKarma = group.getKarmaPoints();
                if (getTrack("default").getNextGroup(group) != null) {
                    karmaToNext = getTrack("default").getNextGroup(group).getKarmaPoints()
                            - group.getKarmaPoints();
                } else {
                    // greybeards only initialize with 2020
                    karmaToNext = 100;
                }
            } else {
                break;
            }
            group = getTrack("default").getNextGroup(group);
        }
        initialKarma += (int) (0.2 * karmaToNext); // start bonus of 20% to next
        // rank
        return initialKarma;
    }

    public void checkForPromotion(KarmaPlayer player, int before, int after) {
        KarmaGroup group = player.getTrack().getFirstGroup();
        Player playerForPromotion = this.findPlayer(player.getName());
        while (group != null && playerForPromotion != null) {
            String perm = "karma." + group.getGroupName();
            if (before < group.getKarmaPoints()
                    && after >= group.getKarmaPoints()
                    && !playerForPromotion.hasPermission(perm)) {
                // promotion
                for (String cmd : config.getStringList("promotion.commands")) {
                    this.runCommand(cmd.replace("<player>", player.getName()).replace("<group>", group.getGroupName()));
                }
                for (Player playerToMessage : server.getOnlinePlayers()) {
                    this.msg(
                            playerToMessage,
                            config.getString("promotion.message").replace("<player>", playerToMessage.getName()).replace("<group>", group.getGroupName()).replace("<groupcolor>",
                            group.getChatColor().toString()));
                }
                log.info(player.getName() + " promoted to " + group.getGroupName());
            }
            group = player.getTrack().getNextGroup(group);
        }
    }

    public void checkForDemotion(KarmaPlayer player, int before, int after,
            boolean automatic) {
        KarmaGroup group = player.getTrack().getFirstGroup();
        Player playerForDemotion = this.findPlayer(player.getName());
        while (group != null && playerForDemotion != null) {
            if (player.getTrack().getNextGroup(group) != null
                    && before >= player.getTrack().getNextGroup(group).getKarmaPoints()
                    && after < player.getTrack().getNextGroup(group).getKarmaPoints()) {
                String perm = "karma." + player.getTrack().getNextGroup(group).getGroupName();
                if (config.getBoolean("groups." + group.getGroupName()
                        + ".first")
                        && automatic) {
                    return; // Prevents players from being demoted to the first
                }							// rank
                if (playerForDemotion.hasPermission(perm)) {
                    // demotion
                    for (String cmd : config.getStringList("demotion.commands")) {
                        this.runCommand(cmd.replace("<player>", player.getName()).replace("<group>", group.getGroupName()));
                    }
                    for (Player playerToMessage : server.getOnlinePlayers()) {
                        this.msg(
                                playerToMessage,
                                config.getString("demotion.message").replace("<player>", player.getName()).replace("<group>",
                                group.getGroupName()).replace("<groupcolor>",
                                group.getChatColor().toString()));
                    }
                    log.info(player.getName() + " demoted to " + group.getGroupName());
                    break;
                }
            }
            group = player.getTrack().getNextGroup(group);
        }
    }

    public String getPlayerNextGroupString(KarmaPlayer karmaPlayer) {
        Player player = this.findPlayer(karmaPlayer.getName());
        KarmaGroup group = karmaPlayer.getTrack().getFirstGroup();
        while (group != null) {
            String perm = "karma." + group.getGroupName();
            if (!player.hasPermission(perm)) {
                return group.getGroupName() + " (" + ChatColor.GREEN
                        + group.getKarmaPoints() + ChatColor.GRAY + ")";
            }
            group = karmaPlayer.getTrack().getNextGroup(group);
        }
        return "none";
    }

    public String getPlayerGroupString(KarmaPlayer karmaPlayer) {
        Player player = this.findPlayer(karmaPlayer.getName());
        KarmaGroup group = karmaPlayer.getTrack().getFirstGroup();
        KarmaGroup lastGroup = null; // first group is recruit
        while (group != null) {
            String perm = "karma." + group.getGroupName();
            if (!player.hasPermission(perm) && group == karmaPlayer.getTrack().getFirstGroup()) {
                log.severe(karmaPlayer.getName() + " does not have permissions for the start group! Permissions configured incorrectly (Did you forget inheritance?).");
                return "PERMISSIONS CONFIGURED INCORRECTLY";
            }
            if (!player.hasPermission(perm)) {
                return lastGroup.getChatColor() + lastGroup.getGroupName() + " (" + ChatColor.YELLOW
                        + lastGroup.getKarmaPoints() + ChatColor.GRAY + ")";
            }
            lastGroup = group;
            if (karmaPlayer.getTrack().getNextGroup(group) == null) {
                return group.getChatColor() + group.getGroupName() + " (" + ChatColor.YELLOW
                        + group.getKarmaPoints() + ChatColor.GRAY + ")";
            }
            group = karmaPlayer.getTrack().getNextGroup(group);
        }
        return "none";
    }

    public ChatColor getPlayerNextGroupColor(KarmaPlayer karmaPlayer) {
        Player player = this.findPlayer(karmaPlayer.getName());
        KarmaGroup group = karmaPlayer.getTrack().getFirstGroup();
        while (group != null) {
            String perm = "karma." + group.getGroupName();

            if (!player.hasPermission(perm)) {
                return group.getChatColor();
            }
            group = karmaPlayer.getTrack().getNextGroup(group);
        }
        return ChatColor.WHITE;
    }

    public ChatColor getPlayerGroupColor(KarmaPlayer karmaPlayer) {
        Player player = this.findPlayer(karmaPlayer.getName());
        KarmaGroup group = karmaPlayer.getTrack().getFirstGroup();
        KarmaGroup lastGroup = null; // first group is recruit
        while (group != null) {
            String perm = "karma." + group.getGroupName();
            if (!player.hasPermission(perm) && group == karmaPlayer.getTrack().getFirstGroup()) {

                return ChatColor.RED;
            }
            if (!player.hasPermission(perm)) {
                return lastGroup.getChatColor();
            }
            lastGroup = group;
            if (karmaPlayer.getTrack().getNextGroup(group) == null) {
                return group.getChatColor();
            }
            group = karmaPlayer.getTrack().getNextGroup(group);
        }
        return ChatColor.WHITE;
    }

    public Player findPlayer(String playerName) {
        for (Player player : server.getOnlinePlayers()) {
            if (player.getName().equals(playerName)) {
                return player;
            }
        }
        return null;
    }

    public int getNextRandomKarmaPartyDelay() {
        // on average 20, between 10 min and 30 min
        int minutes = config.getInt("party.time.minimum")
                + random.nextInt(config.getInt("party.time.maximum")
                - config.getInt("party.time.minimum"));
        // 20 ticks/second, 60 seconds/min
        int ticks = minutes * 20 * 60;
        log.info("Next karma party in " + minutes + " minutes or " + ticks + " ticks.");
        return ticks;
    }
    protected void loadTracks() {
        Set<String> stracks = config.getConfigurationSection("tracks").getKeys(
                false);
        tracks.clear();
        for (String strack : stracks) {
            KarmaTrack track = new KarmaTrack(strack);
            track.setGroups(loadKarmaGroups(track));
            tracks.add(track);
        }
    }
    protected List<KarmaGroup> loadKarmaGroups(KarmaTrack track) {
        Set<String> groups = config.getConfigurationSection("tracks." + 
                track.getName()).getKeys(false);
        List<KarmaGroup> ret = new ArrayList<KarmaGroup>();
        for (String group : groups) {
            ret.add(new KarmaGroup(group, 
                    config.getInt("tracks." + track.getName() + "." + group + ".points"), 
                    ChatColor.getByChar(
                    config.getString("tracks." + track.getName() + "." + group + ".color"))));
        }
        return ret;
    }

    public void runCommand(String command) {
        if (command.contains("<NEWLINE>")) {
            for (String c : command.split("<NEWLINE>")) {
                server.dispatchCommand(server.getConsoleSender(), c);
                return;
            }
        }
        server.dispatchCommand(server.getConsoleSender(), command);
    }

    public void msg(CommandSender destination, String message) {
        if (message == null || "".equals(message)) {
            return;
        }
        if (message.contains("<NEWLINE>")) {
            for (String s : message.split("<NEWLINE>")) {
                destination.sendMessage(parseColor(config.getString("prefix")
                        + s));
            }
            return;
        }
        destination.sendMessage(parseColor(config.getString("prefix") + message));
    }
    /**
     * Get a track by name
     * @param name the tracks name
     * @return the track or null if not found
     */
    public KarmaTrack getTrack(String name) {
        for (KarmaTrack track : tracks) {
            if (track.getName().equals(name)) {
                return track;
            }
        }
        return null;
    }
    
    public KarmaTrack getTrack(long hash) {
        for(KarmaTrack track : tracks) {
            if(track.getName().hashCode() == hash) {
                return track;
            }
        }
        return null;
    }

    private String parseColor(String message) {
        return message.replaceAll("&([0-9a-zA-Z])", ChatColor.COLOR_CHAR + "$1");
    }

    public Map<String, KarmaPlayer> getPlayers() {
        return players;
    }

    public Database getKarmaDatabase() {
        return db;
    }
}
