package com.tommytony.karma;

import com.tommytony.karma.event.KarmaPartyEvent;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;

public class KarmaParty implements Runnable {

    private final Karma karma;

    public KarmaParty(Karma karma) {
        this.karma = karma;
    }

    public void run() {
        Map<KarmaPlayer, Integer> earnings = new HashMap();
        StringBuilder playerList = new StringBuilder();
        int totalDistributedKarma = 0;
        for (KarmaPlayer player : karma.getPlayers().values()) {
            if (player.isAfk()) {
                earnings.put(player, 0); // No karma for being AFK
            } else {
                int warPlayBonus = karma.warEnabled && player.isPlayingWar() ? getWarPlayingBonus(player) : 0;
                int warZonemakerBonus = karma.warEnabled && player.hasActiveWarzone() ? getZonemakerBonus(player) : 0;
                int total = karma.config.getInt("party.points") + warPlayBonus + warZonemakerBonus;
                earnings.put(player, total);
            }
        }
        KarmaPartyEvent party = new KarmaPartyEvent(earnings);
        karma.server.getPluginManager().callEvent(party);
        if (party.isCancelled()) {
            this.scheduleNextKarmaParty();
            return;
        }
        earnings = party.getEarnings(); // Apply modified earnings
        for (KarmaPlayer karmaPlayer : earnings.keySet()) {
            Player player = karmaPlayer.getPlayer().getPlayer();
            int earnedKarmaPoints = earnings.get(karmaPlayer);
            if (!karmaPlayer.isAfk()) {
                if (player != null && !karma.config.getBoolean("party.silent", false)) {
                    karma.msg(player, karma.config.getString("party.messages.announce"));
                    if (karma.warEnabled) {
                        if (karmaPlayer.isPlayingWar()) {
                            karma.msg(karmaPlayer.getPlayer().getPlayer(), karma.config.getString("war.messages.player"));
                        }
                        if (karmaPlayer.hasActiveWarzone()) {
                            karma.msg(karmaPlayer.getPlayer().getPlayer(), karma.config.getString("war.messages.creator"));
                        }
                    }
                    karma.msg(player, karma.config.getString("party.messages.pointgain").replace("<points>", Integer.toString(earnedKarmaPoints)));
                }
                karmaPlayer.addKarma(earnedKarmaPoints);
                totalDistributedKarma += earnedKarmaPoints;
                playerList.append(player.getName()).append(", ");
            } else if (player != null && !karma.config.getBoolean("party.silent", false)) {
                karma.msg(player, karma.config.getString("party.messages.announce"));
                karma.msg(player, karma.config.getString("party.messages.afknogain").replace("<points>", karma.config.getString("party.points")));
            }
        }
        if (playerList.toString().length() > 0) {
            karma.log.fine(playerList.toString() + " gained a total of " + totalDistributedKarma + " karma points.");
        }

        // save
        this.karma.getKarmaDatabase().putAll();

        // schedule next karma party
        this.scheduleNextKarmaParty();
    }

    private int getWarPlayingBonus(KarmaPlayer karmaPlayer) {
        if (karmaPlayer.isPlayingWar()) {
            double chance = karma.config.getDouble("war.chance");
            int points = karma.config.getInt("war.points");
            if (chance < 0 || chance > 1) {
                throw new NullPointerException("war.chance must be a percentage (ie 0.25 for 25%)");
            }
            if (Math.random() < chance) {
                return points;
            }
        }
        return 0;
    }

    private int getZonemakerBonus(KarmaPlayer karmaPlayer) {
        if (karmaPlayer.hasActiveWarzone()) {
            double chance = karma.config.getDouble("war.chance");
            int points = karma.config.getInt("war.points");
            if (chance < 0 || chance > 1) {
                throw new NullPointerException("war.chance must be a percentage (ie 0.25 for 25%)");
            }
            if (Math.random() < chance) {
                return points;
            }
        }
        return 0;
    }
    private void scheduleNextKarmaParty() {
        this.karma.server.getScheduler().runTaskLater(karma.server.getPluginManager().getPlugin("Karma"),
                new KarmaParty(this.karma),
                this.karma.getNextRandomKarmaPartyDelay());
    }
}
