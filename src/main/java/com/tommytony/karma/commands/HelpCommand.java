package com.tommytony.karma.commands;

import com.tommytony.karma.Karma;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class HelpCommand implements CommandExecutor {

    private Karma karma;

    public HelpCommand(Karma instance) {
        karma = instance;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length != 1) {
            karma.msg(sender, karma.config.getString("errors.badargs"));
            return false;
        }
        karma.msg(sender, karma.config.getString("help"));
        if (sender.hasPermission("karma.help")) {
            karma.msg(sender, karma.config.getString("admin-help"));
        }
        return true;
    }
}
