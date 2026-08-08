package com.supercraftmc.spadefall.arena.phase;

import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.arena.ArenaState;

/**
 * Counting down to the first descent.
 *
 * Two behaviours worth noting. The timer shortens when the arena fills, because
 * a full lobby waiting 45 seconds for nothing is how players wander off. And it
 * cancels straight back to WAITING if the count drops below the minimum, rather
 * than starting a round that cannot be played.
 */
public final class CountdownPhase implements Phase {

    private int remaining;
    private boolean shortened;

    public CountdownPhase(int seconds) {
        this.remaining = seconds;
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
        if (arena.getPlayerCount() < arena.getMinPlayers()) {
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
            // Slice 2 replaces this with DescentPhase.
            arena.broadcast("&e&lThe descent would begin here. &7(gameplay lands in slice 2)");
            return null;
        }

        if (remaining <= 5 || remaining % 15 == 0) {
            arena.broadcast("&7Starting in &f" + remaining + "&7s");
        }
        return null;
    }

    @Override
    public void onExit(Arena arena) {
        // nothing to unwind
    }

    @Override
    public int getRemainingSeconds() {
        return remaining;
    }
}
