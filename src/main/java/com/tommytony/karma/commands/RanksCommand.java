package com.tommytony.karma.commands;

import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaGroup;
import com.tommytony.karma.KarmaTrack;
import org.apache.commons.lang.Validate;
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
        Validate.isTrue(args.length == 1, karma.getString("ERROR.ARGS", new Object[] {"/karma ranks"}));
        for (KarmaTrack track : karma.tracks) {
            StringBuilder ranksString = new StringBuilder(track.getName()).append(": ");
            for (KarmaGroup group : track) {
                ranksString.append(group.getFormattedName())
                        .append(ChatColor.GRAY).append("(")
                        .append(ChatColor.YELLOW).append(karma.parseNumber(group.getKarmaPoints()))
                        .append(ChatColor.GRAY).append(")");
                if (track.getNextGroup(group) != null) {
                    ranksString.append(ChatColor.WHITE).append(" -> ")
                            .append(ChatColor.GRAY);
                }
            }
            karma.msg(sender, ranksString.toString());
        }
        return true;
    }
}
