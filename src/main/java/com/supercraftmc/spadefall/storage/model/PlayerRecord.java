package com.supercraftmc.spadefall.storage.model;

import java.util.UUID;

/**
 * A player's persistent row.
 *
 * {@code chips} here is the BANKED balance only. Unbanked chips are round
 * state and deliberately never reach the database - see the design note in
 * the spec, section 8.
 */
public final class PlayerRecord {

    private final UUID uuid;
    private String name;
    private long chips;
    private final long firstSeen;
    private long lastSeen;

    public PlayerRecord(UUID uuid, String name, long chips, long firstSeen, long lastSeen) {
        this.uuid = uuid;
        this.name = name;
        this.chips = chips;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
    }

    public static PlayerRecord fresh(UUID uuid, String name) {
        long now = System.currentTimeMillis();
        return new PlayerRecord(uuid, name, 0L, now, now);
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getChips() { return chips; }
    public void setChips(long chips) { this.chips = Math.max(0L, chips); }
    public long getFirstSeen() { return firstSeen; }
    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
}
