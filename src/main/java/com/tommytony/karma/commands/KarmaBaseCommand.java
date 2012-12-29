package com.tommytony.karma.commands;

import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaPlayer;
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
            karma.msg(
                    sender,
                    karma.config.getString("check.self.message").replace(
                    "<points>",
                    karmaCheckPlayer.getKarmaPoints()
                    + "").replace(
                    "<curgroup>",
                    karma.getPlayerGroupString(karmaCheckPlayer)).replace(
                    "<nextgroup>",
                    karma.getPlayerNextGroupString(karmaCheckPlayer)).replace(
                    "<curgroupcolor>",
                    karma.getPlayerGroupColor(karmaCheckPlayer).toString()).replace(
                    "<nextgroupcolor>",
                    karma.getPlayerNextGroupColor(
                    karmaCheckPlayer).toString()));
        }
        return true;
    }
}
