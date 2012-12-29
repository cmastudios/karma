package com.tommytony.karma;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class KarmaPlugin extends JavaPlugin {

    protected Karma karma;
    public void onDisable() {
        this.getServer().getScheduler().cancelTasks(this);
        karma.db.putAll();
        karma.players.clear();
    }

    public void onEnable() {
        karma = new Karma();
        karma.server = getServer();
        karma.log = getLogger();
        // Init data
        karma.players = new HashMap<String, KarmaPlayer>();
        karma.db = new Database(karma);
        karma.db.initialize();

        PluginManager manager = this.getServer().getPluginManager();

        // Setup config
        saveDefaultConfig();
        karma.config = getConfig();
        try {
            karma.config.load(new File("plugins/Karma/config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
            karma.log.severe("Your computer cannot read the config! Disabling..");
            manager.disablePlugin(this);
            return;
        } catch (InvalidConfigurationException e) {
            karma.log.severe("Bad configuration! Disabling..");
            manager.disablePlugin(this);
            return;
        }

        // Load karma groups
        karma.loadKarmaGroups();

        // Register events

        KarmaWorldListener worldListener = new KarmaWorldListener(this);
        manager.registerEvents(worldListener, this);

        KarmaPlayerListener playerListener = new KarmaPlayerListener(karma);
        manager.registerEvents(playerListener, this);

        // Load online players
        this.getServer().getScheduler().runTask(this, new LoadPlayers(karma));

        // Check for war and enable bonuses if enabled in config
        try {
            Class.forName("com.tommytony.war.War");
            if (karma.config.getBoolean("war.bonus")) {
                karma.warEnabled = true;
            }
        } catch (ClassNotFoundException e) {
            karma.warEnabled = false;
            if (karma.config.getBoolean("war.bonus")) {
                karma.log.warning("war.bonus enabled in configuration but the War plugin was not found! Bonuses have been disabled.");
            }
        }

        // Launch karma party train!!
        this.getServer().getScheduler().runTaskLater(this, new KarmaParty(karma),
                karma.getNextRandomKarmaPartyDelay());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {
        try {
            if (command.getName().equals("karma")
                    || command.getName().equals("k")) {
                if (args.length == 0) {
                    // Check their own karma
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("This command cannot be used by console");
                        return true;
                    }
                    KarmaPlayer karmaCheckPlayer = karma.players.get(((Player) sender).getName());
                    if (karmaCheckPlayer != null) {
                        this.msg(
                                sender,
                                karma.config.getString("check.self.message").replace(
                                "<points>",
                                karmaCheckPlayer.getKarmaPoints()
                                + "").replace(
                                "<curgroup>",
                                getPlayerGroupString(karmaCheckPlayer)).replace(
                                "<nextgroup>",
                                getPlayerNextGroupString(karmaCheckPlayer)).replace(
                                "<curgroupcolor>",
                                getPlayerGroupColor(karmaCheckPlayer).toString()).replace(
                                "<nextgroupcolor>",
                                getPlayerNextGroupColor(
                                karmaCheckPlayer).toString()));
                    }
                    karmaCheckPlayer = null;
                    return true;
                }

                if (args[0].equalsIgnoreCase("ranks")) {
                    String ranksString = karma.config.getString("viewranks.prefix");
                    KarmaGroup group = karma.startGroup;
                    while (group != null) {
                        ranksString += group.getChatColor() + group.getGroupName() + ChatColor.GRAY + "("
                                + ChatColor.YELLOW + group.getKarmaPoints()
                                + ChatColor.GRAY + ")";
                        if (group.getNext() != null) {
                            ranksString += ChatColor.WHITE + " -> "
                                    + ChatColor.GRAY;
                        }
                        group = group.getNext();
                    }
                    this.msg(sender, ranksString);
                    return true;
                }
                if (args[0].equalsIgnoreCase("help")) {
                    for (String line : karma.config.getStringList("help")) {
                        this.msg(sender, line);
                    }
                    return true;
                }
                if (args[0].equalsIgnoreCase("gift")) {
                    if (!sender.hasPermission("karma.gift")) {
                        this.msg(sender,
                                karma.config.getString("errors.nopermission"));
                        return true;
                    }
                    if (args.length < 2) {
                        this.msg(sender, karma.config.getString("errors.badargs"));
                        return true;
                    }
                    Player giftTarget = this.getServer().getPlayer(args[1]);
                    if (giftTarget == null) {
                        this.msg(sender, karma.config.getString("errors.noplayer"));
                        return true;
                    }
                    KarmaPlayer karmaGiver = null;
                    if (sender instanceof Player) {
                        karmaGiver = karma.players.get(((Player) sender).getName());
                    }

                    if (karmaGiver == null || karmaGiver.getKarmaPoints() > 0) {
                        KarmaPlayer karmaGiftReceiver = this.getPlayers().get(
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
                                    this.msg(
                                            sender,
                                            karma.config.getString(
                                            "gift.messages.togifter").replace(
                                            "<player>",
                                            karmaGiftReceiver.getName()).replace(
                                            "<points>",
                                            karma.config.getInt("gift.amount")
                                            + ""));
                                } else {
                                    long since = (System.currentTimeMillis() - karmaGiver.getLastGiftTime()) / 1000;
                                    this.msg(
                                            sender,
                                            karma.config.getString(
                                            "gift.messages.cooldown").replace(
                                            "<minutes>",
                                            ((3600 - since) / 60)
                                            + ""));
                                    return true;
                                }
                            }

                            karmaGiftReceiver.addKarma(karma.config.getInt("gift.amount"));
                            this.msg(
                                    giftTarget,
                                    karma.config.getString("gift.messages.toreceiver").replace("<player>", gifterName).replace(
                                    "<points>",
                                    karma.config.getInt("gift.amount")
                                    + ""));

                            this.getServer().getLogger().log(Level.INFO,
                                    "Karma> "
                                    + gifterName
                                    + " gave "
                                    + karma.config.getInt("gift.amount")
                                    + " karma to "
                                    + giftTarget.getName());

                            return true;
                        } else {
                            this.getServer().getLogger().log(Level.WARNING,
                                    "Karma> Couldn't find target or targetted self.");
                        }
                    }
                    return true;
                }
                if (args[0].equalsIgnoreCase("promote") || args[0].equalsIgnoreCase("promo")) {
                    if (args.length < 2) {
                        this.msg(sender, karma.config.getString("errors.badargs"));
                        return true;
                    }
                    Player promoteTarget = this.getServer().getPlayer(args[1]);
                    KarmaGroup currentGroup = karma.startGroup;
                    if (promoteTarget == null) {
                        this.msg(sender,
                                karma.config.getString("promote.messages.noplayer"));
                        return true;
                    }
                    KarmaPlayer karmaPromoteTarget = karma.players.get(promoteTarget.getName());
                    if (karmaPromoteTarget == null) {
                        return true;
                    }
                    while (currentGroup != null) {
                        if (karmaPromoteTarget.getKarmaPoints() < currentGroup.getKarmaPoints()) {
                            if (sender.hasPermission("karma.promote."
                                    + currentGroup.getGroupName())) {
                                karmaPromoteTarget.addKarma(currentGroup.getKarmaPoints()
                                        - karmaPromoteTarget.getKarmaPoints());
                                this.msg(
                                        promoteTarget,
                                        karma.config.getString(
                                        "promocommand.messages.promoted").replace("<player>",
                                        promoteTarget.getName()).replace(
                                        "<group>",
                                        currentGroup.getGroupName()));
                                return true;
                            } else {
                                this.msg(sender, karma.config.getString("errors.nopermission"));
                                return true;
                            }

                        }
                        currentGroup = currentGroup.getNext();
                    }
                }
                if (args[0].equalsIgnoreCase("set")) {
                    if (args.length < 3) {
                        this.msg(sender, karma.config.getString("errors.badargs"));
                        return true;
                    }

                    try {
                        Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED
                                + "The third argument must be an integer!");
                        return true;
                    }
                    List<Player> matches3 = this.getServer().matchPlayer(
                            args[1]);
                    if (!matches3.isEmpty() && Integer.parseInt(args[2]) >= 0
                            && sender.hasPermission("karma.set")) {
                        return this.setAmount(matches3,
                                Integer.parseInt(args[2]));
                    }
                    return true;
                }
                if (args[0].equalsIgnoreCase("add")) {
                    if (args.length < 3) {
                        this.msg(sender, karma.config.getString("errors.badargs"));
                        return true;
                    }

                    try {
                        Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED
                                + "The third argument must be an integer!");
                        return true;
                    }
                    List<Player> matches3 = this.getServer().matchPlayer(
                            args[1]);
                    if (!matches3.isEmpty() && Integer.parseInt(args[2]) >= 0
                            && sender.hasPermission("karma.set")) {
                        KarmaPlayer playerToAddKarma = karma.players.get(matches3.get(0).getName());
                        return this.setAmount(matches3,
                                Integer.parseInt(args[2]) + playerToAddKarma.getKarmaPoints());
                    }
                    return true;
                }
                if (args.length == 1) {
                    // Check other players karma
                    Player checkOtherTarget = this.getServer().getPlayer(
                            args[0]);
                    if (checkOtherTarget == null) {
                        this.msg(sender, karma.config.getString("errors.noplayer"));
                        return true;
                    }
                    KarmaPlayer karmaCheckOtherTarget = karma.players.get(checkOtherTarget.getName());
                    if (karmaCheckOtherTarget != null) {
                        this.msg(
                                sender,
                                karma.config.getString("check.others.message").replace("<player>",
                                checkOtherTarget.getName()).replace(
                                "<points>",
                                karmaCheckOtherTarget.getKarmaPoints()
                                + "").replace(
                                "<curgroupcolor>",
                                getPlayerGroupColor(karmaCheckOtherTarget).toString()));
                    } else {
                        this.msg(sender, karma.config.getString("errors.noplayer"));
                    }
                    checkOtherTarget = null;
                    karmaCheckOtherTarget = null;

                    return true;
                }

                this.msg(sender, karma.config.getString("errors.unknowncommand"));
                return true;

            }

        } catch (Exception e) {
            this.msg(sender, karma.config.getString("errors.commandexception").replace("<exception>", e.toString()));
            e.printStackTrace();
            System.out.println("Karma> Error encountered.");
        }

        return true;
    }

    public void saveConfig() {
        try {
            karma.config.save(new File("plugins/Karma/config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
