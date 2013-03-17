package com.tommytony.karma.commands;

import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaGroup;
import com.tommytony.karma.KarmaPlayer;
import org.apache.commons.lang.Validate;
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
        Validate.isTrue(sender instanceof Player, karma.getString("ERROR.PLAYER404", new Object[] {sender.getName()}));
        KarmaPlayer target = karma.getPlayer(sender.getName());
        Validate.notNull(target, karma.getString("ERROR.PLAYER404.NOKP", new Object[] {sender.getName()}));
        karma.msg(sender, karma.getString("CHECK.SELF.POINTS", new Object[] {target.getKarmaPoints()}));
        KarmaGroup nextGroup = target.getTrack().getNextGroup(target.getGroup());
        if (nextGroup != null) {
            karma.msg(sender, karma.getString("CHECK.SELF.GROUP", new Object[] {target.getGroup().getFormattedName(),
                target.getTrack().getNextGroup(target.getGroup()).getFormattedName()}));
        } else {
            karma.msg(sender, karma.getString("CHECK.SELF.GROUP.HIGHEST", new Object[] {target.getGroup().getFormattedName()}));
        }
        return true;
    }
}
