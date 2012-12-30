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
        if (args.length < 2) {
            karma.msg(sender, karma.config.getString("errors.badargs"));
            return true;
        }
        Player promoteTarget = karma.server.getPlayer(args[1]);
        KarmaPlayer karmaPromoteTarget = karma.players.get(promoteTarget.getName());
        if (karmaPromoteTarget == null) {
            return true;
        }

        KarmaGroup currentGroup = karmaPromoteTarget.getTrack().getFirstGroup();
        if (promoteTarget == null) {
            karma.msg(sender, karma.config.getString("promote.messages.noplayer"));
            return true;
        }
        while (currentGroup != null) {
            if (karmaPromoteTarget.getKarmaPoints() < currentGroup.getKarmaPoints()) {
                if (sender.hasPermission("karma.promote." + currentGroup.getGroupName())) {
                    karmaPromoteTarget.addKarma(currentGroup.getKarmaPoints() - karmaPromoteTarget.getKarmaPoints());
                    karma.msg(
                            promoteTarget,
                            karma.config.getString("promocommand.messages.promoted")
                            .replace("<player>", promoteTarget.getName())
                            .replace("<group>", currentGroup.getGroupName()));
                    return true;
                } else {
                    karma.msg(sender, karma.config.getString("errors.nopermission"));
                    return true;
                }

            }
            currentGroup = karmaPromoteTarget.getTrack().getNextGroup(currentGroup);
        }
        return false;
    }
}
