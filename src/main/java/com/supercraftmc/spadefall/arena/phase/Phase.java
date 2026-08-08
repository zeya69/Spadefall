package com.supercraftmc.spadefall.arena.phase;

import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.arena.ArenaState;

/**
 * One step of the round machine.
 *
 * Phases are ticked once per second rather than once per game tick. Nothing in
 * the round loop needs 20Hz resolution, and a one-second tick keeps timers
 * readable and cheap.
 */
public interface Phase {

    /** The state this phase represents. */
    ArenaState getState();

    /** Called once when the phase begins. */
    void onEnter(Arena arena);

    /**
     * Called once per second.
     *
     * @return the next phase to move to, or null to stay in this one
     */
    Phase tick(Arena arena);

    /** Called once when leaving, including on a forced stop. */
    void onExit(Arena arena);

    /** Seconds remaining, for scoreboards. Negative when not time-based. */
    default int getRemainingSeconds() {
        return -1;
    }
}
