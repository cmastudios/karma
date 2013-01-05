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
        for (String line : karma.config.getStringList("help")) {
            karma.msg(sender, line);
        }
        if (sender.hasPermission("karma.help")) {
            for (String line : karma.config.getStringList("admin-help")) {
                karma.msg(sender, line);
            }
        }
        return true;
    }
}
