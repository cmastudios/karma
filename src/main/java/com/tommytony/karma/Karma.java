package com.tommytony.karma;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
    public KarmaGroup startGroup;
    public FileConfiguration config;
    public Random random = new Random();
    public boolean warEnabled = false;
    public Logger log;

    public Karma() {
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
                KarmaGroup currentGroup = startGroup;
                while (currentGroup != null) {
                    if (karmaPlayer.getKarmaPoints() >= currentGroup.getKarmaPoints()
                            && !player.hasPermission("karma."
                            + currentGroup.getGroupName())
                            && !(currentGroup.getNext() != null && karmaPlayer.getKarmaPoints() >= currentGroup.getNext().getKarmaPoints())) {
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

                    currentGroup = currentGroup.getNext();
                }//end while	
                // Check if a player has enough karma points for his rank, if not, add them
                // This allows easy installation of karma: no having to change preexisting users' karma to their rank's karma	
                currentGroup = startGroup;
                while (currentGroup != null) {
                    if (karmaPlayer.getKarmaPoints() < currentGroup.getKarmaPoints()
                            && player.hasPermission("karma." + currentGroup.getGroupName())) {

                        this.setAmount(player, currentGroup.getKarmaPoints());


                    }//end if

                    currentGroup = currentGroup.getNext();
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
                    initialKarma, System.currentTimeMillis(), 0);
            players.put(player.getName(), karmaPlayer);
            db.put(karmaPlayer);

            this.msg(player, config.getString("newplayer.message"));
            log.info("Karma> " + player.getName() + " created with "
                    + initialKarma + " karma points");
        }
    }

    private int getInitialKarma(Player player) {
        KarmaGroup group = startGroup;
        int initialKarma = 0;
        int karmaToNext = 0;
        while (group != null) {
            String perm = "karma." + group.getGroupName();
            if (player.hasPermission(perm)) {
                initialKarma = group.getKarmaPoints();
                if (group.getNext() != null) {
                    karmaToNext = group.getNext().getKarmaPoints()
                            - group.getKarmaPoints();
                } else {
                    // greybeards only initialize with 2020
                    karmaToNext = 100;
                }
            } else {
                break;
            }
            group = group.getNext();
        }
        initialKarma += (int) (0.2 * karmaToNext); // start bonus of 20% to next
        // rank
        return initialKarma;
    }

    public void checkForPromotion(String playerName, int before, int after) {
        KarmaGroup group = startGroup;
        Player playerForPromotion = this.findPlayer(playerName);
        while (group != null && playerForPromotion != null) {
            String perm = "karma." + group.getGroupName();
            if (before < group.getKarmaPoints()
                    && after >= group.getKarmaPoints()
                    && !playerForPromotion.hasPermission(perm)) {
                // promotion
                for (String cmd : config.getStringList("promotion.commands")) {
                    this.runCommand(cmd.replace("<player>", playerName).replace("<group>", group.getGroupName()));
                }
                for (Player player : server.getOnlinePlayers()) {
                    this.msg(
                            player,
                            config.getString("promotion.message").replace("<player>", playerName).replace("<group>", group.getGroupName()).replace("<groupcolor>",
                            group.getChatColor().toString()));
                }
                log.info("Karma> " + playerName + " promoted to " + group.getGroupName());
            }
            group = group.getNext();
        }
    }

    public void checkForDemotion(String playerName, int before, int after,
            boolean automatic) {
        KarmaGroup group = startGroup;
        Player playerForDemotion = this.findPlayer(playerName);
        while (group != null && playerForDemotion != null) {
            if (group.getNext() != null
                    && before >= group.getNext().getKarmaPoints()
                    && after < group.getNext().getKarmaPoints()) {
                String perm = "karma." + group.getNext().getGroupName();
                if (config.getBoolean("groups." + group.getGroupName()
                        + ".first")
                        && automatic) {
                    return; // Prevents players from being demoted to the first
                }							// rank
                if (playerForDemotion.hasPermission(perm)) {
                    // demotion
                    for (String cmd : config.getStringList("demotion.commands")) {
                        this.runCommand(cmd.replace("<player>", playerName).replace("<group>", group.getGroupName()));
                    }
                    for (Player player : server.getOnlinePlayers()) {
                        this.msg(
                                player,
                                config.getString("demotion.message").replace("<player>", playerName).replace("<group>",
                                group.getGroupName()).replace("<groupcolor>",
                                group.getChatColor().toString()));
                    }
                    log.info("Karma> " + playerName + " demoted to " + group.getGroupName());
                    break;
                }
            }
            group = group.getNext();
        }
    }

    public String getPlayerNextGroupString(KarmaPlayer karmaPlayer) {
        Player player = this.findPlayer(karmaPlayer.getName());
        KarmaGroup group = startGroup;
        while (group != null) {
            String perm = "karma." + group.getGroupName();
            if (!player.hasPermission(perm)) {
                return group.getGroupName() + " (" + ChatColor.GREEN
                        + group.getKarmaPoints() + ChatColor.GRAY + ")";
            }
            group = group.getNext();
        }
        return "none";
    }

    public String getPlayerGroupString(KarmaPlayer karmaPlayer) {
        Player player = this.findPlayer(karmaPlayer.getName());
        KarmaGroup group = startGroup;
        KarmaGroup lastGroup = null; // first group is recruit
        while (group != null) {
            String perm = "karma." + group.getGroupName();
            if (!player.hasPermission(perm) && group == startGroup) {
                log.severe(karmaPlayer.getName() + " does not have permissions for the start group! Permissions configured incorrectly (Did you forget inheritance?).");
                return "PERMISSIONS CONFIGURED INCORRECTLY";
            }
            if (!player.hasPermission(perm)) {
                return lastGroup.getChatColor() + lastGroup.getGroupName() + " (" + ChatColor.YELLOW
                        + lastGroup.getKarmaPoints() + ChatColor.GRAY + ")";
            }
            lastGroup = group;
            if (group.getNext() == null) {
                return group.getChatColor() + group.getGroupName() + " (" + ChatColor.YELLOW
                        + group.getKarmaPoints() + ChatColor.GRAY + ")";
            }
            group = group.getNext();
        }
        return "none";
    }

    public ChatColor getPlayerNextGroupColor(KarmaPlayer karmaPlayer) {
        Player player = this.findPlayer(karmaPlayer.getName());
        KarmaGroup group = startGroup;
        while (group != null) {
            String perm = "karma." + group.getGroupName();

            if (!player.hasPermission(perm)) {
                return group.getChatColor();
            }
            group = group.getNext();
        }
        return ChatColor.WHITE;
    }

    public ChatColor getPlayerGroupColor(KarmaPlayer karmaPlayer) {
        Player player = this.findPlayer(karmaPlayer.getName());
        KarmaGroup group = startGroup;
        KarmaGroup lastGroup = null; // first group is recruit
        while (group != null) {
            String perm = "karma." + group.getGroupName();
            if (!player.hasPermission(perm) && group == startGroup) {

                return ChatColor.RED;
            }
            if (!player.hasPermission(perm)) {
                return lastGroup.getChatColor();
            }
            lastGroup = group;
            if (group.getNext() == null) {
                return group.getChatColor();
            }
            group = group.getNext();
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

    protected void loadKarmaGroups() {
        Set<String> groups = config.getConfigurationSection("groups").getKeys(
                false);
        int currentGroup = 0;
        KarmaGroup nextgroup = null;
        for (String group : groups) {
            nextgroup = new KarmaGroup(group, config.getInt("groups." + group
                    + ".points"), nextgroup, ChatColor.getByChar(config.getString("groups." + group + ".color")));
            if (config.getBoolean("groups." + group + ".first")) {
                startGroup = nextgroup;
            }
            currentGroup++;
        }
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
