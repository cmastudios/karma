package com.tommytony.karma.commands;

import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaGroup;
import com.tommytony.karma.KarmaPlayer;
import com.tommytony.karma.KarmaTrack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChangeTrackCommand implements CommandExecutor {

    private Karma karma;

    public ChangeTrackCommand(Karma instance) {
        karma = instance;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length != 3) {
            karma.msg(sender, karma.config.getString("errors.badargs"));
            return false;
        }
        Player chTrackTarget = karma.server.getPlayer(args[1]);
        if (chTrackTarget == null) {
            karma.msg(sender, karma.config.getString("errors.noplayer"));
            return true;
        }
        KarmaPlayer chKarmaTrackTarget = karma.players.get(chTrackTarget.getName());
        if (chKarmaTrackTarget == null) {
            return true;
        }

        KarmaTrack targetTrack = null;
        for (KarmaTrack track : karma.tracks) {
            if (track.getName().equals(args[2])) {
                targetTrack = track;
            }
        }
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
        KarmaGroup currentGroup = targetTrack.getFirstGroup();
        while (currentGroup != null) {
            if (chKarmaTrackTarget.getKarmaPoints() >= currentGroup.getKarmaPoints()) {
                chKarmaTrackTarget.setGroup(currentGroup);
            }
            currentGroup = targetTrack.getNextGroup(currentGroup);
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
}
