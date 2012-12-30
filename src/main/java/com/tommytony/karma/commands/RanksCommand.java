package com.tommytony.karma.commands;

import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaGroup;
import com.tommytony.karma.KarmaTrack;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RanksCommand implements CommandExecutor {

    private Karma karma;

    public RanksCommand(Karma instance) {
        karma = instance;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        for (KarmaTrack track : karma.tracks) {
            KarmaGroup group = track.getFirstGroup();
            StringBuilder ranksString = new StringBuilder();
            ranksString.append(track.getName()).append(": ");
            while (group != null) {
                ranksString.append(group.getChatColor()).append(group.getGroupName())
                        .append(ChatColor.GRAY).append("(")
                        .append(ChatColor.YELLOW).append(group.getKarmaPoints())
                        .append(ChatColor.GRAY).append(")");
                if (track.getNextGroup(group) != null) {
                    ranksString.append(ChatColor.WHITE).append(" -> ")
                            .append(ChatColor.GRAY);
                }
                group = track.getNextGroup(group);
            }
            karma.msg(sender, ranksString.toString());
        }
        return true;
    }
}
