package com.tommytony.karma.commands;

import com.google.common.collect.ImmutableList;
import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaGroup;
import com.tommytony.karma.KarmaPlayer;
import com.tommytony.karma.KarmaTrack;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class ChangeTrackCommand implements CommandExecutor, TabCompleter {

    private Karma karma;

    public ChangeTrackCommand(Karma instance) {
        karma = instance;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length != 3) {
            karma.msg(sender, karma.config.getString("errors.badargs"));
            return false;
        }
        OfflinePlayer chTrackTarget = karma.getBukkitPlayer(args[1]);
        if (chTrackTarget == null) {
            karma.msg(sender, karma.config.getString("errors.noplayer"));
            return true;
        }
        KarmaPlayer chKarmaTrackTarget = karma.getPlayer(chTrackTarget.getName());
        if (chKarmaTrackTarget == null) {
            karma.msg(sender, karma.config.getString("errors.noplayer"));
            return true;
        }

        KarmaTrack targetTrack = karma.getTrack(args[2]);
        if (targetTrack == null) {
            karma.msg(sender, karma.config.getString("errors.notrack"));
            return true;
        }
        if (chKarmaTrackTarget.getTrack() == targetTrack) {
            karma.msg(sender, karma.config.getString("errors.isalreadyintrack"));
            return true;
        }
        if (!sender.hasPermission("karma.track." + targetTrack.getName()) && !sender.hasPermission("karma.track.*")) {
            karma.msg(sender, karma.config.getString("errors.nopermission"));
            return true;
        }
        // Will automatically add karma to bump the player's karma up to the first group in the track
        chKarmaTrackTarget.setTrack(targetTrack);
        chKarmaTrackTarget.setGroup(targetTrack.getGroupOnBounds(chKarmaTrackTarget.getKarmaPoints()));
        if (chTrackTarget.isOnline()) {
            if (chKarmaTrackTarget.getGroupByPermissions() != chKarmaTrackTarget.getGroup()) {
                throw new NullPointerException("Attempted to promote player " + chKarmaTrackTarget.getName() + " to group " + chKarmaTrackTarget.getGroup().getGroupName()
                        + "during a track change, but after promotion, the player doesn't have " + chKarmaTrackTarget.getGroup().getPermission() + "! Promote command "
                        + "configured incorrectly.");
            }
        }
        String msg = karma.config.getString("changetrack.message")
                .replace("<player>", chTrackTarget.getName())
                .replace("<track>", targetTrack.getName())
                .replace("<groupcolor>", chKarmaTrackTarget.getGroup().getChatColor().toString())
                .replace("<group>", chKarmaTrackTarget.getGroup().getGroupName());
        for (Player playerOnline : karma.server.getOnlinePlayers()) {
            karma.msg(playerOnline, msg);
        }
        return false;
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 2) {
            List<String> players = new ArrayList();
            for (Player player : karma.server.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return StringUtil.copyPartialMatches(args[1], players, new ArrayList<String>(players.size()));
        }
        if (args.length == 3) {
            List<String> tracks = new ArrayList();
            for (KarmaTrack track : karma.tracks) {
                tracks.add(track.getName());
            }
            return StringUtil.copyPartialMatches(args[2], tracks, new ArrayList<String>(tracks.size()));
        }
        return ImmutableList.of();
    }
}
