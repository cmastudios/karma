package com.tommytony.karma.commands;

import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaGroup;
import com.tommytony.karma.KarmaPlayer;
import java.util.List;
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
        if(args.length != 3) {
            karma.msg(sender, karma.config.getString("errors.badargs"));
            return false;
        }

        try {
            Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED
                    + "The third argument must be an integer!");
            return true;
        }
        List<Player> matches3 = karma.server.matchPlayer(
                args[1]);
        if (!matches3.isEmpty() && Integer.parseInt(args[2]) >= 0
                && sender.hasPermission("karma.set")) {
            KarmaPlayer playerToAddKarma = karma.players.get(matches3.get(0).getName());
            karma.msg(sender, "Karma of " + matches3.get(0) + " changed");
            karma.setAmount(matches3,
                    Integer.parseInt(args[2]) + playerToAddKarma.getKarmaPoints());
            return true;
        }
        return true;

    }
}
