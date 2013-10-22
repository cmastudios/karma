package com.tommytony.karma.commands;

import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaPlayer;
import org.apache.commons.lang.Validate;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CheckKarmaCommand implements CommandExecutor {

    private Karma karma;

    public CheckKarmaCommand(Karma instance) {
        karma = instance;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        OfflinePlayer playerTarget = karma.getBukkitPlayer(args[0]);
        Validate.notNull(playerTarget, karma.getString("ERROR.PLAYER404", new Object[] {args[0]}));
        Validate.isTrue(playerTarget.hasPlayedBefore(), karma.getString("ERROR.PLAYER404", new Object[] {args[0]}));
        KarmaPlayer target = karma.getPlayer(playerTarget.getName());
        if (target != null) {
            karma.msg(sender, karma.getString("CHECK.OTHERS", new Object[] {
                target.getGroup().getChatColor() + target.getName(),
                target.getKarmaPoints()}));
        } else {
            karma.msg(sender, karma.getString("ERROR.PLAYER404.NOKP", new Object[] {playerTarget.getName()}));
        }
        return true;
    }
}
