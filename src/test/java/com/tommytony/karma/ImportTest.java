package com.tommytony.karma;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import static junit.framework.Assert.*;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 *
 * @author Connor
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(PlayerJoinEvent.class)
public class ImportTest {

    Karma karma;
    FileConfiguration config;
    Database db;
    Player p;
    KarmaTrack def;
    List<KarmaGroup> groups;
    PlayerJoinEvent event;

    @Before
    public void setup() {
        karma = new Karma();
        config = mock(FileConfiguration.class);
        db = mock(Database.class);
        p = mock(Player.class);
        event = PowerMockito.mock(PlayerJoinEvent.class);
        when(p.getName()).thenReturn("cmastudios");
        PowerMockito.when(event.getPlayer()).thenReturn(p);
        groups = Arrays.asList(
                new KarmaGroup("recruit", 0, ChatColor.WHITE),
                new KarmaGroup("builder", 10, ChatColor.BLACK),
                new KarmaGroup("zonemaker", 100, ChatColor.BLUE));
        def = new KarmaTrack("default");
        def.setFirst(true);
        def.setGroups(groups);
        karma.tracks = Arrays.asList(def);
        karma.db = db;
        karma.players = new HashMap<String, KarmaPlayer>();
        karma.log = mock(Logger.class);
        karma.messages = new DummyResourceBundle();
    }

    @Test
    public void testNewPlayerPermissionImportNoBonus() {
        when(db.exists(p)).thenReturn(false);
        when(p.hasPermission("karma.recruit")).thenReturn(true);
        when(p.hasPermission("karma.builder")).thenReturn(true);
        when(config.getBoolean("import.bonus")).thenReturn(false);
        karma.config = config;
        KarmaPlayerListener listener = new KarmaPlayerListener(karma);
        listener.onPlayerJoin(event);
        assertNotNull(karma.players.get("cmastudios"));
        assertEquals(10, karma.players.get("cmastudios").getKarmaPoints());
    }
    @Test
    public void testNewPlayerPermissionImportBonus() {
        when(db.exists(p)).thenReturn(false);
        when(p.hasPermission("karma.recruit")).thenReturn(true);
        when(p.hasPermission("karma.builder")).thenReturn(true);
        when(config.getBoolean("import.bonus")).thenReturn(true);
        when(config.getDouble("import.percent")).thenReturn(0.25);
        karma.config = config;
        KarmaPlayerListener listener = new KarmaPlayerListener(karma);
        listener.onPlayerJoin(event);
        assertNotNull(karma.players.get("cmastudios"));
        assertEquals(32, karma.players.get("cmastudios").getKarmaPoints());
    }

    @Test
    public void testExistingPlayer() {
        KarmaPlayer kp = mock(KarmaPlayer.class);
        when(kp.getTrack()).thenReturn(def);
        when(db.get(p)).thenReturn(kp);
        when(db.exists(p)).thenReturn(true);
        when(p.hasPermission("karma.recruit")).thenReturn(true);
        when(p.hasPermission("karma.builder")).thenReturn(true);
        when(p.hasPermission("karma.zonemaker")).thenReturn(false);
        Server bukkitServ = mock(Server.class);
        when(bukkitServ.getOnlinePlayers()).thenReturn(new Player[]{});
        karma.server = bukkitServ;
        when(kp.getKarmaPoints()).thenReturn(15);
        when(config.getString("promotion.command")).thenReturn("pex user <player> setgroup <group>");
        karma.config = config;
        KarmaPlayerListener listener = new KarmaPlayerListener(karma);
        listener.onPlayerJoin(event);
        assertNotNull(karma.players.get("cmastudios"));
        assertEquals(15, karma.players.get("cmastudios").getKarmaPoints());
        verify(bukkitServ, never()).dispatchCommand(any(CommandSender.class), anyString());
    }
    @Test
    public void testExistingPlayerPermissionWipe() {
        KarmaPlayer kp = mock(KarmaPlayer.class);
        when(kp.getTrack()).thenReturn(def);
        when(db.get(p)).thenReturn(kp);
//        List<KarmaGroup> modgroups = Arrays.asList(
//                new KarmaGroup("minimod", 500, ChatColor.WHITE),
//                new KarmaGroup("moderator", 1000, ChatColor.BLACK),
//                new KarmaGroup("megamod", 2000, ChatColor.BLUE));
//        KarmaTrack mod = new KarmaTrack("mod");
//        mod.setGroups(groups);
//        karma.tracks = Arrays.asList(def, mod);
        when(db.exists(p)).thenReturn(true);
        when(p.hasPermission("karma.recruit")).thenReturn(false);
        when(p.hasPermission("karma.builder")).thenReturn(false);
        when(p.hasPermission("karma.zonemaker")).thenReturn(false);
        Server bukkitServ = mock(Server.class);
        when(bukkitServ.getOnlinePlayers()).thenReturn(new Player[]{});
        karma.server = bukkitServ;
        when(kp.getKarmaPoints()).thenReturn(15);
        when(config.getString("promotion.command")).thenReturn("pex user <player> setgroup <group>");
        karma.config = config;
        KarmaPlayerListener listener = new KarmaPlayerListener(karma);
        listener.onPlayerJoin(event);
        assertNotNull(karma.players.get("cmastudios"));
        verify(bukkitServ).dispatchCommand(any(CommandSender.class), eq("pex user cmastudios setgroup builder"));
    }
    @Test
    public void testExistingPlayerActivityLoss() {
        KarmaPlayer kp = mock(KarmaPlayer.class);
        when(kp.getTrack()).thenReturn(def);
        // 5 days off the server
        when(kp.getLastActivityTime()).thenReturn(System.currentTimeMillis() - (86400000 * 5));
        when(db.get(p)).thenReturn(kp);
        when(db.exists(p)).thenReturn(true);
        when(p.hasPermission("karma.recruit")).thenReturn(true);
        when(p.hasPermission("karma.builder")).thenReturn(true);
        when(p.hasPermission("karma.zonemaker")).thenReturn(false);
        Server bukkitServ = mock(Server.class);
        when(bukkitServ.getOnlinePlayers()).thenReturn(new Player[]{});
        karma.server = bukkitServ;
        when(kp.getKarmaPoints()).thenReturn(15);
        when(config.getString("promotion.command")).thenReturn("pex user <player> setgroup <group>");
        karma.config = config;
        KarmaPlayerListener listener = new KarmaPlayerListener(karma);
        listener.onPlayerJoin(event);
        assertNotNull(karma.players.get("cmastudios"));
        verify(kp).removeKarmaAutomatic(5);
        verify(bukkitServ, never()).dispatchCommand(any(CommandSender.class), anyString());
    }
}
