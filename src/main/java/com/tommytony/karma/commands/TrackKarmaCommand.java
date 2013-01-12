package com.tommytony.karma.commands;

import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaPlayer;
import com.tommytony.karma.KarmaGroup;
import com.tommytony.karma.KarmaTrack;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TrackKarmaCommand implements CommandExecutor {
    
    private Karma karma;
    
    public TrackKarmaCommand(Karma instance) {
        this.karma = instance;
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        if(args.length < 3) {
            karma.msg(sender, karma.config.getString("errors.badargs"));
            return true;
        }
        
        List<Player> matches = karma.server.matchPlayer(args[1]);
        
        if(!(matches.isEmpty()) && (karma.getTrack(args[2]) != null) && sender.hasPermission("karma.track")) {
            KarmaTrack trackToChangeTo = karma.getTrack(args[2]);
            KarmaPlayer playerToChangeGroup = karma.players.get(matches.get(0).getName());
            karma.msg(sender, "Track of " + matches.get(0) + " changed from " + playerToChangeGroup.getTrack().getName() + " to " + args[2]);
            playerToChangeGroup.setTrack(trackToChangeTo);
        }
        
        return true;
    }
}