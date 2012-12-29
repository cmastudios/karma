package com.tommytony.karma.commands;

import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaGroup;
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
        String ranksString = karma.config.getString("viewranks.prefix");
        KarmaGroup group = karma.startGroup;
        while (group != null) {
            ranksString += group.getChatColor() + group.getGroupName() + ChatColor.GRAY + "("
                    + ChatColor.YELLOW + group.getKarmaPoints()
                    + ChatColor.GRAY + ")";
            if (group.getNext() != null) {
                ranksString += ChatColor.WHITE + " -> "
                        + ChatColor.GRAY;
            }
            group = group.getNext();
        }
        karma.msg(sender, ranksString);
        return true;
    }
}
