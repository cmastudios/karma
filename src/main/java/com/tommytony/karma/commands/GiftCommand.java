package com.tommytony.karma.commands;

import com.tommytony.karma.Karma;
import com.tommytony.karma.KarmaPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GiftCommand implements CommandExecutor {

    private Karma karma;

    public GiftCommand(Karma instance) {
        karma = instance;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("karma.gift")) {
            karma.msg(sender, karma.config.getString("errors.nopermission"));
            return true;
        }
        if (args.length != 2) {
            karma.msg(sender, karma.config.getString("errors.badargs"));
            return false;
        }
        Player giftTarget = karma.server.getPlayer(args[1]);
        if (giftTarget == null) {
            karma.msg(sender, karma.config.getString("errors.noplayer"));
            return true;
        }
        KarmaPlayer karmaGiver = null;
        if (sender instanceof Player) {
            karmaGiver = karma.players.get(((Player) sender).getName());
        }

        if (karmaGiver == null || karmaGiver.getKarmaPoints() > 0) {
            KarmaPlayer karmaGiftReceiver = karma.getPlayers().get(
                    giftTarget.getName());

            if (karmaGiftReceiver != null
                    && !sender.getName().equals(
                    giftTarget.getName())) {

                String gifterName = "server";
                if (karmaGiver != null) {
                    gifterName = ((Player) sender).getName();
                    if (karmaGiver.canGift()) {
                        karmaGiver.updateLastGiftTime();
                        karmaGiver.removeKarma(karma.config.getInt("gift.amount"));
                        karma.msg(
                                sender,
                                karma.config.getString("gift.messages.togifter").replace("<player>", karmaGiftReceiver.getName()).replace("<points>", Integer.toString(karma.config.getInt("gift.amount"))));
                    } else {
                        long since = (System.currentTimeMillis() - karmaGiver.getLastGiftTime()) / 1000;
                        karma.msg(
                                sender,
                                karma.config.getString("gift.messages.cooldown").replace("<minutes>", Long.toString((3600 - since) / 60)));
                        return true;
                    }
                }

                karmaGiftReceiver.addKarma(karma.config.getInt("gift.amount"));
                karma.msg(
                        giftTarget,
                        karma.config.getString("gift.messages.toreceiver").replace("<player>", gifterName).replace("<points>", Integer.toString(karma.config.getInt("gift.amount"))));

                karma.log.info(
                        gifterName + " gave "
                        + karma.config.getInt("gift.amount")
                        + " karma to " + giftTarget.getName());

                return true;
            } else {
                karma.log.warning("Couldn't find target or targeted self.");
            }
        }
        return true;
    }
}
