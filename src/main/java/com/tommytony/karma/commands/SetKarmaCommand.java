package com.tommytony.karma.commands;

import com.google.common.collect.ImmutableList;
import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaPlayer;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.Validate;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class SetKarmaCommand implements CommandExecutor, TabCompleter {

    private Karma karma;

    public SetKarmaCommand(Karma instance) {
        karma = instance;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        Validate.isTrue(sender.hasPermission("karma.set"), karma.getString("ERROR.NOPERMISSION", new Object[] {}));
        Validate.isTrue(args.length == 3, karma.getString("ERROR.ARGS", new Object[] {"/karma set <player> <amount>"}));
        OfflinePlayer playerTarget = karma.getBukkitPlayer(args[1]);
        Validate.notNull(playerTarget, karma.getString("ERROR.PLAYER404", new Object[] {args[1]}));
        Validate.isTrue(playerTarget.hasPlayedBefore(), karma.getString("ERROR.PLAYER404", new Object[] {args[1]}));
        KarmaPlayer target = karma.getPlayer(playerTarget.getName());
        Validate.notNull(target, karma.getString("ERROR.PLAYER404.NOKP", new Object[] {playerTarget.getName()}));
        int karmaToSet = Integer.parseInt(args[2]);
        Validate.isTrue(karmaToSet >= 0, karma.getString("SET.POSITIVE", new Object[] {}));
        Validate.isTrue(karmaToSet != target.getKarmaPoints(), karma.getString("SET.NOCHANGE", new Object[] {}));
        karma.msg(sender, karma.getString("SET.SUCCESS", new Object[] {target.getName(), karmaToSet}));
        karma.log.info(target.getName() + "'s karma points set to " + karmaToSet + " from " + target.getKarmaPoints() + ".");
        target.setKarmaPoints(karmaToSet);
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
