package com.tommytony.karma.commands;

import com.google.common.collect.ImmutableList;
import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaGroup;
import com.tommytony.karma.KarmaPlayer;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class PromoteCommand implements CommandExecutor, TabCompleter {

    private Karma karma;

    public PromoteCommand(Karma instance) {
        karma = instance;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        if(args.length != 2) {
            karma.msg(sender, karma.config.getString("errors.badargs"));
            return false;
        }

        OfflinePlayer promoteTarget = karma.getBukkitPlayer(args[1]);
        if (promoteTarget == null) {
            karma.msg(sender, karma.config.getString("errors.noplayer"));
            return true;
        }
        KarmaPlayer karmaPromoteTarget = karma.getPlayer(promoteTarget.getName());
        if (karmaPromoteTarget == null) {
            karma.msg(sender, karma.config.getString("errors.noplayer"));
            return true;
        }
        KarmaGroup currentGroup = karmaPromoteTarget.getGroup();
        KarmaGroup nextGroup = karmaPromoteTarget.getTrack().getNextGroup(currentGroup);
        if (nextGroup == null) {
            karma.msg(sender, karma.config.getString("promocommand.messages.highest"));
            return true;
        }
        int difference = nextGroup.getKarmaPoints() - karmaPromoteTarget.getKarmaPoints();

        if (sender.hasPermission("karma.promote." + nextGroup.getGroupName())
                || sender.hasPermission("karma.promote.*")) {
            karmaPromoteTarget.addKarma(difference);
            if (promoteTarget.isOnline()) {
                if (karmaPromoteTarget.getGroupByPermissions() != nextGroup) {
                    throw new NullPointerException("Attempted to promote player " + karmaPromoteTarget.getName() + " to group " + nextGroup.getGroupName()
                            + ", but after promotion, the player doesn't have " + nextGroup.getPermission() + "! Promote command configured incorrectly.");
                }
            }
            karma.msg(sender, karma.config.getString("promocommand.messages.promoted")
                    .replace("<player>", promoteTarget.getName())
                    .replace("<group>", nextGroup.getGroupName()));
            return true;
        } else {
            karma.msg(sender, karma.config.getString("errors.nopermission"));
            return true;
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 2) {
            List<String> players = new ArrayList();
            for (Player player : karma.server.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return StringUtil.copyPartialMatches(args[1], players, new ArrayList<String>(players.size()));
        }
        return ImmutableList.of();
    }
}
