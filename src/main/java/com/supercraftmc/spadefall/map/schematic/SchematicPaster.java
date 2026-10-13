package com.supercraftmc.spadefall.map.schematic;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.map.Marker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pastes a schematic into the world a budgeted number of blocks per tick, so
 * a quarter-million-block crater lands in a few seconds without freezing the
 * server. No WorldEdit needed.
 *
 * Spadefall marker blocks are NOT placed. They're recorded with their world
 * position and handed back so the map can be built from them directly - the
 * structure block itself would just be a solid lump in the way.
 *
 * Palette strings are parsed once each with Bukkit.createBlockData; a state
 * the server doesn't know (a block from a newer version, say) falls back to
 * stone rather than aborting the whole paste.
 */
public final class SchematicPaster {

    private final SpadefallPlugin plugin;

    public SchematicPaster(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    public static final class Result {
        public final Location origin;
        public final List<Marker> markers;
        public final int placed;
        public final int unknownStates;

        Result(Location origin, List<Marker> markers, int placed, int unknownStates) {
            this.origin = origin;
            this.markers = markers;
            this.placed = placed;
            this.unknownStates = unknownStates;
        }
    }

    public void paste(Schematic schematic, Location origin, Consumer<Integer> onProgress,
                      Consumer<Result> onComplete, Consumer<String> onFailure) {
        World world = origin.getWorld();
        if (world == null) {
            onFailure.accept("No world");
            return;
        }
        int ox = origin.getBlockX(), oy = origin.getBlockY(), oz = origin.getBlockZ();
        if (oy < world.getMinHeight() || oy + schematic.getHeight() > world.getMaxHeight()) {
            onFailure.accept("Schematic doesn't fit vertically here (needs Y " + oy + " to "
                    + (oy + schematic.getHeight()) + ", world allows " + world.getMinHeight()
                    + " to " + world.getMaxHeight() + ")");
            return;
        }

        // Resolve the palette once.
        String[] palette = schematic.getPalette();
        BlockData[] resolved = new BlockData[palette.length];
        boolean[] isMarker = new boolean[palette.length];
        int unknown = 0;
        BlockData fallback = Material.STONE.createBlockData();
        for (int i = 0; i < palette.length; i++) {
            String state = palette[i];
            if (state == null) { resolved[i] = fallback; continue; }
            if (state.startsWith("minecraft:structure_block")) isMarker[i] = true;
            try {
                resolved[i] = Bukkit.createBlockData(state);
            } catch (IllegalArgumentException ex) {
                resolved[i] = fallback;
                unknown++;
                plugin.getLogger().warning("Unknown block state in schematic, using stone: " + state);
            }
        }
        final int unknownStates = unknown;

        // Markers go to world coordinates.
        List<Marker> markers = new ArrayList<>();
        for (Marker m : schematic.getMarkers()) {
            markers.add(new Marker(m.getRole(), ox + m.getX(), oy + m.getY(), oz + m.getZ(), m.getValue()));
        }

        int budget = Math.max(500, plugin.getConfig().getInt("maps.paste-blocks-per-tick", 8000));
        int[] blocks = schematic.getBlocks();
        int w = schematic.getWidth(), h = schematic.getHeight(), l = schematic.getLength();
        long total = schematic.getVolume();
        BlockData air = Material.AIR.createBlockData();

        new BukkitRunnable() {
            int index = 0;
            int placed = 0;
            int lastPercent = -1;

            @Override
            public void run() {
                int done = 0;
                while (done < budget && index < blocks.length) {
                    int x = index % w;
                    int z = (index / w) % l;
                    int y = index / (w * l);
                    int p = blocks[index];
                    BlockData data = (p >= 0 && p < resolved.length) ? resolved[p] : fallback;
                    if (p >= 0 && p < isMarker.length && isMarker[p]) {
                        data = air; // record only, never place
                    }
                    world.getBlockAt(ox + x, oy + y, oz + z).setBlockData(data, false);
                    placed++;
                    index++;
                    done++;
                }
                if (onProgress != null) {
                    int percent = (int) ((index * 100L) / total);
                    if (percent / 10 != lastPercent / 10) {
                        lastPercent = percent;
                        onProgress.accept(percent);
                    }
                }
                if (index >= blocks.length) {
                    cancel();
                    onComplete.accept(new Result(origin, markers, placed, unknownStates));
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
