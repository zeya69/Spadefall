package com.supercraftmc.spadefall.arena.phase;

import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.arena.ArenaState;

/**
 * Idling in the lobby until enough players show up.
 */
public final class WaitingPhase implements Phase {

    @Override
    public ArenaState getState() {
        return ArenaState.WAITING;
    }

    @Override
    public void onEnter(Arena arena) {
        arena.broadcast("&7Waiting for players... &f" + arena.getPlayerCount()
                + "&7/&f" + arena.getMinPlayers());
    }

    @Override
    public Phase tick(Arena arena) {
        if (arena.getPlayerCount() >= arena.getMinPlayers()) {
            return new CountdownPhase(arena.getPlugin().getConfigManager().getCountdownSeconds());
        }
        return null;
    }

    @Override
    public void onExit(Arena arena) {
        // nothing to unwind
    }
}
