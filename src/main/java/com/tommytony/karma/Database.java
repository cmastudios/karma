package com.tommytony.karma;

import java.io.File;
import java.sql.*;
import org.bukkit.entity.Player;

public class Database {

    private final Karma karma;

    public Database(Karma karmaPlugin) {
        this.karma = karmaPlugin;
        if (new File("plugins/Karma").mkdir()) {
            this.karma.log.info("Creating files...");
        }
        if (karma.config.getBoolean("mysql.enabled")) {
            sqlite = false;
        }
        if (this.sqlite()) {
            try {
                Connection connection = this.getConnection();
                Statement statement = connection.createStatement();
                // (name, karma, lastactive, lastgift, lastprize, track)
                statement.executeUpdate("create table if not exists players (name text, karma numeric, lastactive numeric)");

                this.addColumn(connection, "lastgift numeric");
                // Shakes fist at sqlite no drop column
                this.addColumn(connection, "lastprize numeric");
                this.addColumn(connection, "track numeric");

                statement.close();
                connection.close();
            } catch (SQLException e) {
                this.karma.log.warning("Error while intilializing database. " + e.toString());
            }
        }

    }

    public boolean exists(String playerName) {
        return get(playerName) != null ? true : false;
    }

    public KarmaPlayer get(String playerName) {
        KarmaPlayer karmaPlayer = null;
        if (this.sqlite()) {
            try {
                Connection conn = this.getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM players WHERE name = ?");
                stmt.setString(1, playerName);
                ResultSet result = stmt.executeQuery();
                if (result.next()) {
                    KarmaTrack track = karma.getTrack(result.getLong("track"));
                    if (track == null) {
                        Player player = karma.server.getPlayerExact(playerName);
                        if (player != null) {
                            // If player is online get their track normally
                            track = karma.getInitialTrack(player);
                        } else {
                            track = karma.getDefaultTrack();
                        }
                    }
                    karmaPlayer = new KarmaPlayer(this.karma, playerName, 
                            result.getInt("karma"), 
                            result.getLong("lastactive"), 
                            result.getLong("lastgift"), 
                            track);
                }
                result.close();
                stmt.close();
                conn.close();
            } catch (SQLException e) {
                this.karma.log.warning("Error while getting " + playerName + ". " + e.toString());
            }
        }
        return karmaPlayer;
    }

    public void put(KarmaPlayer karmaPlayer) {
        if (this.sqlite()) {
            boolean exists = this.exists(karmaPlayer.getName());
            try {
                Connection conn = this.getConnection();
                Statement stat = conn.createStatement();
                if (exists) {
                    // update
// Tom's bad code                   
//                    stat.executeUpdate(
//                            "update players set karma=" + karmaPlayer.getKarmaPoints()
//                            + ", lastactive=" + karmaPlayer.getLastActivityTime()
//                            + ", lastgift=" + karmaPlayer.getLastGiftTime()
//                            + " where name='" + karmaPlayer.getName() + "'");
                    // Cma's good code
                    PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE players SET karma = ?, lastactive = ?, lastgift = ?, track = ? WHERE name = ?");
                    pstmt.setInt(1, karmaPlayer.getKarmaPoints());
                    pstmt.setLong(2, karmaPlayer.getLastActivityTime());
                    pstmt.setLong(3, karmaPlayer.getLastGiftTime());
                    //TODO: Don't Ever store anything in a database as a String unless you are absolutly forced to cma, assign numbers to the tracks... -grin
                    pstmt.setLong(4, karmaPlayer.getTrack().getName().hashCode());
                    pstmt.setString(5, karmaPlayer.getName());
                    pstmt.executeUpdate();
                    // See, much better!
                } else {
                    // insert
                    PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO players (name, karma, lastactive, lastgift, track) VALUES (?, ?, ?, ?, ?)");
                    pstmt.setString(1, karmaPlayer.getName());
                    pstmt.setInt(2, karmaPlayer.getKarmaPoints());
                    pstmt.setLong(3, karmaPlayer.getLastActivityTime());
                    pstmt.setLong(4, karmaPlayer.getLastGiftTime());
                    pstmt.setLong(5, karmaPlayer.getTrack().getName().hashCode());
                    pstmt.executeUpdate();

//                    stat.executeUpdate(
//                            "insert into players values ('" + karmaPlayer.getName() + "', "
//                            + karmaPlayer.getKarmaPoints() + ", " + karmaPlayer.getLastActivityTime() + ", " + karmaPlayer.getLastGiftTime() + ", 0)");
                }
                stat.close();
                conn.close();
            } catch (SQLException e) {
                if (exists) {
                    this.karma.log.warning("Error while updating " + karmaPlayer.getName() + ". " + e.toString());
                } else {
                    this.karma.log.warning("Error while inserting " + karmaPlayer.getName() + ". " + e.toString());
                }
            }
        }
    }

    public void putAll() {
        for (String playerName : this.karma.getPlayers().keySet()) {
            KarmaPlayer player = this.karma.getPlayers().get(playerName);
            this.put(player);
        }
    }

    private void addColumn(Connection connection, String newColumn) {
        boolean updatedSchema = true;
        try {
            Statement alterStatement = connection.createStatement();
            alterStatement.executeUpdate("alter table players add column " + newColumn);
        } catch (SQLException e) {
            updatedSchema = false;
        }

        if (updatedSchema) {
            this.karma.log.info("Table schema updated to add " + newColumn + ".");
        }
    }

    private boolean sqlite() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            this.karma.log.warning("You need SQLite to run this plugin.");
            return false;
        }
        return true;
    }
    private boolean sqlite = true; // false for mysql

    private Connection getConnection() throws SQLException {
        if (sqlite) {
            return DriverManager.getConnection("jdbc:sqlite:plugins/Karma/"
                    + karma.config.getString("sqlite.database"));
        } else {
            return DriverManager.getConnection("jdbc:mysql://"
                    + karma.config.getString("mysql.host")
                    + "/" + karma.config.getString("mysql.database")
                    + "?user=" + karma.config.getString("mysql.username")
                    + "&password=" + karma.config.getString("mysql.password"));
        }
    }
}
