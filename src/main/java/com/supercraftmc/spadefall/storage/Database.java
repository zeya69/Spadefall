package com.supercraftmc.spadefall.storage;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Connection pool plus schema management.
 *
 * SQLite is the default so a fresh install needs zero setup; MySQL is a config
 * switch for anyone running a network. The DAO layer above this does not know
 * or care which is active - the only difference that leaks upward is
 * {@link #isMySql()}, used for the handful of places the dialects differ.
 */
public final class Database {

    private final SpadefallPlugin plugin;
    private HikariDataSource source;
    private boolean mySql;

    public Database(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() throws StorageException {
        FileConfiguration c = plugin.getConfig();
        String type = c.getString("storage.type", "sqlite").toLowerCase();
        this.mySql = type.equals("mysql");

        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("Spadefall-Pool");

        if (mySql) {
            String host = c.getString("storage.mysql.host", "localhost");
            int port = c.getInt("storage.mysql.port", 3306);
            String db = c.getString("storage.mysql.database", "spadefall");
            boolean ssl = c.getBoolean("storage.mysql.useSSL", false);

            hikari.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db
                    + "?useSSL=" + ssl + "&allowPublicKeyRetrieval=true"
                    + "&characterEncoding=utf8&serverTimezone=UTC");
            hikari.setUsername(c.getString("storage.mysql.username", "spadefall"));
            hikari.setPassword(c.getString("storage.mysql.password", ""));
            hikari.setMaximumPoolSize(c.getInt("storage.mysql.pool-size", 6));
        } else {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                throw new StorageException("Could not create the plugin data folder");
            }
            File file = new File(dataFolder, c.getString("storage.file", "data.db"));
            hikari.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
            // SQLite is a single file; a pool larger than one invites lock contention.
            hikari.setMaximumPoolSize(1);
        }

        hikari.setConnectionTimeout(10_000L);
        hikari.setLeakDetectionThreshold(30_000L);

        try {
            this.source = new HikariDataSource(hikari);
        } catch (RuntimeException ex) {
            throw new StorageException("Could not open the database: " + ex.getMessage(), ex);
        }

        createSchema();
    }

    private void createSchema() throws StorageException {
        String autoInc = mySql ? "BIGINT AUTO_INCREMENT PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT";

        String[] statements = {
            "CREATE TABLE IF NOT EXISTS sf_players (" +
                "uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
                "name VARCHAR(16) NOT NULL," +
                "chips BIGINT NOT NULL DEFAULT 0," +
                "first_seen BIGINT NOT NULL," +
                "last_seen BIGINT NOT NULL)",

            "CREATE TABLE IF NOT EXISTS sf_stats (" +
                "uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
                "games INT NOT NULL DEFAULT 0," +
                "wins INT NOT NULL DEFAULT 0," +
                "dm_wins INT NOT NULL DEFAULT 0," +
                "descents_won INT NOT NULL DEFAULT 0," +
                "total_score BIGINT NOT NULL DEFAULT 0," +
                "best_score INT NOT NULL DEFAULT 0," +
                "spades INT NOT NULL DEFAULT 0," +
                "chips_earned BIGINT NOT NULL DEFAULT 0," +
                "blackjacks INT NOT NULL DEFAULT 0," +
                "playtime BIGINT NOT NULL DEFAULT 0)",

            "CREATE TABLE IF NOT EXISTS sf_perks (" +
                "uuid VARCHAR(36) NOT NULL," +
                "perk_id VARCHAR(64) NOT NULL," +
                "qty INT NOT NULL DEFAULT 0," +
                "PRIMARY KEY (uuid, perk_id))",

            "CREATE TABLE IF NOT EXISTS sf_history (" +
                "id " + autoInc + "," +
                "arena VARCHAR(64) NOT NULL," +
                "started BIGINT NOT NULL," +
                "ended BIGINT NOT NULL," +
                "winner_uuid VARCHAR(36)," +
                "scores_json TEXT)",

            "CREATE INDEX IF NOT EXISTS idx_sf_history_ended ON sf_history (ended)"
        };

        try (Connection conn = source.getConnection(); Statement st = conn.createStatement()) {
            for (String sql : statements) {
                st.executeUpdate(sql);
            }
        } catch (SQLException ex) {
            throw new StorageException("Could not create the database schema", ex);
        }
    }

    /** Drops match history older than the configured retention window. */
    public int pruneHistory(int retentionDays) throws StorageException {
        long cutoff = System.currentTimeMillis() - (retentionDays * 86_400_000L);
        try (Connection conn = source.getConnection();
             var ps = conn.prepareStatement("DELETE FROM sf_history WHERE ended < ?")) {
            ps.setLong(1, cutoff);
            return ps.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("Could not prune match history", ex);
        }
    }

    public Connection getConnection() throws SQLException {
        if (source == null || source.isClosed()) {
            throw new SQLException("The database is not connected");
        }
        return source.getConnection();
    }

    public boolean isMySql() {
        return mySql;
    }

    public boolean isConnected() {
        return source != null && !source.isClosed();
    }

    public void close() {
        if (source != null && !source.isClosed()) {
            source.close();
        }
    }
}
