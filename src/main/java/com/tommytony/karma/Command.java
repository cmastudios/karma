package com.tommytony.karma;

public class Command {
	/*
	 * Commands
	 * -1 - unknown
	 * 
	 * 0 - Check self '/k'
	 * 1 - Check others '/k <player>'
	 * 2 - View ranks '/k ranks'
	 * 3 - View help '/k help'
	 * 4 - Gift karma '/k gift <player>'
	 * 5 - Promote a player '/k promote <player>'
	 * 6 - Set a player's karma '/k set <player> <karma>'
	 */
	public static int getCommand(String cmd) {
		if (cmd == "ranks" || cmd == "rank" || cmd == "groups") {
			return 2;
		}
		if (cmd == "help" || cmd == "?") {
			return 3;
		}
		if (cmd == "gift" || cmd == "give") {
			return 4;
		}
		if (cmd == "promo" || cmd == "promote") {
			return 5;
		}
		if (cmd == "set") {
			return 6;
		}
		if (cmd.length() >= 1) {
			return 1;
		}
		if (cmd.length() == 0) {
			return 0;
		}
		return -1;
		
	}
	
}
