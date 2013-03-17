package com.tommytony.karma.commands;

import com.google.common.collect.ImmutableList;
import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaPlayer;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class AddKarmaCommand implements CommandExecutor, TabCompleter {

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
        OfflinePlayer addKarmaTarget = karma.getBukkitPlayer(args[1]);
        if (addKarmaTarget == null) {
            karma.msg(sender, karma.config.getString("errors.noplayer"));
            return true;
        }
        KarmaPlayer addKarmaPlayer = karma.getPlayer(addKarmaTarget.getName());
        if (addKarmaPlayer == null) {
            karma.msg(sender, karma.config.getString("errors.noplayer"));
            return true;
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
