package com.supercraftmc.spadefall.storage.dao;

import com.supercraftmc.spadefall.storage.Database;
import com.supercraftmc.spadefall.storage.StorageException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/** Writes per-round stat deltas. Blocking - call off the main thread. */
public final class StatsDao {

    private final Database database;

    public StatsDao(Database database) {
        this.database = database;
    }

    public void recordRound(UUID uuid, boolean won, int descentsWon, int score, int chips, long playtimeMs)
            throws StorageException {
        String insert = "INSERT INTO sf_stats (uuid, games, wins, descents_won, total_score, best_score, chips_earned, playtime) "
                + "VALUES (?, 1, ?, ?, ?, ?, ?, ?)";
        String onDup = database.isMySql()
                ? " ON DUPLICATE KEY UPDATE games=games+1, wins=wins+VALUES(wins), descents_won=descents_won+VALUES(descents_won), "
                  + "total_score=total_score+VALUES(total_score), best_score=GREATEST(best_score, VALUES(best_score)), "
                  + "chips_earned=chips_earned+VALUES(chips_earned), playtime=playtime+VALUES(playtime)"
                : " ON CONFLICT(uuid) DO UPDATE SET games=games+1, wins=wins+excluded.wins, descents_won=descents_won+excluded.descents_won, "
                  + "total_score=total_score+excluded.total_score, best_score=MAX(best_score, excluded.best_score), "
                  + "chips_earned=chips_earned+excluded.chips_earned, playtime=playtime+excluded.playtime";

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(insert + onDup)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, won ? 1 : 0);
            ps.setInt(3, descentsWon);
            ps.setLong(4, score);
            ps.setInt(5, score);
            ps.setLong(6, chips);
            ps.setLong(7, playtimeMs);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("Could not record round stats for " + uuid, ex);
        }
    }
}
