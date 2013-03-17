package com.tommytony.karma;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import org.bukkit.OfflinePlayer;

/**
 * The base player class for use in karma.
 */
public class KarmaPlayer {

    private final Karma karma;
    private final String name;
    private int karmaPoints;
    private long lastActivityTime;
    private long lastGift;
    private KarmaTrack track;

    public KarmaPlayer(Karma karma, String name, int karmaPoints, long lastActivityTime, long lastGift, KarmaTrack track) {
        this.karma = karma;
        this.name = name;
        this.karmaPoints = karmaPoints;
        this.lastActivityTime = lastActivityTime;
        this.lastGift = lastGift;
        this.track = track;
    }

    /**
     * Adds a defined amount of karma points to a player.
     * This method will check if a player needs a promotion.
     * @param pointsToAdd Amount of karma points to add
     */
    public void addKarma(int pointsToAdd) {
        if (pointsToAdd > 0) {
            int before = this.karmaPoints;
            this.karmaPoints += pointsToAdd;
//            this.karma.checkForPromotion(this, before, this.karmaPoints);
            this.updatePermissions(before, this.karmaPoints);
            this.karma.getKarmaDatabase().put(this);
        }
    }

    /**
     * Removes a defined amount of karma points from a player.
     * This method will check if a player needs a demotion.
     * @param pointsToRemove Amount of karma points to remove
     */
    public void removeKarma(int pointsToRemove) {
        this.removeKarma(pointsToRemove, false);
    }

    /**
     * Removes a defined amount of karma points from a player.
     * This method will check if a player needs a demotion.
     * This method will not demote the player to their track's start group.
     * @param pointsToRemove Amount of karma points to remove
     */
    public void removeKarmaAutomatic(int pointsToRemove) {
        this.removeKarma(pointsToRemove, true);
    }

    private void removeKarma(int pointsToRemove, boolean automatic) {
        if (pointsToRemove > this.karmaPoints) {
            pointsToRemove = this.karmaPoints;
        }
        if (pointsToRemove > 0) {
            int before = this.karmaPoints;
            this.karmaPoints -= pointsToRemove;
            // Prevent losing more karma than the start group in the player's track
            if (this.karmaPoints < this.track.getFirstGroup().getKarmaPoints()) {
                this.karmaPoints = this.track.getFirstGroup().getKarmaPoints();
            }
//            this.karma.checkForDemotion(this, before, this.karmaPoints, automatic);
            this.updatePermissions(before, this.karmaPoints);
            this.karma.getKarmaDatabase().put(this);
        }
    }

    /**
     * Set a player's karma point value directly.
     * @param amount the amount of karma points.
     * @throws IllegalArgumentException amount to set is less than zero.
     */
    public void setKarmaPoints(int amount) {
        if (amount != this.getKarmaPoints()) {
            if (amount >= 0) {
                int before = this.getKarmaPoints();
                if (amount > this.getKarmaPoints()) {
                    this.addKarma(amount - this.getKarmaPoints());
                } else {
                    this.removeKarma(this.getKarmaPoints() - amount);
                }
            } else {
                throw new IllegalArgumentException("amount cannot be a negative number.");
            }
        }
    }

    /**
     * Get the player's name
     * @return the player's name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets how many karma points the player currently has.
     * @return the amount of karma points
     */
    public int getKarmaPoints() {
        return karmaPoints;
    }

    /**
     * Gets the last time the player did an action which made the player un-AFK.
     * @return the last action time in milliseconds since the Unix epoch
     */
    public long getLastActivityTime() {
        return lastActivityTime;
    }

    /**
     * Sets a player's last activity time to the current time.
     */
    public void ping() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    /**
     * Sets a player's last gift time to the current time.
     */
    public void updateLastGiftTime() {
        this.lastGift = System.currentTimeMillis();
    }

    /**
     * Gets the last time the player gifted a karma point to another player.
     * @return the last action time in milliseconds since the Unix epoch
     */
    public long getLastGiftTime() {
        return lastGift;
    }

    /**
     * Checks if a player can gift based on their last gift time.
     * This will not check if the player has the <code>karma.gift</code>
     * permission.
     * @return true if the player can gift, false otherwise
     */
    public boolean canGift() {
        if (karma.config.getInt("gift.cooldown", 60) == 0) {
            return true;
        }
        long since = System.currentTimeMillis() - getLastGiftTime();
        return since > (60 * karma.config.getInt("gift.cooldown", 60)) * 1000;
    }

    /**
     * Gets the player's current track.
     * @return the track
     */
    public KarmaTrack getTrack() {
        return track;
    }

    /**
     * Set the player's current track.
     * Warning: if this track cannot be found from the config file during a
     * reload, then it will break.
     * @param track the track to set
     */
    public void setTrack(KarmaTrack track) {
        this.track = track;
        if (this.getKarmaPoints() < track.getFirstGroup().getKarmaPoints()) {
            this.addKarma(track.getFirstGroup().getKarmaPoints() - this.getKarmaPoints());
        }
        this.karma.getKarmaDatabase().put(this);
    }

