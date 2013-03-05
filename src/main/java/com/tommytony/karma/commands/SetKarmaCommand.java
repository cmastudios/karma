package com.tommytony.karma.commands;

import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaPlayer;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SetKarmaCommand implements CommandExecutor {

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
}
