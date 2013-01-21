package com.tommytony.karma.commands;

import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaPlayer;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetKarmaCommand implements CommandExecutor {

    private Karma karma;

    public SetKarmaCommand(Karma instance) {
        karma = instance;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        if(args.length != 3) {
            karma.msg(sender, karma.config.getString("errors.badargs"));
            return false;
        }
        Player setKarmaTarget = karma.server.getPlayer(args[1]);
        if (setKarmaTarget == null) {
            karma.msg(sender, karma.config.getString("errors.noplayer"));
            return true;
        }
        KarmaPlayer setKarmaPlayer = karma.players.get(setKarmaTarget.getName());
        if (setKarmaPlayer == null) {
            throw new NullPointerException(setKarmaTarget.getName() + " is not a Karma player!");
        }
        int karmaToSet;
        try {
            karmaToSet = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "The third argument must be an integer!");
            return true;
        }
        karma.msg(sender, "Karma of " + setKarmaPlayer.getName() + " changed");
        karma.setAmount(setKarmaTarget, karmaToSet);
        return true;
    }
}
