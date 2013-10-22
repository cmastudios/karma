package com.tommytony.karma.event;

import com.tommytony.karma.KarmaPlayer;
import java.util.Map;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class KarmaPartyEvent extends Event implements Cancellable {

    private Map<KarmaPlayer, Integer> earnings;
    private boolean cancelled;

    public KarmaPartyEvent(Map<KarmaPlayer, Integer> earnings) {
        this.earnings = earnings;
        this.cancelled = false;
    }
    private static final HandlerList handlers = new HandlerList();

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Get the earnings for every player.
     *
     * @return map of earnings
     */
    public Map<KarmaPlayer, Integer> getEarnings() {
        return earnings;
    }

    /**
     * Get the earnings for a specific player.
     * If a player's earnings is 0, it means they are AFK.
     *
     * @param player the player to get earnings for
     * @return amount of karma points earned
     */
    public int getEarnings(KarmaPlayer player) {
        return earnings.get(player);
    }

    /**
     * Set a player's earnings.
     * Note: if the points is less than zero, then the player will not be
     * notified of the karma party at all.
     *
     * @param player the player to modify earnings for
     * @param points amount of karma points earned
     */
    public void setEarnings(KarmaPlayer player, int points) {
        if (points < 0) {
            earnings.remove(player);
        } else {
            earnings.put(player, points);
        }
    }
}
