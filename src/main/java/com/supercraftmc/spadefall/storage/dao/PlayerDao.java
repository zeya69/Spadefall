package com.supercraftmc.spadefall.storage.dao;

import com.supercraftmc.spadefall.storage.Database;
import com.supercraftmc.spadefall.storage.StorageException;
import com.supercraftmc.spadefall.storage.model.PlayerRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Reads and writes {@link PlayerRecord}. Every method here blocks, so callers
 * must be off the main thread.
 */
public final class PlayerDao {

    private final Database database;

    public PlayerDao(Database database) {
        this.database = database;
    }

    public PlayerRecord load(UUID uuid) throws StorageException {
        String sql = "SELECT name, chips, first_seen, last_seen FROM sf_players WHERE uuid = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new PlayerRecord(uuid, rs.getString("name"), rs.getLong("chips"),
                        rs.getLong("first_seen"), rs.getLong("last_seen"));
            }
        } catch (SQLException ex) {
            throw new StorageException("Could not load player " + uuid, ex);
        }
    }

    public PlayerRecord loadOrCreate(UUID uuid, String name) throws StorageException {
        PlayerRecord existing = load(uuid);
        if (existing != null) {
            if (!existing.getName().equals(name)) {
                existing.setName(name);
            }
            existing.setLastSeen(System.currentTimeMillis());
            save(existing);
            return existing;
        }
        PlayerRecord fresh = PlayerRecord.fresh(uuid, name);
        save(fresh);
        return fresh;
    }

    public void save(PlayerRecord record) throws StorageException {
        String sql = database.isMySql()
                ? "INSERT INTO sf_players (uuid, name, chips, first_seen, last_seen) VALUES (?,?,?,?,?) "
                  + "ON DUPLICATE KEY UPDATE name=VALUES(name), chips=VALUES(chips), last_seen=VALUES(last_seen)"
                : "INSERT INTO sf_players (uuid, name, chips, first_seen, last_seen) VALUES (?,?,?,?,?) "
                  + "ON CONFLICT(uuid) DO UPDATE SET name=excluded.name, chips=excluded.chips, last_seen=excluded.last_seen";

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, record.getUuid().toString());
            ps.setString(2, record.getName());
            ps.setLong(3, record.getChips());
            ps.setLong(4, record.getFirstSeen());
            ps.setLong(5, record.getLastSeen());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("Could not save player " + record.getUuid(), ex);
        }
    }
}