    /**
     * Gets the group that the player is in by their current permissions.
     * @return the group
     * @throws NullPointerException if permissions do not match karma
     */
    public KarmaGroup getGroupByPermissions() {
        for (KarmaGroup currentGroup : getTrack()) {
            if (getKarmaPoints() >= currentGroup.getKarmaPoints()) {
                String perm = "karma." + currentGroup.getGroupName();
                if (getPlayer().isOnline() &&
                        !getPlayer().getPlayer().hasPermission(perm) && currentGroup == getTrack().getFirstGroup()) {
                    throw new NullPointerException(getName() + " does not have permissions for the start group! "
                            + "Permissions configured incorrectly (Did you forget inheritance?).");
                }
                if (getPlayer().isOnline() &&
                        !getPlayer().getPlayer().hasPermission(perm)) {
                    // If player is online and has the karma, but lacks the permission, throw an error
                    throw new NullPointerException(getName() + " has the karma for rank "
                            + currentGroup.getGroupName() + " yet lacks the permission for it! "
                            + "Permissions configured incorrectly (Did you forget inheritance?)");
                }
                if ((getTrack().getNextGroup(currentGroup) != null
                        && getKarmaPoints() < getTrack().getNextGroup(currentGroup).getKarmaPoints())
                        || (getTrack().getNextGroup(currentGroup) == null)) {
                    return currentGroup;
                }
            }
        }
        return null;
    }

    /**
     * Updates permissions for a promote/demote on the same track.
     * This does not handle track switches.
     * @param before karma points before change.
     * @param after karma points after change.
     * @see #setGroup(com.tommytony.karma.KarmaGroup) Use for track changes.
     */
    protected void updatePermissions(int before, int after) {
        KarmaGroup oldGroup = this.track.getGroupOnBounds(before);
        KarmaGroup newGroup = this.track.getGroupOnBounds(after);
        if (oldGroup == null || newGroup == null) {
            throw new NullPointerException("Could not find player's group after karma change.");
        }
        if (oldGroup == newGroup) {
            return;
        }
        switch (oldGroup.compareTo(newGroup)) {
            case -1:
                karma.runCommand(karma.config.getString("promotion.command")
                        .replace("<player>", name)
                        .replace("<group>", newGroup.getGroupName()));
                if (this.getPlayer().isOnline()) {
                    if (this.getGroupByPermissions() != newGroup) {
                        throw new NullPointerException("Attempted to promote player " + this.name + " to group " + newGroup.getGroupName()
                                + ", but after promotion, the player doesn't have " + newGroup.getPermission() + "! Promote command configured incorrectly.");
                    }
                }
                karma.msg(this.karma.server.getOnlinePlayers(), this.karma.config.getString("promotion.message")
                        .replace("<player>", name)
                        .replace("<group>", newGroup.getGroupName())
                        .replace("<groupcolor>", newGroup.getChatColor().toString()));
                karma.log.info(name + " promoted to " + newGroup.getGroupName());
                break;
            case 1:
                if (newGroup.isFirstGroup(this.track) && this.track.isFirst()
                        && !karma.config.getBoolean("demotion.demotetofirstgroup")) {
                    break; // Prevents players from being demoted to the first group automatically
                }
                karma.runCommand(karma.config.getString("demotion.command")
                        .replace("<player>", name)
                        .replace("<group>", newGroup.getGroupName()));
                if (this.getPlayer().isOnline()) {
                    if (this.getGroupByPermissions() != newGroup) {
                        throw new NullPointerException("Attempted to demote player " + this.name + " to group " + newGroup.getGroupName()
                                + ", but after demotion, the player isn't in this group! Demote command configured incorrectly.");
                    }
                }
                karma.msg(this.karma.server.getOnlinePlayers(), this.karma.config.getString("demotion.message")
                        .replace("<player>", name)
                        .replace("<group>", newGroup.getGroupName())
                        .replace("<groupcolor>", newGroup.getChatColor().toString()));
                karma.log.info(name + " demoted to " + newGroup.getGroupName());
                break;
            default:
                break;
        }
    }

    /**
     * Gets the group that the player is in by their amount of karma points.
     * @return the group
     */
    public KarmaGroup getGroup() {
        return getTrack().getGroupOnBounds(karmaPoints);
    }

    /**
     * Set a player to a group. Use for track switching only.
     * @param group the group to change the player to
     */
    public void setGroup(KarmaGroup group) {
        karma.runCommand(karma.config.getString("promotion.command").replace("<player>", getName()).replace("<group>", group.getGroupName()));
    }

    /**
     * Get the Bukkit player.
     * @return the player
     */
    public OfflinePlayer getPlayer() {
        return karma.server.getOfflinePlayer(getName());
    }

    /**
     * Check if a player is AFK.
     * AFK players do not receive karma in a karma party.
     * @return true if the player is AFK, false otherwise
     */
    public boolean isAfk() {
        long activeInterval = System.currentTimeMillis() - this.getLastActivityTime();
        int minutesAfk = (int) Math.floor(activeInterval / (1000 * 60));
        return minutesAfk >= karma.config.getInt("afk.time", 10);
    }
    /**
     * Check if a player is playing in a warzone.
     * @return true if player is playing war, false otherwise
     */
    public boolean isPlayingWar() {
        if (!karma.warEnabled) {
            throw new NullPointerException("The war plugin is not enabled");
        }
        if (Warzone.getZoneByPlayerName(name) != null) {
            return true;
        } else {
            return false;
        }
    }
    /**
     * Check if the player has made a warzone which people are currently playing
     * in.
     * Notice: this will only return true if players are playing in this
     * player's warzone actively.
     * @return true if player has a warzone, false otherwise
     */
    public boolean hasActiveWarzone() {
        if (!karma.warEnabled) {
            throw new NullPointerException("The war plugin is not enabled");
        }
        for (Warzone zone : War.war.getWarzones()) {
            for (String author : zone.getAuthors()) {
                if (author.equals(name) && zone.isEnoughPlayers() && this.getPlayersInWarzone(zone) > 0) {
                    return true;
                }
            }
        }
        return false;
    }
    private int getPlayersInWarzone(Warzone zone) {
        int players = 0;
        for (Team team : zone.getTeams()) {
            players += team.getPlayers().size();
        }
        return players;
    }
}
