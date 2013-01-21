package com.tommytony.karma.commands;

import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaPlayer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KarmaBaseCommand implements CommandExecutor {

    private Karma karma;

    public KarmaBaseCommand(Karma instance) {
        karma = instance;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command cannot be used by console");
            return true;
        }
        KarmaPlayer karmaCheckPlayer = karma.players.get(((Player) sender).getName());
        if (karmaCheckPlayer != null) {
            karma.msg(sender, karma.config.getString("check.self.message")
                    .replace("<points>", Integer.toString(karmaCheckPlayer.getKarmaPoints()))
                    .replace("<curgroup>", karmaCheckPlayer.getGroup().getGroupName())
                    .replace("<nextgroup>", karmaCheckPlayer.getTrack().getNextGroup(karmaCheckPlayer.getGroup()) != null 
                        ? karmaCheckPlayer.getTrack().getNextGroup(karmaCheckPlayer.getGroup()).getGroupName()
                        : "none")
                    .replace("<curgroupcolor>", karmaCheckPlayer.getGroup().getChatColor().toString())
                    .replace("<nextgroupcolor>", karmaCheckPlayer.getTrack().getNextGroup(karmaCheckPlayer.getGroup()) != null 
                        ? karmaCheckPlayer.getTrack().getNextGroup(karmaCheckPlayer.getGroup()).getChatColor().toString()
                        : ChatColor.WHITE.toString()));
        }
        return true;
    }
}
