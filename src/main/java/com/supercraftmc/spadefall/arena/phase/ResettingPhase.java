package com.supercraftmc.spadefall.arena.phase;

import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.arena.ArenaState;

/**
 * Tears the round down. Everyone goes back where they came from with their
 * own inventory, pickups are cleared, and the arena returns to WAITING.
 * Nothing in the world was changed by a round, so there is no block restore.
 */
public final class ResettingPhase implements Phase {

    @Override
    public ArenaState getState() {
        return ArenaState.RESETTING;
    }

    @Override
    public void onEnter(Arena arena) {
        arena.broadcast("&7Thanks for playing. &f/sf join&7 to go again.");
        arena.stop(null);
    }

    @Override
    public Phase tick(Arena arena) {
        return null; // stop() already moved us to WAITING
    }

    @Override
    public void onExit(Arena arena) { }
}
