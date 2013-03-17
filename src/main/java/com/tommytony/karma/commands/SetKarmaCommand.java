package com.tommytony.karma.commands;

import com.google.common.collect.ImmutableList;
import com.tommytony.karma.Karma;
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

public class SetKarmaCommand implements CommandExecutor, TabCompleter {

    private Karma karma;

    public SetKarmaCommand(Karma instance) {
        karma = instance;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("karma.set")) {
            karma.msg(sender, karma.config.getString("errors.nopermission"));
            return true;
        }
        if(args.length != 3) {
            karma.msg(sender, karma.config.getString("errors.badargs"));
            return false;
        }
        OfflinePlayer setKarmaTarget = karma.getBukkitPlayer(args[1]);
        if (setKarmaTarget == null) {
            karma.msg(sender, karma.config.getString("errors.noplayer"));
            return true;
        }
        KarmaPlayer setKarmaPlayer = karma.getPlayer(setKarmaTarget.getName());
        if (setKarmaPlayer == null) {
            karma.msg(sender, karma.config.getString("errors.noplayer"));
        }
        int karmaToSet;
        try {
            karmaToSet = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("The amount of points to set must be an integer.");
        }
        if (karmaToSet < 0) {
            throw new IllegalArgumentException("The amount of karma points to set must be greather than zero.");
        }
        if (karmaToSet == setKarmaPlayer.getKarmaPoints()) {
            throw new IllegalArgumentException("The amount of karma points to set must be different than the player's current karma points.");
        }
        karma.msg(sender, "Karma of " + setKarmaPlayer.getName() + " set to " + karmaToSet + ".");
        karma.log.info(setKarmaPlayer.getName() + "'s karma points set to " + karmaToSet + " from " + setKarmaPlayer.getKarmaPoints() + ".");
        setKarmaPlayer.setKarmaPoints(karmaToSet);
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
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
