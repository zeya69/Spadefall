package com.supercraftmc.spadefall.map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

/**
 * One placed marker: a role, a position, and an optional value.
 *
 * Positions are stored relative to nothing in particular - they are absolute
 * world coordinates for a region-sourced map, and will become origin-relative
 * offsets when schematic sourcing lands in a later slice.
 */
public final class Marker {

    private final MarkerRole role;
    private final int x;
    private final int y;
    private final int z;
    private final int value;

    public Marker(MarkerRole role, int x, int y, int z, int value) {
        this.role = Objects.requireNonNull(role, "role");
        this.x = x;
        this.y = y;
        this.z = z;
        this.value = value;
    }

    public MarkerRole getRole() { return role; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public int getValue() { return value; }

    public Location toLocation(World world) {
        // Centre of the block, standing on top of it - correct for spawns.
        return new Location(world, x + 0.5D, y, z + 0.5D);
    }

    public void write(ConfigurationSection section) {
        section.set("role", role.getId());
        section.set("x", x);
        section.set("y", y);
        section.set("z", z);
        if (role.isValued()) {
            section.set("value", value);
        }
    }

    public static Marker read(ConfigurationSection section) {
        MarkerRole role = MarkerRole.byId(section.getString("role"));
        if (role == null) {
            return null;
        }
        return new Marker(role,
                section.getInt("x"),
                section.getInt("y"),
                section.getInt("z"),
                section.getInt("value", 0));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Marker marker)) return false;
        return x == marker.x && y == marker.y && z == marker.z && role == marker.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, x, y, z);
    }

    @Override
    public String toString() {
        return role.getId() + "@" + x + "," + y + "," + z + (role.isValued() ? ":" + value : "");
    }
}
