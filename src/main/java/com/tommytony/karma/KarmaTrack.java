package com.tommytony.karma;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KarmaTrack {
    // Ordered least to greatest karma point value
    private String name;
    private List<KarmaGroup> groups;
    public KarmaTrack(String name) {
        groups = new ArrayList<KarmaGroup>();
        this.name = name;
    }
    /**
     * Set all groups in the track
     * @param groups the groups to change to
     */
    protected void setGroups(List<KarmaGroup> groups) {
        // Sort the groups by karma point value
        // Thank you Comparable!
        Collections.sort(groups);
        this.groups = groups;
    }
    /**
     * Get a group by name
     * @param name the group's name
     * @return the group or null if it can't be found
     */
    public KarmaGroup getGroup(String name) {
        for (KarmaGroup group : groups) {
            if (group.getGroupName().equals(name)) {
                return group;
            }
        }
        return null;
    }
    public KarmaGroup getFirstGroup() {
        return groups.get(0);
    }
    /**
     * Get a group by karma point value
     * @param karma the amount of karma the group requires
     * @return the group or null if it can't be found
     */
    public KarmaGroup getGroup(int karma) {
        for (KarmaGroup group : groups) {
            if (group.getKarmaPoints() == karma) {
                return group;
            }
        }
        return null;
    }
    /**
     * Get the group with more karma than the specified group
     * @param previousGroup the group with less karma then the next
     * @return the group or null if it can't be found
     */
    public KarmaGroup getNextGroup(KarmaGroup previousGroup) {
        for (KarmaGroup nextGroup : groups) {
            if (nextGroup.getKarmaPoints() > previousGroup.getKarmaPoints()) {
                return nextGroup;
            }
        }
        return null;
    }

    /**
     * Get the name of the track
     * @return the name
     */
    public String getName() {
        return name;
    }

}
