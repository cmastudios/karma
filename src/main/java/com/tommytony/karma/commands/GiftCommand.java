package com.tommytony.karma.commands;

import com.google.common.collect.ImmutableList;
import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaPlayer;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.Validate;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class GiftCommand implements CommandExecutor, TabCompleter{

    private Karma karma;
    private static List<Gift> confirms;

    public GiftCommand(Karma instance) {
        karma = instance;
    }
    static {
        confirms = new ArrayList();
    }

    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        Validate.isTrue(sender.hasPermission("karma.gift"), karma.getString("ERROR.NOPERMISSION", new Object[] {}));
        Validate.isTrue(args.length >= 2, karma.getString("ERROR.ARGS", new Object[] {"/karma gift <player> [points]"}));
        KarmaPlayer karmaGiver = null;
        boolean senderIsPlayer = false;
        if (sender instanceof Player) {
            senderIsPlayer = true;
            karmaGiver = karma.getPlayer(sender.getName());
        }
        if ("confirm".equals(args[1]) && senderIsPlayer) {
            Gift confirm = this.getConfirmation(karmaGiver);
            if (confirm == null || confirm.expiry < System.currentTimeMillis()) {
                karma.msg(sender, karma.getString("GIFT.CONFIRM.404", new Object[] {}));
                confirms.remove(confirm);
                return true;
            }
            if (confirm.receiver == null || (!confirm.receiver.getPlayer().isOnline() && !karma.config.getBoolean("gift.offline"))) {
                karma.msg(sender, karma.getString("ERROR.PLAYER404", new Object[] {confirm.receiver == null ? "null" : confirm.receiver.getName()}));
                confirms.remove(confirm);
                return true;
            }
            if ((karmaGiver.getKarmaPoints() < confirm.amount
                    || (karmaGiver.getGroup().isFirstGroup(karmaGiver.getTrack())
                        && karmaGiver.getKarmaPoints() - karmaGiver.getGroup().getKarmaPoints() < confirm.amount))) {
                karma.msg(sender, karma.getString("GIFT.NOTENOUGH", new Object[] {}));
                confirms.remove(confirm);
                return true;
            }
            this.sendGift(confirm);
            confirms.remove(confirm);
            return true;
        } else if (senderIsPlayer) {
            Gift confirm = this.getConfirmation(karmaGiver);
            if (confirm != null) {
                GiftCommand.confirms.remove(confirm);
            }
        }
        OfflinePlayer giftTarget = karma.getBukkitPlayer(args[1]);
        Validate.notNull(giftTarget, karma.getString("ERROR.PLAYER404", new Object[] {args[1]}));
        Validate.isTrue(giftTarget.hasPlayedBefore(), karma.getString("ERROR.PLAYER404", new Object[] {args[1]}));
        Validate.isTrue(giftTarget.isOnline() || karma.config.getBoolean("gift.offline", false) == true, karma.getString("ERROR.PLAYER404.OFFLINE", new Object[] {giftTarget.getName()}));
        KarmaPlayer receipient = karma.getPlayer(giftTarget.getName());
        Validate.notNull(receipient, karma.getString("ERROR.PLAYER404.NOKP", new Object[] {giftTarget.getName()}));
        Validate.isTrue(receipient != karmaGiver, karma.getString("GIFT.NOGIFTSELF", new Object[] {}));
        int amount = karma.config.getInt("gift.amounts.default", karma.config.getInt("gift.amount", 1));
        if (args.length > 2) {
            int attemptedAmount = Integer.parseInt(args[2]);
            if (attemptedAmount < karma.config.getInt("gift.amounts.minimum", 1)
                    || attemptedAmount > karma.config.getInt("gift.amounts.maximum", 1)) {
                karma.msg(sender, karma.getString("GIFT.OUTOFBOUNDS", new Object[] {}));
                return true;
            }
            amount = attemptedAmount;
        }
        /*
         * Explanation of the massive second argument
         * Basically, this checks to make sure the player isn't exploiting
         * karma's feature which doesn't remove karma if it would cause the
         * player to go below the first group's karma points.
         * If the player's group is the first group and the change in karma
         * points would cause the player to go less than their group's karma
         * points, then cancel the gift.
         */
        if (karmaGiver != null && (karmaGiver.getKarmaPoints() < amount
                || (karmaGiver.getGroup().isFirstGroup(karmaGiver.getTrack())
                    && karmaGiver.getKarmaPoints() - karmaGiver.getGroup().getKarmaPoints() < amount))) {
            karma.msg(sender, karma.getString("GIFT.NOTENOUGH", new Object[] {}));
            return true;
        }
        if (karmaGiver != null && !karmaGiver.canGift()) {
            long since = (System.currentTimeMillis() - karmaGiver.getLastGiftTime()) / 1000;
            long minutes = ((60 * karma.config.getInt("gift.cooldown", 60)) - since) / 60 + 1;
            karma.msg(sender, karma.getString("GIFT.COOLDOWN", new Object[] {karma.parseNumber(minutes)}));
            return true;
        }
        long expiry = System.currentTimeMillis() + (karma.config.getInt("gift.confirm.timeout", 60) * 1000);
        Gift gift = new Gift(karmaGiver, receipient, amount, expiry);
        if (karmaGiver == null || !karma.config.getBoolean("gift.confirm.enabled", false)) {
            this.sendGift(gift);
        } else {
            GiftCommand.confirms.add(gift);
            karma.msg(sender, karma.getString("GIFT.CONFIRM.ASK", new Object[] {karma.parseNumber(amount), receipient.getName()}));
        }
        return true;
    }
    private void sendGift(Gift gift) {
        gift.receiver.addKarma(gift.amount);
        if (gift.gifter != null) {
            gift.gifter.updateLastGiftTime();
            gift.gifter.removeKarma(gift.amount);
            if (gift.gifter.getPlayer().isOnline()) {
                karma.msg(gift.gifter.getPlayer().getPlayer(), karma.getString("GIFT.THANKS", new Object[] {gift.receiver.getName(), karma.parseNumber(gift.amount)}));
            }
            karma.log.info(gift.gifter.getName() + " gave " + karma.parseNumber(gift.amount) + " karma points to " + gift.receiver.getName() + ".");
        }
        if (gift.receiver.getPlayer().isOnline()) {
            karma.msg(gift.receiver.getPlayer().getPlayer(), karma.getString("GIFT.RECEIPIENT", new Object[] {gift.gifter != null ? gift.gifter.getName() : "Server", karma.parseNumber(gift.amount)}));
        }
    }
    private Gift getConfirmation(KarmaPlayer gifter) {
        for (Gift confirm : confirms) {
            if (confirm.gifter == gifter) {
                return confirm;
            }
        }
        return null;
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        KarmaPlayer karmaGiver = null;
        if (sender instanceof Player) {
            karmaGiver = karma.getPlayer(sender.getName());
        }
        if (args.length == 2 && karmaGiver != null && this.getConfirmation(karmaGiver) != null) {
            return ImmutableList.of("confirm");
        } else if (args.length == 2) {
            List<String> players = new ArrayList();
            for (Player player : karma.server.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return StringUtil.copyPartialMatches(args[1], players, new ArrayList<String>(players.size()));
        }
        if (args.length == 3) {
            return ImmutableList.of(Integer.toString(karma.config.getInt("gift.amounts.default", karma.config.getInt("gift.amount", 1))));
        }
        return ImmutableList.of();
    }
    private class Gift {
        public KarmaPlayer gifter;
        public KarmaPlayer receiver;
        public int amount;
        public long expiry;

        public Gift(KarmaPlayer gifter, KarmaPlayer receiver, int amount, long expiry) {
            this.gifter = gifter;
            this.receiver = receiver;
            this.amount = amount;
            this.expiry = expiry;
        }
    }
}
