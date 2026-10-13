package com.supercraftmc.spadefall.arena.phase;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.arena.ArenaState;
import com.supercraftmc.spadefall.game.RoundState;
import com.supercraftmc.spadefall.storage.StorageException;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Round over. Show standings, bank everyone's chips, write stats, then reset.
 *
 * Shop, deathmatch and the Spade Table slot in before this phase in later
 * slices; the round currently ends after the last descent.
 */
public final class ResultsPhase implements Phase {

    private int remaining;

    @Override
    public ArenaState getState() {
        return ArenaState.RESULTS;
    }

    @Override
    public void onEnter(Arena arena) {
        SpadefallPlugin plugin = arena.getPlugin();
        RoundState round = arena.getRound();
        remaining = plugin.getConfig().getInt("game.results-seconds", 10);
        if (round == null) {
            arena.stop(null);
            return;
        }

        List<Map.Entry<UUID, Integer>> standings = round.standings(arena.getPlayerIds());
        UUID winner = standings.isEmpty() ? null : standings.get(0).getKey();
        Player winnerPlayer = winner == null ? null : Bukkit.getPlayer(winner);
        String winnerName = winnerPlayer == null ? "nobody" : winnerPlayer.getName();

        arena.broadcast("");
        arena.broadcast("&6&l\u2660 " + winnerName + " &ewins the round!");
        int n = 0;
        for (Map.Entry<UUID, Integer> e : standings) {
            if (n++ >= 5) break;
            Player p = Bukkit.getPlayer(e.getKey());
            arena.broadcast("&7  " + n + ". &f" + (p == null ? "?" : p.getName()) + " &a" + e.getValue()
                    + " &8\u00B7 &6" + round.getChipsCollected(e.getKey()) + " chips");
        }
        arena.title("&6&l\u2660 " + winnerName, "&ewins", 10, 60, 20);
        for (Player p : arena.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }

        // Bank chips and write stats, off the main thread.
        Map<UUID, Integer> banked = round.drainUnbanked();
        long startedAt = round.getStartedAt();
        List<UUID> mapWinners = round.getMapWinners();
        Map<UUID, Integer> scores = new java.util.HashMap<>();
        for (UUID uuid : arena.getPlayerIds()) scores.put(uuid, round.getScore(uuid));
        Map<UUID, String> names = new java.util.HashMap<>();
        for (Player p : arena.getOnlinePlayers()) names.put(p.getUniqueId(), p.getName());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (UUID uuid : scores.keySet()) {
                try {
                    int chips = banked.getOrDefault(uuid, 0);
                    int descentsWon = (int) mapWinners.stream().filter(uuid::equals).count();
                    boolean won = uuid.equals(winner);
                    plugin.getPlayerDao().addChips(uuid, names.getOrDefault(uuid, "?"), chips);
                    plugin.getStatsDao().recordRound(uuid, won, descentsWon, scores.get(uuid), chips,
                            System.currentTimeMillis() - startedAt);
                } catch (StorageException ex) {
                    plugin.getLogger().warning("Could not save round results for " + uuid + ": " + ex.getMessage());
                }
            }
        });

        for (Player p : arena.getOnlinePlayers()) {
            int chips = banked.getOrDefault(p.getUniqueId(), 0);
            if (chips > 0) {
                p.sendMessage(com.supercraftmc.spadefall.config.Messages.colour("&6+" + chips + " chips &7banked."));
            }
        }
    }

    @Override
    public Phase tick(Arena arena) {
        remaining--;
        return remaining <= 0 ? new ResettingPhase() : null;
    }

    @Override
    public void onExit(Arena arena) { }

    @Override
    public int getRemainingSeconds() {
        return remaining;
    }
}
