package com.tommytony.karma.commands;

import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaPlayer;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GiftCommand implements CommandExecutor {

    private Karma karma;
    private static List<Gift> confirms;

    public GiftCommand(Karma instance) {
        karma = instance;
    }
    static {
        confirms = new ArrayList();
    }

    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("karma.gift")) {
            karma.msg(sender, karma.config.getString("errors.nopermission"));
            return true;
        }
        if (args.length < 2) {
            karma.msg(sender, karma.config.getString("errors.badargs"));
            return false;
        }
        KarmaPlayer karmaGiver = null;
        if (sender instanceof Player) {
            karmaGiver = karma.getPlayer(sender.getName());
        }
        if ("confirm".equals(args[1]) && karmaGiver != null) {
            Gift confirm = this.getConfirmation(karmaGiver);
            if (confirm == null || confirm.expiry < System.currentTimeMillis()) {
                karma.msg(sender, karma.config.getString("gift.messages.noconfirm", "Can't find confirmation"));
                confirms.remove(confirm);
                return true;
            }
            if (confirm.receiver == null || (!confirm.receiver.getPlayer().isOnline() && !karma.config.getBoolean("gift.offline"))) {
                karma.msg(sender, karma.config.getString("errors.noplayer"));
                confirms.remove(confirm);
                return true;
            }
            if ((karmaGiver.getKarmaPoints() < confirm.amount
                    || (karmaGiver.getGroup().isFirstGroup(karmaGiver.getTrack())
                        && karmaGiver.getKarmaPoints() - karmaGiver.getGroup().getKarmaPoints() < confirm.amount))) {
                karma.msg(sender, karma.config.getString("gift.messages.notenough", "Overdrafts forbidden"));
                confirms.remove(confirm);
                return true;
            }
            this.sendGift(confirm);
            confirms.remove(confirm);
            return true;
        } else if (karmaGiver != null) {
            Gift confirm = this.getConfirmation(karmaGiver);
            if (confirm != null) {
                GiftCommand.confirms.remove(confirm);
            }
        }
        OfflinePlayer giftTarget = karma.getBukkitPlayer(args[1]);
        if (giftTarget == null) {
            karma.msg(sender, karma.config.getString("errors.noplayer"));
            return true;
        }
        if (!giftTarget.isOnline() && !karma.config.getBoolean("gift.offline", false)) {
            karma.msg(sender, karma.config.getString("errors.noplayer"));
            return true;
        }
        KarmaPlayer receipient = karma.getPlayer(giftTarget.getName());
        if (receipient == null) {
            karma.msg(sender, karma.config.getString("errors.noplayer"));
            return true;
        }
        int amount = karma.config.getInt("gift.amounts.default", karma.config.getInt("gift.amount", 1));
        if (args.length > 2) {
            int attemptedAmount = Integer.parseInt(args[2]);
            if (attemptedAmount < karma.config.getInt("gift.amounts.minimum", 1)
                    || attemptedAmount > karma.config.getInt("gift.amounts.maximum", 1)) {
                karma.msg(sender, karma.config.getString("gift.messages.outofbounds", "Too little/too much karma"));
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
            karma.msg(sender, karma.config.getString("gift.messages.notenough", "Overdrafts forbidden"));
            return true;
        }
        if (karmaGiver != null && !karmaGiver.canGift()) {
            long since = (System.currentTimeMillis() - karmaGiver.getLastGiftTime()) / 1000;
            long minutes = ((60 * karma.config.getInt("gift.cooldown", 60)) - since) / 60 + 1;
            karma.msg(sender, karma.config.getString("gift.messages.cooldown").replace("<minutes>", Long.toString(minutes)));
            return true;
        }
        long expiry = System.currentTimeMillis() + (karma.config.getInt("gift.confirm.timeout", 60) * 1000);
        Gift gift = new Gift(karmaGiver, receipient, amount, expiry);
        if (karmaGiver == null || !karma.config.getBoolean("gift.confirm.enabled", false)) {
            this.sendGift(gift);
        } else {
            GiftCommand.confirms.add(gift);
            karma.msg(sender, karma.config.getString("gift.messages.confirm", "/k gift confirm")
                    .replace("<player>", gift.receiver.getName())
                    .replace("<points>", Integer.toString(gift.amount)));
        }
        return true;
    }
    private void sendGift(Gift gift) {
        gift.receiver.addKarma(gift.amount);
        if (gift.gifter != null) {
            gift.gifter.updateLastGiftTime();
            gift.gifter.removeKarma(gift.amount);
            if (gift.gifter.getPlayer().isOnline()) {
                karma.msg(gift.gifter.getPlayer().getPlayer(), karma.config.getString("gift.messages.togifter")
                        .replace("<player>", gift.receiver.getName())
                        .replace("<points>", Integer.toString(gift.amount)));
            }
            karma.log.info(gift.gifter.getName() + " gave " + gift.amount + " karma points to " + gift.receiver.getName() + ".");
        }
        if (gift.receiver.getPlayer().isOnline()) {
            karma.msg(gift.receiver.getPlayer().getPlayer(),
                    karma.config.getString("gift.messages.toreceiver")
                    .replace("<player>", gift.gifter != null ? gift.gifter.getName() : "Server")
                    .replace("<points>", Integer.toString(gift.amount)));
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
