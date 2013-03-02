package com.tommytony.karma;

import java.util.List;
import org.bukkit.ChatColor;

/**
 * A group that may be obtained by players with their karma points.
 * All groups are attached to a track.
 */
public class KarmaGroup implements Comparable {

    private final String groupName;
    private final int karmaPoints;
    private final ChatColor chatColor;

    public KarmaGroup(String groupName, int karmaPoints, ChatColor color) {
        this.groupName = groupName;
        this.karmaPoints = karmaPoints;
        this.chatColor = color;
    }

    /**
     * Get the group's karma point level.
     * This is the minimum amount of karma that a player needs to be in the group.
     * @return the point level
     */
    public int getKarmaPoints() {
        return karmaPoints;
    }

    /**
     * Get the group's name.
     * @return the group name
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Get the group's associated color.
     * This should be the same as their color in chat which is defined by the
     * server's plugin setup.
     * @return the group color
     */
    public ChatColor getChatColor() {
        return chatColor;
    }

    /**
     * Get the track that this group is attached to.
     * @param tracks A list of all the available tracks.
     * @return the group's track
     * @see KarmaAPI#getTracks()
     */
    public KarmaTrack getTrack(List<KarmaTrack> tracks) {
        for (KarmaTrack track : tracks) {
            if (track.getGroup(getGroupName()) != null) {
                return track;
            }
        }
        return null;
    }

    /**
     * Get the permission level required for this group.
     * This is the level the player needs to be in the group. The player should
     * inherit the permissions of groups lower in the track. Karma points
     * instead should be used to determine if a player is in a group.
     * @return the group permission
     * @see KarmaTrack#getGroupOnBounds(int karma)
     */
    public String getPermission() {
        return "karma." + getGroupName();
    }

    /**
     * Check if this group is the first group in the specified track.
     * @param track the track this group is in.
     * @return true if this group is the first group in the track, false otherwise.
     */
    public boolean isFirstGroup(KarmaTrack track) {
        return track.getFirstGroup() == this ? true : false;
    }

    /**
     * Check relationship with another group based on karma points.
     * @param o the other group.
     * @return -1 if this group has less karma points than the second, 0 if the
     * two groups have equal karma points, or 1 if this group has more karma
     * points than the other group.
     */
    public int compareTo(Object o) {
        if (!(o instanceof KarmaGroup)) {
            throw new ClassCastException(this.getClass().getCanonicalName()
                    + " is not comparable to a " + o.getClass().getCanonicalName());
        }
        KarmaGroup group2 = (KarmaGroup) o;
        if (this.getKarmaPoints() < group2.getKarmaPoints()) {
            return -1;
        }
        if (this.getKarmaPoints() == group2.getKarmaPoints()) {
            return 0;
        }
        if (this.getKarmaPoints() > group2.getKarmaPoints()) {
            return 1;
        }
        return 0;
    }
}
