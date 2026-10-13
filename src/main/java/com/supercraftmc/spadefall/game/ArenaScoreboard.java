package com.supercraftmc.spadefall.game;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.arena.ArenaState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The sidebar. One board per player, rebuilt once a second by the arena tick,
 * because the "your score" line differs per viewer.
 */
public final class ArenaScoreboard {

    private final SpadefallPlugin plugin;

    public ArenaScoreboard(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    public void update(Arena arena) {
        for (Player player : arena.getOnlinePlayers()) {
            render(arena, player);
        }
    }

    public void remove(Player player) {
        if (Bukkit.getScoreboardManager() != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private void render(Arena arena, Player player) {
        if (Bukkit.getScoreboardManager() == null) return;
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("spadefall", "dummy", "\u00A7a\u00A7l\u2660 \u00A7fSPADEFALL");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = new ArrayList<>();
        ArenaState state = arena.getState();
        RoundState round = arena.getRound();
        int remaining = arena.getPhase() == null ? -1 : arena.getPhase().getRemainingSeconds();

        lines.add("\u00A77" + arena.getId());
        lines.add("");

        switch (state) {
            case WAITING -> {
                lines.add("\u00A7fWaiting " + "\u00A78" + arena.getPlayerCount() + "/" + arena.getMinPlayers());
                lines.add("\u00A77need " + Math.max(0, arena.getMinPlayers() - arena.getPlayerCount()) + " more");
            }
            case COUNTDOWN -> {
                lines.add("\u00A7fStarting in \u00A7a" + remaining + "s");
                lines.add("\u00A77players \u00A7f" + arena.getPlayerCount() + "\u00A78/" + arena.getMaxPlayers());
            }
            case DESCENT -> {
                lines.add("\u00A7fMap \u00A7a" + (round == null ? 1 : round.getMapIndex() + 1)
                        + "\u00A77/" + arena.getMapNames().size());
                lines.add("\u00A7fTime \u00A7e" + fmt(remaining));
                lines.add("");
                if (round != null) {
                    lines.add("\u00A7fScore \u00A7a" + round.getScore(player.getUniqueId()));
                    lines.add("\u00A7fChips \u00A76" + round.getUnbankedChips(player.getUniqueId()));
                    lines.add("");
                    lines.add("\u00A7fDown \u00A77" + round.getFinishedThisMap().size()
                            + "/" + arena.getActiveCount());
                }
            }
            case BREATHER, RESULTS, INTERMISSION, DEATHMATCH, TABLE, RESETTING -> {
                lines.add("\u00A7f" + state.getDisplay());
                if (remaining >= 0) lines.add("\u00A7e" + fmt(remaining));
                if (round != null) {
                    lines.add("");
                    int n = 0;
                    for (Map.Entry<UUID, Integer> e : round.standings(arena.getPlayerIds())) {
                        if (n++ >= 3) break;
                        Player p = Bukkit.getPlayer(e.getKey());
                        String name = p == null ? "?" : p.getName();
                        lines.add("\u00A77" + n + ". \u00A7f" + name + " \u00A7a" + e.getValue());
                    }
                }
            }
            default -> lines.add("\u00A77" + state.getDisplay());
        }

        lines.add("");
        lines.add("\u00A7asupercraft-mc.com");

        // Sidebar entries must be unique strings; pad duplicates with colour codes.
        int score = lines.size();
        java.util.Set<String> used = new java.util.HashSet<>();
        for (String line : lines) {
            String entry = line;
            while (used.contains(entry) || entry.isEmpty()) {
                entry = entry + "\u00A7r";
            }
            used.add(entry);
            obj.getScore(entry).setScore(score--);
        }
        player.setScoreboard(board);
    }

    private String fmt(int seconds) {
        if (seconds < 0) return "--";
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
