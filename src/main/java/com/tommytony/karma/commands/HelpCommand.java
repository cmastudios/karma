package com.tommytony.karma.commands;

import com.tommytony.karma.Karma;
import org.apache.commons.lang.Validate;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class HelpCommand implements CommandExecutor {

    private Karma karma;

    public HelpCommand(Karma instance) {
        karma = instance;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        Validate.isTrue(args.length == 1, karma.getString("ERROR.ARGS", new Object[] {"/karma help"}));
        karma.msg(sender, karma.getString("HELP", new Object[] {}));
        if (sender.hasPermission("karma.help")) {
            karma.msg(sender, karma.getString("HELP.ADMIN", new Object[] {}));
        }
        return true;
    }
}
