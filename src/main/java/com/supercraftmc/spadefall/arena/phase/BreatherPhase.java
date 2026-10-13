package com.supercraftmc.spadefall.arena.phase;

import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.arena.ArenaState;
import com.supercraftmc.spadefall.game.RoundState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/** A few seconds between maps so placements can sink in. */
public final class BreatherPhase implements Phase {

    private final int nextMapIndex;
    private int remaining;

    public BreatherPhase(int nextMapIndex) {
        this.nextMapIndex = nextMapIndex;
    }

    @Override
    public ArenaState getState() {
        return ArenaState.BREATHER;
    }

    @Override
    public void onEnter(Arena arena) {
        remaining = arena.getPlugin().getConfig().getInt("game.breather-seconds", 5);
        RoundState round = arena.getRound();
        arena.broadcast("&7Standings:");
        if (round != null) {
            int n = 0;
            for (Map.Entry<UUID, Integer> e : round.standings(arena.getPlayerIds())) {
                if (n++ >= 3) break;
                Player p = Bukkit.getPlayer(e.getKey());
                arena.broadcast("&7  " + n + ". &f" + (p == null ? "?" : p.getName()) + " &a" + e.getValue());
            }
        }
        arena.title("&e&lMap " + (nextMapIndex + 1), "&7in " + remaining + "s", 5, 40, 10);
    }

    @Override
    public Phase tick(Arena arena) {
        remaining--;
        return remaining <= 0 ? new DescentPhase(nextMapIndex) : null;
    }

    @Override
    public void onExit(Arena arena) { }

    @Override
    public int getRemainingSeconds() {
        return remaining;
    }
}
