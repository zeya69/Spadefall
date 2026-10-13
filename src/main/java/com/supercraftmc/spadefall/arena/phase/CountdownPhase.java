package com.supercraftmc.spadefall.arena.phase;

import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.arena.ArenaState;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Counting down to the first descent. Shortens when the arena fills, cancels
 * if the count drops below minimum, and hands off to the round when it hits
 * zero - exactly once.
 */
public final class CountdownPhase implements Phase {

    private int remaining;
    private boolean shortened;

    public CountdownPhase(int seconds) {
        this.remaining = Math.max(1, seconds);
    }

    @Override
    public ArenaState getState() {
        return ArenaState.COUNTDOWN;
    }

    @Override
    public void onEnter(Arena arena) {
        arena.broadcast("&aEnough players. Starting in &f" + remaining + "&as.");
    }

    @Override
    public Phase tick(Arena arena) {
        if (arena.getPlayerCount() < arena.getMinPlayers() && !arena.isFull()) {
            arena.broadcast("&cNot enough players. Countdown cancelled.");
            return new WaitingPhase();
        }

        if (!shortened && arena.isFull()) {
            int quick = arena.getPlugin().getConfigManager().getCountdownSecondsWhenFull();
            if (quick < remaining) {
                remaining = quick;
                shortened = true;
                arena.broadcast("&aArena is full. Starting in &f" + remaining + "&as.");
            }
        }

        remaining--;

        if (remaining <= 0) {
            arena.startRound();
            return null; // startRound transitions; returning null avoids a double transition
        }

        if (remaining <= 5) {
            arena.title("&e" + remaining, "", 0, 25, 5);
            for (Player p : arena.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
            }
        } else if (remaining % 15 == 0) {
            arena.broadcast("&7Starting in &f" + remaining + "&7s");
        }
        return null;
    }

    @Override
    public void onExit(Arena arena) { }

    @Override
    public int getRemainingSeconds() {
        return remaining;
    }
}
