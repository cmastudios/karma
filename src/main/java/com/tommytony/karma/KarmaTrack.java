package com.tommytony.karma;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * List of groups that a player can be promoted/demoted between.
 */
public class KarmaTrack implements Iterable<KarmaGroup> {
    // Ordered least to greatest karma point value
    private String name;
    private List<KarmaGroup> groups;
    private boolean first;
    public KarmaTrack(String name) {
        this.groups = new ArrayList<KarmaGroup>();
        this.name = name;
        this.first = false;
    }
    /**
     * Set all groups in the track.
     * @param groups the groups to change to.
     */
    protected void setGroups(List<KarmaGroup> groups) {
        // Sort the groups by karma point value
        // Thank you Comparable!
        Collections.sort(groups);
        this.groups = groups;
    }
    /**
     * Get a group by name.
     * @param name the group's name.
     * @return the group or null if it can't be found.
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
     * Get a group by karma point value.
     * @param karma the amount of karma the group requires.
     * @return the group or null if it can't be found.
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
     * Get a group in the track based on a player's karma.
     * @param karma the amount of karma that falls in the group.
     * @return the group in bounds or null if one can't be found.
     */
    public KarmaGroup getGroupOnBounds(int karma) {
        for (KarmaGroup group : groups) {
            KarmaGroup nextGroup = this.getNextGroup(group);
            if ((group.getKarmaPoints() <= karma) && nextGroup == null) {
                return group;
            } else if ((group.getKarmaPoints() <= karma)
                    && (karma < nextGroup.getKarmaPoints())) {
                return group;
            }
        }
        return null;
    }
    /**
     * Get the group with more karma than the specified group.
     * @param previousGroup the group with less karma then the next.
     * @return the group or null if it can't be found.
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
     * Get the group with less karma than the specified group
     * @param nextGroup the group with more karma then the next
     * @return the group or null if it can't be found
     */
    public KarmaGroup getPreviousGroup(KarmaGroup nextGroup) {
        ListIterator<KarmaGroup> li = groups.listIterator(groups.size());
        while (li.hasPrevious()) {
            KarmaGroup previousGroup = li.previous();
            if (previousGroup.getKarmaPoints() < nextGroup.getKarmaPoints()) {
                return previousGroup;
            }
        }
        return null;
    }

    /**
     * Get the name of the track.
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Check if this track is the first/default track.
     * @return true if this track is first, false otherwise.
     */
    public boolean isFirst() {
        return first;
    }

    /**
     * Set this track as the first/default track or unset it.
     * @param first true if this is the first track, false if not.
     */
    public void setFirst(boolean first) {
        this.first = first;
    }

    public Iterator<KarmaGroup> iterator() {
        return groups.iterator();
    }
    public ListIterator<KarmaGroup> reverseListIterator() {
        return groups.listIterator(groups.size());
    }

}
