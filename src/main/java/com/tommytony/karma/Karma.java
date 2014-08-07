package com.tommytony.karma;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Logger;
import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
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
    public ResourceBundle messages;

    public Karma() {
        random = new Random();
        warEnabled = false;
        tracks = new ArrayList<KarmaTrack>();
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
                for (KarmaGroup currentGroup : karmaPlayer.getTrack()) {
                    if (karmaPlayer.getKarmaPoints() >= currentGroup.getKarmaPoints() // Player has enough karma for this rank
                            && !player.hasPermission(currentGroup.getPermission()) // and he doesn't have permission for it...
                            // and he doesn't have the karma for his next group
                            && !(karmaPlayer.getTrack().getNextGroup(currentGroup) != null && karmaPlayer.getKarmaPoints() >= karmaPlayer.getTrack().getNextGroup(currentGroup).getKarmaPoints())) {
                        // either doesn't have a next rank or can't beat the
                        // next rank's k points, we found the right rank
                        this.runCommand(config.getString("promotion.command").replace("<player>", player.getName()).replace("<group>", currentGroup.getGroupName()));
                        for (Player playerOnline : server.getOnlinePlayers()) {
                            this.msg(playerOnline, this.getString("PLAYER.PROMOTED", new Object[] {player.getName(), currentGroup.getFormattedName()}));
                        }//end for
                    }//end if
                }//end while
                // Check if a player has enough karma points for his rank, if not, add them
                // This prevents problems when server admins use their permission manager's commands for promotions instead of karma's
                // TODO: add configuration option to disable this for setups with one mysql db for karma but multiple servers
                for (KarmaGroup currentGroup : karmaPlayer.getTrack()) {
                    if (karmaPlayer.getKarmaPoints() < currentGroup.getKarmaPoints()
                            && player.hasPermission(currentGroup.getPermission())) {
                        karmaPlayer.setKarmaPoints(currentGroup.getKarmaPoints());
                    }//end if
                }//end while

                // check for last activity, remove one karma point per day off
                // TODO: make time and loss configurable
                long gone = System.currentTimeMillis()
                        - karmaPlayer.getLastActivityTime();
                int howManyDays = (int) Math.floor(gone / 86400000L);

                if (howManyDays > 0) {
                    int before = karmaPlayer.getKarmaPoints();
                    karmaPlayer.removeKarmaAutomatic(howManyDays);
                    log.finer(String.format("%s lost %d karma points.", player.getName(), before - karmaPlayer.getKarmaPoints()));
                }

                // update last activity
                karmaPlayer.ping();
                db.put(karmaPlayer);
            }
        } else {
            // create player
            int initialKarma = this.getInitialKarma(player);
            KarmaPlayer karmaPlayer = new KarmaPlayer(this, player,
                    initialKarma, System.currentTimeMillis(), 0, getInitialTrack(player));
            players.put(player.getName(), karmaPlayer);
            db.put(karmaPlayer);

            this.msg(player, this.getString("WELCOME"));
            log.finer(String.format("%s created with %d karma points", player.getName(), initialKarma));
        }
    }

    protected int getInitialKarma(Player player) {
        int initialKarma = 0;
        int karmaToNext = 0;
        KarmaTrack track = getInitialTrack(player);
        for (KarmaGroup group : track) {
            if (player.hasPermission(group.getPermission())) {
                initialKarma = group.getKarmaPoints();
                if (track.getNextGroup(group) != null) {
                    karmaToNext = track.getNextGroup(group).getKarmaPoints()
                            - group.getKarmaPoints();
                } else {
                    // Highest rank in track only initialize with 2020
                    karmaToNext = 100;
                }
            } else {
                // If they don't have permission for the next group in the track, we're done here
                break;
            }
        }
        // Extra karma added on initial import
        if (initialKarma > 0 && config.getBoolean("import.bonus")) {
            double percent = config.getDouble("import.percent");
            if (percent < 0 || percent > 1) {
                throw new NullPointerException("import.percent must be a percentage (ie 0.25 for 25%)");
            }
            initialKarma += (int) (percent * karmaToNext);
        }
        return initialKarma;
    }

    protected KarmaTrack getInitialTrack(Player player) {
		// List of all the groups that this player belongs to
        // It only lists a group if it is the last group in a track they have
        // permission for
        List<KarmaGroup> groupList = new ArrayList<KarmaGroup>();
        for (KarmaTrack track : tracks) {
            KarmaGroup lastGroup = null;
            // Iterate the groups in the track
            for (KarmaGroup group : track) {
                String perm = group.getPermission();
                if (group.isFirstGroup(track) && !player.hasPermission(perm)) {
                    // If they do not have permission for the first group in
                    // this track, skip to next track to save time
                    break;
                }
                if (!player.hasPermission(perm) && lastGroup != null) {
                    // Store the last group as it is the last group in the track
                    // that they have permission for
                    groupList.add(lastGroup);
                } else if (player.hasPermission(perm) && track.getNextGroup(group) == null) {
                    // When you have reached the last group in the track and the
                    // player has permissions for it, add it
                    groupList.add(group);
                } else if (player.hasPermission(perm)) {
                    // If it is not the last group and the player has permission
                    // for it, set the last group as it for the first check
                    lastGroup = group;
                }
            }
        }
        Collections.sort(groupList);
        if (groupList.isEmpty()) {
            // If player is new, give them default track
            return getDefaultTrack();
        }
        // Get the group with the greatest amount of Karma points that this user
        // belongs to in any track and return the track it is in
        return groupList.get((groupList.size()-1)).getTrack(tracks);
    }

    public int getNextRandomKarmaPartyDelay() {
        // on average 20, between 10 min and 30 min
        int minutes = config.getInt("party.time.minimum")
                + random.nextInt(config.getInt("party.time.maximum")
                - config.getInt("party.time.minimum"));
        // 20 ticks/second, 60 seconds/min
        int ticks = minutes * 20 * 60;
        log.fine("Next karma party in " + minutes + " minutes or " + ticks + " ticks.");
        return ticks;
    }

    protected void loadTracks() {
        Set<String> stracks = config.getConfigurationSection("tracks").getKeys(false);
        tracks.clear();
        for (String strack : stracks) {
            KarmaTrack track = new KarmaTrack(strack);
            track.setGroups(loadKarmaGroups(track));
            tracks.add(track);
        }
    }

    protected List<KarmaGroup> loadKarmaGroups(KarmaTrack track) {
        Set<String> groups = config.getConfigurationSection("tracks." + track.getName()).getKeys(false);
        List<KarmaGroup> ret = new ArrayList<KarmaGroup>();
        for (String group : groups) {
            if ("default".equals(group) &&
                    config.getBoolean("tracks." + track.getName() + "." + group)) {
                track.setFirst(true);
                continue;
            }
            ret.add(new KarmaGroup(group,
                    config.getInt("tracks." + track.getName() + "." + group + ".points"),
                    ChatColor.getByChar(
                    config.getString("tracks." + track.getName() + "." + group + ".color"))));
        }
        return ret;
    }

    public KarmaTrack getDefaultTrack() {
        for (KarmaTrack track : tracks) {
            if (track.isFirst()) {
                return track;
            }
        }
        throw new NullPointerException("No default track");
    }

    public void runCommand(String command) {
        if (command.contains("\n")) {
            for (String c : command.split("\n")) {
                server.dispatchCommand(server.getConsoleSender(), c);
            }
            return;
        }
        server.dispatchCommand(server.getConsoleSender(), command);
    }

    public void msg(CommandSender destination, String message) {
        for (String s : this.processMessage(message)) {
            if (destination instanceof ConsoleCommandSender) {
                // Remove formatting from messages going to console
                destination.sendMessage(ChatColor.stripColor(s));
            } else {
                destination.sendMessage(s);
            }
        }
    }

    public void msg(List<CommandSender> destinations, String message) {
        for (CommandSender dest : destinations) {
            this.msg(dest, message);
        }
    }

    public void msg(CommandSender[] destinations, String message) {
        this.msg(Arrays.asList(destinations), message);
    }

    protected List<String> processMessage(String message) {
        if (message == null || "".equals(message)) {
            throw new NullPointerException("message cannot be null");
        }
        message = message.replaceAll("<NEWLINE>", "\n");
        List<String> ret = new ArrayList<String>();
        if (message.contains("\n")) {
            for (String s : message.split("\n")) {
                ret.add(parseColor(this.getString("PREFIX") + s));
            }
            return ret;
        }
        ret.add(parseColor(this.getString("PREFIX") + message));
        return ret;
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

    /**
     * Get a track by hash code
     * @param hash the tracks hash code
     * @return the track or null if not found
     */
    public KarmaTrack getTrack(long hash) {
        for(KarmaTrack track : tracks) {
            if(track.getName().hashCode() == hash) {
                return track;
            }
        }
        return null;
    }

    private String parseColor(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public Map<String, KarmaPlayer> getPlayers() {
        return players;
    }

    public KarmaPlayer getPlayer(String player) {
        if (players.containsKey(player)) {
            return players.get(player);
        } else {
            return db.get(player);
        }
    }

    public OfflinePlayer getBukkitPlayer(String player) {
        if (server.getPlayer(player) == null) {
            if (server.getOfflinePlayer(player).hasPlayedBefore()) {
                return server.getOfflinePlayer(player);
            } else {
                return null;
            }
        } else {
            return server.getPlayer(player);
        }
    }

    public Database getKarmaDatabase() {
        return db;
    }

    public String getString(String key) {
        Validate.notNull(key);
        Validate.notEmpty(key);
        return messages.getString(key).replace("''", "'");
    }

    public String getString(String key, Object[] args) {
        Validate.notNull(key);
        Validate.notEmpty(key);
        return MessageFormat.format(messages.getString(key), args);
    }

	public String parseNumber(long lng) {
        NumberFormat numberInstance = NumberFormat.getNumberInstance(Locale.getDefault());
        return numberInstance.format(lng);
    }
}
