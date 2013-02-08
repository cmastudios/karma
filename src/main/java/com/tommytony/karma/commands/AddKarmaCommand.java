package com.tommytony.karma.commands;

import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaPlayer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AddKarmaCommand implements CommandExecutor {

    private Karma karma;

    public AddKarmaCommand(Karma instance) {
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
        Player addKarmaTarget = karma.server.getPlayer(args[1]);
        if (addKarmaTarget == null) {
            karma.msg(sender, karma.config.getString("errors.noplayer"));
            return true;
        }
        KarmaPlayer addKarmaPlayer = karma.players.get(addKarmaTarget.getName());
        if (addKarmaPlayer == null) {
            throw new NullPointerException(addKarmaTarget.getName() + " is not a Karma player!");
        }
        int karmaToAdd;
        try {
            karmaToAdd = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "The third argument must be an integer!");
            return true;
        }
        karma.msg(sender, "Karma of " + addKarmaPlayer.getName() + " changed");
        addKarmaPlayer.addKarma(karmaToAdd);
        return true;
    }
}
