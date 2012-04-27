package com.tommytony.karma;

public enum Command {
	UNKNOWN,
	CHECK,
	CHECKOTHER,
	RANKS,
	HELP,
	GIFT,
	PROMOTE,
	SET;
	public static Command getCommand(String cmd) {
		if (cmd == "ranks" || cmd == "rank" || cmd == "groups") {
			return RANKS;
		}
		if (cmd == "help" || cmd == "?") {
			return HELP;
		}
		if (cmd == "gift" || cmd == "give") {
			return GIFT;
		}
		if (cmd == "promo" || cmd == "promote") {
			return PROMOTE;
		}
		if (cmd == "set") {
			return SET;
		}
		if (cmd.length() == 1) {
			return CHECKOTHER;
		}
		if (cmd.length() == 0) {
			return CHECK;
		}
		return UNKNOWN;
		
	}
	
}
