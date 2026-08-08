package com.supercraftmc.spadefall.map;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.util.Cuboid;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Structure;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

/**
 * Walks a region and reads every Spadefall marker out of it.
 *
 * Block access has to happen on the main thread, and a large region is far too
 * much work for one tick - a 256x384x256 region is 25 million blocks. So the
 * scan is spread across ticks in budgeted slices, reporting progress as it
 * goes. The budget is configurable because the right value depends entirely on
 * what else the server is doing.
 *
 * Foreign structure blocks are skipped silently. A downloaded map may well
 * contain the builder's own structure blocks and those are not our business.
 */
public final class MarkerScanner {

    private final SpadefallPlugin plugin;

    public MarkerScanner(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Scans asynchronously across ticks.
     *
     * @param onProgress called with a 0-100 percentage, may be null
     * @param onComplete called on the main thread with the finished definition
     */
    public void scan(String mapName, Cuboid region,
                     Consumer<Integer> onProgress,
                     Consumer<MapDefinition> onComplete,
                     Consumer<String> onFailure) {

        World world = Bukkit.getWorld(region.getWorldName());
        if (world == null) {
            onFailure.accept("World '" + region.getWorldName() + "' is not loaded");
            return;
        }

        MapDefinition definition = new MapDefinition(mapName, region);
        int budget = plugin.getConfigManager().getScanBlocksPerTick();
        long volume = region.getVolume();

        new BukkitRunnable() {
            int x = region.getMinX();
            int y = region.getMinY();
            int z = region.getMinZ();
            long visited = 0L;
            int lastPercent = -1;

            @Override
            public void run() {
                int processed = 0;
                while (processed < budget) {
                    if (x > region.getMaxX()) {
                        finish();
                        return;
                    }

                    // Only load chunks that already exist; never generate terrain
                    // just to look for markers in it.
                    if (world.isChunkLoaded(x >> 4, z >> 4) || loadIfExists(world, x, z)) {
                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType() == Material.STRUCTURE_BLOCK) {
                            readMarker(block, definition);
                        }
                    }

                    processed++;
                    visited++;
                    advance();
                }
                reportProgress();
            }

            private void advance() {
                // y innermost: a whole vertical column lives in one chunk, so
                // the chunk lookup is amortised instead of changing every
                // sixteen blocks. Matters a lot on a tall descent map.
                y++;
                if (y > region.getMaxY()) {
                    y = region.getMinY();
                    z++;
                    if (z > region.getMaxZ()) {
                        z = region.getMinZ();
                        x++;
                    }
                }
            }

            private void reportProgress() {
                if (onProgress == null || volume == 0L) {
                    return;
                }
                int percent = (int) ((visited * 100L) / volume);
                if (percent != lastPercent && percent % 10 == 0) {
                    lastPercent = percent;
                    onProgress.accept(percent);
                }
            }

            private void finish() {
                cancel();
                onComplete.accept(definition);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private boolean loadIfExists(World world, int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        if (!world.isChunkGenerated(chunkX, chunkZ)) {
            return false;
        }
        world.getChunkAt(chunkX, chunkZ);
        return true;
    }

    private void readMarker(Block block, MapDefinition definition) {
        BlockState state = block.getState();
        if (!(state instanceof Structure structure)) {
            return;
        }
        MarkerRole.ParsedName parsed = MarkerRole.parse(structure.getStructureName());
        if (parsed == null) {
            return;
        }
        definition.add(new Marker(parsed.role(),
                block.getX(), block.getY(), block.getZ(), parsed.value()));
    }
}
