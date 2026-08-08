package com.supercraftmc.spadefall.map;

import java.util.Locale;

/**
 * The roles a marker can declare.
 *
 * A marker is a structure block whose name field reads {@code spadefall:<role>},
 * optionally with a value suffix: {@code spadefall:chip:25}.
 *
 * Structure blocks were chosen over "lime wool means spawn" because they are
 * op/creative-only, nobody uses them decoratively, and the name field carries
 * unlimited roles in a single block type.
 */
public enum MarkerRole {

    SPAWN("spawn", false),
    FINISH("finish", false),
    SPADE("spade", false),
    CHIP("chip", true),
    DM_SPAWN("dm_spawn", false),
    CHEST("chest", false);

    public static final String NAMESPACE = "spadefall";

    private final String id;
    private final boolean valued;

    MarkerRole(String id, boolean valued) {
        this.id = id;
        this.valued = valued;
    }

    public String getId() {
        return id;
    }

    /** True when this role expects a numeric suffix, e.g. {@code chip:25}. */
    public boolean isValued() {
        return valued;
    }

    public static MarkerRole byId(String id) {
        if (id == null) {
            return null;
        }
        String needle = id.toLowerCase(Locale.ROOT).trim();
        for (MarkerRole role : values()) {
            if (role.id.equals(needle)) {
                return role;
            }
        }
        return null;
    }

    /**
     * Parses a structure block name into a role and value.
     *
     * @return null when the name is not one of ours - foreign structure blocks
     *         in a downloaded map must be ignored, not treated as errors.
     */
    public static ParsedName parse(String structureName) {
        if (structureName == null || structureName.isEmpty()) {
            return null;
        }
        String[] parts = structureName.toLowerCase(Locale.ROOT).trim().split(":");
        if (parts.length < 2 || !parts[0].equals(NAMESPACE)) {
            return null;
        }
        MarkerRole role = byId(parts[1]);
        if (role == null) {
            return null;
        }
        int value = 0;
        if (parts.length >= 3) {
            try {
                value = Integer.parseInt(parts[2]);
            } catch (NumberFormatException ignored) {
                // A malformed value is a warning at validation time, not a parse failure.
                value = -1;
            }
        }
        return new ParsedName(role, value);
    }

    public String toStructureName(int value) {
        return valued ? NAMESPACE + ":" + id + ":" + value : NAMESPACE + ":" + id;
    }

    /** Result of {@link #parse(String)}. */
    public record ParsedName(MarkerRole role, int value) { }
}
