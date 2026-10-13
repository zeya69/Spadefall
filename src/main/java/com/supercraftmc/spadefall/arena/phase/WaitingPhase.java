package com.supercraftmc.spadefall.arena.phase;

import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.arena.ArenaState;

/**
 * Idling in the lobby.
 *
 * With auto-start on (the default) the countdown begins at min-players. With
 * it off, the arena waits until it is full or an admin runs /sf start - for
 * owners who want a 24-slot arena to actually fill before it goes.
 */
public final class WaitingPhase implements Phase {

    @Override
    public ArenaState getState() {
        return ArenaState.WAITING;
    }

    @Override
    public void onEnter(Arena arena) {
        if (arena.getPlayerCount() > 0) {
            arena.broadcast("&7Waiting for players... &f" + arena.getPlayerCount()
                    + "&7/&f" + arena.getMinPlayers());
        }
    }

    @Override
    public Phase tick(Arena arena) {
        if (arena.isFull()) {
            return new CountdownPhase(arena.getPlugin().getConfigManager().getCountdownSecondsWhenFull());
        }
        if (arena.isAutoStart() && arena.getPlayerCount() >= arena.getMinPlayers()) {
            return new CountdownPhase(arena.getCountdownSeconds());
        }
        return null;
    }

    @Override
    public void onExit(Arena arena) { }
}
