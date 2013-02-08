package com.tommytony.karma.commands;

import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaGroup;
import com.tommytony.karma.KarmaPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PromoteCommand implements CommandExecutor {

    private Karma karma;

    public PromoteCommand(Karma instance) {
        karma = instance;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        if(args.length != 2) {
            karma.msg(sender, karma.config.getString("errors.badargs"));
            return false;
        }
        
        Player promoteTarget = karma.server.getPlayer(args[1]);
        if (promoteTarget == null) {
            karma.msg(sender, karma.config.getString("errors.noplayer"));
            return true;
        }
        KarmaPlayer karmaPromoteTarget = karma.players.get(promoteTarget.getName());
        if (karmaPromoteTarget == null) {
            return true;
        }

        for (KarmaGroup currentGroup : karmaPromoteTarget.getTrack()) {
            if (karmaPromoteTarget.getKarmaPoints() < currentGroup.getKarmaPoints()) {
                if (sender.hasPermission("karma.promote." + currentGroup.getGroupName())
                        || sender.hasPermission("karma.promote.*")) {
                    karmaPromoteTarget.addKarma(currentGroup.getKarmaPoints() - karmaPromoteTarget.getKarmaPoints());
                    if (karmaPromoteTarget.getGroupByPermissions() != currentGroup) {
                        throw new NullPointerException("Attempted to promote player " + karmaPromoteTarget.getName() + " to group " + currentGroup.getGroupName() 
                                + ", but after promotion, the player doesn't have " + currentGroup.getPermission() + "! Promote command configured incorrectly.");
                    }
                    karma.msg(
                            promoteTarget, karma.config.getString("promocommand.messages.promoted")
                            .replace("<player>", promoteTarget.getName())
                            .replace("<group>", currentGroup.getGroupName()));
                    return true;
                } else {
                    karma.msg(sender, karma.config.getString("errors.nopermission"));
                    return true;
                }

            }
        }
        return false;
    }
}
