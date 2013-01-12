package com.tommytony.karma;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import org.bukkit.entity.Player;

public class KarmaParty implements Runnable {

    private final Karma karma;

    public KarmaParty(Karma karma) {
        this.karma = karma;
    }

    public void run() {
        for (Player player : this.karma.server.getOnlinePlayers()) {
            this.karma.msg(player, karma.config.getString("party.messages.announce"));
        }
        StringBuilder playerList = new StringBuilder();
        int totalDistributedKarma = 0;
        for (String playerName : this.karma.getPlayers().keySet()) {
            KarmaPlayer karmaPlayer = this.karma.getPlayers().get(playerName);
            long activeInterval = System.currentTimeMillis() - karmaPlayer.getLastActivityTime();
            int minutesAfk = (int) Math.floor(activeInterval / (1000 * 60));
            Player player = this.karma.findPlayer(karmaPlayer.getName());
            if (minutesAfk < 10) {
                int warPlayBonus = getWarPlayingBonus(karmaPlayer);
                int warZonemakerBonus = getZonemakerBonus(karmaPlayer);
                int total = karma.config.getInt("party.points") + warPlayBonus + warZonemakerBonus;
                this.karma.msg(player, karma.config.getString("party.messages.pointgain").replace("<points>", Integer.toString(total)));

                karmaPlayer.addKarma(total);
                playerList.append(playerName).append(", ");
                totalDistributedKarma += total;
            } else {
                this.karma.msg(player, karma.config.getString("party.messages.afknogain").replace("<points>", karma.config.getString("party.points")));
            }
        }
        if (playerList.toString().length() > 0) {
            this.karma.log.info(playerList.toString() + " gained " + totalDistributedKarma + " karma");
        }

        // save
        this.karma.getKarmaDatabase().putAll();

        // schedule next karma party
        this.karma.server.getScheduler().runTaskLater(karma.server.getPluginManager().getPlugin("Karma"),
                new KarmaParty(this.karma),
                this.karma.getNextRandomKarmaPartyDelay());
    }

    private int getWarPlayingBonus(KarmaPlayer karmaPlayer) {
        if (!karma.warEnabled) {
            return 0;
        }
        if (Warzone.getZoneByPlayerName(karmaPlayer.getName()) != null) {
            double chance = karma.config.getDouble("war.chance");
            int points = karma.config.getInt("war.points");
            if (chance < 0 || chance > 1) {
                throw new NullPointerException("war.chance must be a percentage (ie 0.25 for 25%)");
            }
            if (Math.random() < chance) {
                karma.msg(karmaPlayer.getPlayer().getPlayer(), karma.config.getString("war.messages.player"));
                return points;
            }
        }
        return 0;
    }

    private int getZonemakerBonus(KarmaPlayer karmaPlayer) {
        if (!karma.warEnabled) {
            return 0;
        }
        for (Warzone zone : War.war.getWarzones()) {
            for (String author : zone.getAuthors()) {
                if (author.equals(karmaPlayer.getName()) && !zoneIsEmpty(zone)
                        && zone.isEnoughPlayers()) {
                    double chance = karma.config.getDouble("war.chance");
                    int points = karma.config.getInt("war.points");
                    if (chance < 0 || chance > 1) {
                        throw new NullPointerException("war.chance must be a percentage (ie 0.25 for 25%)");
                    }
                    if (Math.random() < chance) {
                        karma.msg(karmaPlayer.getPlayer().getPlayer(), karma.config.getString("war.messages.creator"));
                        return points;
                    }
                }
            }
        }
        return 0;
    }

    private boolean zoneIsEmpty(Warzone zone) {
        for (Team team : zone.getTeams()) {
            if (team.getPlayers().size() > 0) {
                return false;
            }
        }
        return true;
    }
}
