package com.tommytony.karma;

import java.util.Random;
import java.util.logging.Level;

import org.bukkit.entity.Player;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;


public class KarmaParty implements Runnable {

	private final Karma karma;
	private final Random random = new Random();

	public KarmaParty(Karma karma) {
		this.karma = karma;
	}

	public void run() {
		for (Player player : this.karma.getServer().getOnlinePlayers()) {
			this.karma.msg(player, karma.config.getString("party.messages.announce"));
		}
		String playerList = "";
		for (String playerName : this.karma.getPlayers().keySet()) {
			KarmaPlayer player = this.karma.getPlayers().get(playerName);
			long activeInterval = System.currentTimeMillis() - player.getLastActivityTime();
			int minutesAfk = (int) Math.floor(activeInterval / (1000 * 60));
			Player p = this.karma.findPlayer(player.getName());
			if (minutesAfk < 10) {
				int warPlayBonus = getWarPlayingBonus(player, p);
				int warZonemakerBonus = getZonemakerBonus(player, p);
				int total = karma.config.getInt("party.points") + warPlayBonus + warZonemakerBonus;
					this.karma.msg(p, karma.config.getString("party.messages.pointgain").replace("<points>", karma.config.getString("party.points")));
				
				player.addKarma(total);
				playerList += playerName + ", ";
			} else {
				this.karma
						.msg(p, karma.config.getString("party.messages.afknogain").replace("<points>", karma.config.getString("party.points")));
			}
		}
		if (!playerList.equals("")) {
			this.karma.getServer().getLogger()
					.log(Level.INFO, "Karma> " + playerList + "gained "+karma.config.getString("party.points")+" karma");
		}

		// save
		this.karma.getKarmaDatabase().putAll();

		// schedule next karma party
		this.karma
				.getServer()
				.getScheduler()
				.scheduleSyncDelayedTask(this.karma,
						new KarmaParty(this.karma),
						this.karma.getNextRandomKarmaPartyDelay());
	}

	private int getWarPlayingBonus(KarmaPlayer karmaPlayer, Player player) {
		if (!karma.warenabled) return 0;
		if (Warzone.getZoneByPlayerName(karmaPlayer.getName()) != null) {
			if (random.nextInt(3) == 2) {
				karma.msg(player, karma.config.getString("war.messages.player"));
				return 1;
			}
		}
		return 0;
	}

	private int getZonemakerBonus(KarmaPlayer karmaPlayer, Player player) {
		if (!karma.warenabled) return 0;
		for (Warzone zone : War.war.getWarzones()) {
			for (String author : zone.getAuthors()) {
				if (author.equals(karmaPlayer.getName()) && !zoneIsEmpty(zone)
						&& zone.isEnoughPlayers()) {
					if (random.nextInt(3) == 2) {
						karma.msg(player, karma.config.getString("war.messages.creator"));
						return 1;
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
