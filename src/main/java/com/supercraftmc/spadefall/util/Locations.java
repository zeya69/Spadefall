package com.supercraftmc.spadefall.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

/** Small location helpers. */
public final class Locations {

    private Locations() { }

    /**
     * Drops a location to the nearest solid block below it, up to {@code maxDrop}
     * blocks. Setting a lobby while flying was a trap - this is the fix.
     * Returns the input unchanged if nothing solid is found.
     */
    public static Location snapToGround(Location location, int maxDrop) {
        World world = location.getWorld();
        if (world == null) return location;
        Location probe = location.clone();
        for (int i = 0; i <= maxDrop; i++) {
            Block below = probe.clone().subtract(0, 1, 0).getBlock();
            if (below.getType().isSolid()) {
                Location out = location.clone();
                out.setY(below.getY() + 1.0D);
                return out;
            }
            probe.subtract(0, 1, 0);
            if (probe.getY() < world.getMinHeight()) break;
        }
        return location;
    }
}
