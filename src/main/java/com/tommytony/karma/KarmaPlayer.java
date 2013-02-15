package com.tommytony.karma;

import org.bukkit.OfflinePlayer;

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

    public void addKarma(int pointsToAdd) {
        if (pointsToAdd > 0) {
            int before = this.karmaPoints;
            this.karmaPoints += pointsToAdd;
            this.karma.checkForPromotion(this, before, this.karmaPoints);
            this.karma.getKarmaDatabase().put(this);
        }
    }

    public void removeKarma(int pointsToRemove) {
        this.removeKarma(pointsToRemove, false);
    }

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
            this.karma.checkForDemotion(this, before, this.karmaPoints, automatic);
            this.karma.getKarmaDatabase().put(this);
        }
    }

    public String getName() {
        return name;
    }

    public int getKarmaPoints() {
        return karmaPoints;
    }

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public void ping() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    public void updateLastGiftTime() {
        this.lastGift = System.currentTimeMillis();
    }

    public long getLastGiftTime() {
        return lastGift;
    }

    public boolean canGift() {
        long since = System.currentTimeMillis() - getLastGiftTime();
        return since > 3600 * 1000;
    }

    /**
     * @return the track
     */
    public KarmaTrack getTrack() {
        return track;
    }

    /**
     * @param track the track to set
     */
    public void setTrack(KarmaTrack track) {
        this.track = track;
        if (this.getKarmaPoints() < track.getFirstGroup().getKarmaPoints()) {
            this.addKarma(track.getFirstGroup().getKarmaPoints() - this.getKarmaPoints());
        }
    }
    /**
     * Gets the group that the player is in, based off of permissions
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
     * Gets the group that the player is in, based off of karma points
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
    public OfflinePlayer getPlayer() {
        return karma.server.getOfflinePlayer(getName());
    }
}
