package com.tommytony.karma;

import java.io.File;
import java.sql.*;

import org.bukkit.OfflinePlayer;
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
                // If the table has already been created its in sqlite so numeric is working fine for longs
                // If it hasn't been created theres a chance that they're using MySQL so create it with lastactive as a bigint
                // bigint works in SQLite too, but no difference between numeric. In MySQL, there's a large difference
                statement.executeUpdate("create table if not exists players (name text, karma numeric, lastactive bigint)");

                this.addColumn(connection, "lastgift bigint");
                // Shakes fist at sqlite no drop column
                this.addColumn(connection, "lastprize bigint");
                this.addColumn(connection, "track numeric");

                statement.close();
                connection.close();
            } catch (SQLException e) {
                this.karma.log.warning("Error while intilializing database. " + e.toString());
            }
        }

    }

    public boolean exists(OfflinePlayer player) {
        return get(player) != null;
    }

    /**
     * Get the player based on the player's name or UUID, while converting to UUID.
     * @param player Bukkit object to draw from.
     * @return karma player
     */
    public KarmaPlayer get(OfflinePlayer player) {
        if (!this.sqlite())
            throw new RuntimeException("Err... why do you not have SQLite installed?");
        try (Connection conn = this.getConnection()) {
            // First check the database to see if the user is either in name or UUID format
            boolean idExists = inlineExistsUUID(player, conn), nameExists = inlineExistsName(player, conn);
            // No results at all? quit
            if (!idExists && !nameExists)
                return null;
            // Only name exists? Do a conversion first
            if (!idExists)
                inlineConvertToUUID(player, conn);
            // Finally, query the database again for the return
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM players WHERE name = ?")) {
                stmt.setString(1, player.getUniqueId().toString());
                try (ResultSet result = stmt.executeQuery()) {
                    if (!result.next())
                        return null;
                    KarmaTrack track = karma.getTrack(result.getLong("track"));
                    if (track == null) {
                        if (player.isOnline()) {
                            // If player is online get their track normally
                            track = karma.getInitialTrack(player.getPlayer());
                        } else {
                            track = karma.getDefaultTrack();
                        }
                    }
                    return new KarmaPlayer(this.karma, player,
                            result.getInt("karma"),
                            result.getLong("lastactive"),
                            result.getLong("lastgift"),
                            track);
                }
            }
        } catch (SQLException e) {
            karma.log.warning("Failed to get karma player for " + player.toString());
            return null;
        }
    }

    public void put(KarmaPlayer karmaPlayer) {
        if (!this.sqlite())
            throw new RuntimeException("Err... why do you not have SQLite installed?");
        try (Connection conn = this.getConnection()) {
            if (inlineExistsUUID(karmaPlayer.getPlayer(), conn)) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE players SET karma = ?, lastactive = ?, lastgift = ?, track = ? WHERE name = ?")) {
                    stmt.setInt(1, karmaPlayer.getKarmaPoints());
                    stmt.setLong(2, karmaPlayer.getLastActivityTime());
                    stmt.setLong(3, karmaPlayer.getLastGiftTime());
                    stmt.setLong(4, karmaPlayer.getTrack().getName().hashCode());
                    stmt.setString(5, karmaPlayer.getPlayer().getUniqueId().toString());
                    stmt.executeUpdate();
                }
            } else {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO players (name, karma, lastactive, lastgift, track) VALUES (?, ?, ?, ?, ?)")) {
                    stmt.setString(1, karmaPlayer.getPlayer().getUniqueId().toString());
                    stmt.setInt(2, karmaPlayer.getKarmaPoints());
                    stmt.setLong(3, karmaPlayer.getLastActivityTime());
                    stmt.setLong(4, karmaPlayer.getLastGiftTime());
                    stmt.setLong(5, karmaPlayer.getTrack().getName().hashCode());
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            this.karma.log.warning("Error fixing " + karmaPlayer.toString());
        }
    }

    private void inlineConvertToUUID(OfflinePlayer player, Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE players SET name = ? WHERE name = ?"
        )) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, player.getName());
            stmt.executeUpdate();
        }
    }

    private boolean inlineExistsUUID(OfflinePlayer player, Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM players WHERE name = ?"
        )) {
            stmt.setString(1, player.getUniqueId().toString());
            try (ResultSet result = stmt.executeQuery()) {
                return result.next();
            }
        }
    }

    private boolean inlineExistsName(OfflinePlayer player, Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM players WHERE name = ?"
        )) {
            stmt.setString(1, player.getName());
            try (ResultSet result = stmt.executeQuery()) {
                return result.next();
            }
        }
    }

    public boolean existsName(OfflinePlayer player) {
        try (Connection conn = this.getConnection()) {
            return inlineExistsName(player, conn);
        } catch (SQLException e) {
            this.karma.log.warning("Error finding " + player.toString());
            return false;
        }
    }

    public boolean existsUUID(OfflinePlayer player) {
        try (Connection conn = this.getConnection()) {
            return inlineExistsUUID(player, conn);
        } catch (SQLException e) {
            this.karma.log.warning("Error finding " + player.toString());
            return false;
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
