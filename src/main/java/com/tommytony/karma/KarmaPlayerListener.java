package com.tommytony.karma;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;

public class KarmaPlayerListener implements Listener {

    private final Karma karma;

    public KarmaPlayerListener(Karma karma) {
        this.karma = karma;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.karma.loadOrCreateKarmaPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String playerName = event.getPlayer().getName();
        KarmaPlayer player = this.karma.getPlayer(playerName);
        if (player != null) {
            this.karma.getKarmaDatabase().put(player);	// save latest changes
            this.karma.getPlayers().remove(event.getPlayer().getName());
        }
    }
    boolean pingOnPlayerCommand = true;
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String playerName = event.getPlayer().getName();
        if (pingOnPlayerCommand && this.karma.getPlayers().containsKey(playerName)) {
            this.karma.getPlayer(playerName).ping();
        }
    }
    boolean pingOnPlayerChat = true;
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String playerName = event.getPlayer().getName();
        if (pingOnPlayerChat && this.karma.getPlayers().containsKey(playerName)) {
            this.karma.getPlayer(playerName).ping();
        }
    }
    boolean pingOnPlayerMove = true;
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        String playerName = event.getPlayer().getName();
        if (pingOnPlayerMove && this.karma.getPlayers().containsKey(playerName)) {
            this.karma.getPlayer(playerName).ping();
        }
    }
    boolean pingOnPlayerBuild = true;
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        String playerName = event.getPlayer().getName();
        if (pingOnPlayerBuild && this.karma.getPlayers().containsKey(playerName)) {
            this.karma.getPlayer(playerName).ping();
        }
    }
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        String playerName = event.getPlayer().getName();
        if (pingOnPlayerBuild && this.karma.getPlayers().containsKey(playerName)) {
            this.karma.getPlayer(playerName).ping();
        }
    }
    boolean pingOnPlayerDamage = true;
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            String playerName = ((Player)event.getDamager()).getName();
            if (pingOnPlayerDamage && this.karma.getPlayers().containsKey(playerName)) {
                this.karma.getPlayer(playerName).ping();
            }
        }
    }
}
