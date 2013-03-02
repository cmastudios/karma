package com.tommytony.karma;

import java.util.List;
import java.util.Map;

/**
 * Provides a karma API that other plugins can use.<br>
 * <p>Getting an instance of the API: 
 * <code>KarmaAPI karma = (KarmaAPI) this.getServer().getPluginManager().getPlugin("Karma");</code></p>
 * <p>Adding karma points to a player: 
 * <code>karma.getPlayer("cmastudios").addKarma(20);</code></p>
 */
public interface KarmaAPI {
    /**
     * Gets a map of all <i>online</i> karma players.
     * @return map of player names and the KarmaPlayer object.
     */
    public Map<String, KarmaPlayer> getPlayers();
    
    /**
     * Gets a karma player by name.
     * @param player the player's name
     * @return the karma player
     */
    public KarmaPlayer getPlayer(String player);
    
    /**
     * Get all karma tracks on the server.
     * @return a list of KarmaTracks
     */
    public List<KarmaTrack> getTracks();
    
    /**
     * Get the server's default track.
     * @return the track
     */
    public KarmaTrack getDefaultTrack();
    
    /**
     * Get a track on the server by name.
     * @param name the track's name
     * @return the track
     */
    public KarmaTrack getTrack(String name);
    
    /**
     * Get a track by it's hash code.
     * This is used in the karma database when storing the track.
     * @param hash the track's name's hash code
     * @return the track
     */
    public KarmaTrack getTrack(long hash);
    
    /**
     * Reload the list of players from the configuration.
     */
    public void reloadPlayers();
    
    /**
     * Reload the list of tracks from the configuration.
     * This also reloads all the groups as well.
     */
    public void reloadTracks();
}
