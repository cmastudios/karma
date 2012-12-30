package com.tommytony.karma;

import org.bukkit.ChatColor;

public class KarmaGroup implements Comparable {

    private final String groupName;
    private final int karmaPoints;
    private final ChatColor chatColor;

    public KarmaGroup(String groupName, int karmaPoints, ChatColor color) {
        this.groupName = groupName;
        this.karmaPoints = karmaPoints;
        this.chatColor = color;
    }

    public int getKarmaPoints() {
        return karmaPoints;
    }

    public String getGroupName() {
        return groupName;
    }

    public ChatColor getChatColor() {
        return chatColor;
    }

    public int compareTo(Object o) {
        if (!(o instanceof KarmaGroup)) {
            throw new ClassCastException(this.getClass().getCanonicalName() + 
                    " is not comparable to a " + o.getClass().getCanonicalName()
                    );
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
