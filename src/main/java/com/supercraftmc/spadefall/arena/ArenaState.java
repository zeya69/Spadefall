package com.supercraftmc.spadefall.arena;

/**
 * The phases a round moves through.
 *
 * Slice 1 implements WAITING and COUNTDOWN and stubs the rest, so the machine
 * can be exercised end to end before any gameplay exists.
 */
public enum ArenaState {

    /** Not usable - misconfigured, or its map failed to load. */
    DISABLED("Disabled", false),

    /** Accepting players, waiting to reach the minimum. */
    WAITING("Waiting", true),

    /** Minimum reached, timer running. Still joinable. */
    COUNTDOWN("Starting", true),

    /** Racing down one of the maps. */
    DESCENT("In progress", false),

    /** Shop is open between the last descent and the deathmatch. */
    INTERMISSION("Intermission", false),

    /** Last one standing. */
    DEATHMATCH("Deathmatch", false),

    /** Spade holders playing their hands. */
    TABLE("Card table", false),

    /** Restoring the world and clearing up. */
    RESETTING("Resetting", false);

    private final String display;
    private final boolean joinable;

    ArenaState(String display, boolean joinable) {
        this.display = display;
        this.joinable = joinable;
    }

    public String getDisplay() { return display; }
    public boolean isJoinable() { return joinable; }

    public boolean isRunning() {
        return this == DESCENT || this == INTERMISSION || this == DEATHMATCH || this == TABLE;
    }
}
