package com.tommytony.karma.commands;

import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CheckKarmaCommand implements CommandExecutor {

    private Karma karma;

    public CheckKarmaCommand(Karma instance) {
        karma = instance;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        Player checkOtherTarget = karma.server.getPlayer(args[0]);
        if (checkOtherTarget == null) {
            karma.msg(sender, karma.config.getString("errors.noplayer"));
            return true;
        }
        KarmaPlayer karmaCheckOtherTarget = karma.players.get(checkOtherTarget.getName());
        if (karmaCheckOtherTarget != null) {
            karma.msg(
                    sender,
                    karma.config.getString("check.others.message")
                    .replace("<player>",checkOtherTarget.getName())
                    .replace("<points>", Integer.toString(karmaCheckOtherTarget.getKarmaPoints()))
                    .replace( "<curgroupcolor>", karma.getPlayerGroupColor(karmaCheckOtherTarget).toString()));
        } else {
            karma.msg(sender, karma.config.getString("errors.noplayer"));
        }
        return true;
    }
}