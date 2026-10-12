package com.supercraftmc.spadefall.setup.glow;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.map.MapDefinition;
import com.supercraftmc.spadefall.map.Marker;
import com.supercraftmc.spadefall.map.MarkerRole;
import com.supercraftmc.spadefall.setup.SetupStep;
import com.supercraftmc.spadefall.util.Cuboid;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Makes markers visible in the world while someone is in edit mode.
 *
 * Every marker becomes a translucent stained-glass BlockDisplay glowing in its
 * role colour, with a TextDisplay label above it - "Start #3", "Chip 25".
 * The region draws as twelve thin white edges so the owner can see whether
 * the box actually covers the build.
 *
 * Display entities have no hitbox, physics or AI, which makes them the right
 * tool: they are pure visuals. They're spawned invisible-by-default and shown
 * only to players in edit mode, so a running match never sees them. They're
 * non-persistent and tagged, so a crash can't leave them in the world either -
 * {@link #sweepStrays()} removes any survivors on the next enable.
 */
public final class MarkerGlowRenderer {

    public static final String TAG = "spadefall_glow";

    private final SpadefallPlugin plugin;

    /** Per editing player: the entities currently shown to them. */
    private final Map<UUID, List<Entity>> shown = new HashMap<>();

    public MarkerGlowRenderer(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    /** Rebuilds the glow for one player from the map's current state. */
    public void render(Player player, MapDefinition map) {
        clear(player);

        World world = Bukkit.getWorld(map.getWorldName());
        if (world == null) {
            return;
        }

        List<Entity> entities = new ArrayList<>();

        for (Marker marker : map.all()) {
            int index = map.indexOf(marker);
            entities.add(spawnBlock(world, marker.getX(), marker.getY(), marker.getZ(),
                    GlowPalette.glass(marker.getRole()), GlowPalette.glow(marker.getRole()), 1.0f));
            entities.add(spawnLabel(world, marker.getX() + 0.5D, marker.getY() + 1.35D, marker.getZ() + 0.5D,
                    label(marker, index)));
        }

        Cuboid region = map.getRegion();
        if (region != null) {
            entities.addAll(spawnRegionEdges(world, region));
        }

        for (Entity entity : entities) {
            player.showEntity(plugin, entity);
        }
        shown.put(player.getUniqueId(), entities);
    }

    public void clear(Player player) {
        List<Entity> entities = shown.remove(player.getUniqueId());
        if (entities == null) {
            return;
        }
        for (Entity entity : entities) {
            if (entity.isValid()) {
                entity.remove();
            }
        }
    }

    public void clearAll() {
        for (UUID uuid : new ArrayList<>(shown.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                clear(player);
            } else {
                List<Entity> entities = shown.remove(uuid);
                if (entities != null) {
                    entities.forEach(e -> { if (e.isValid()) e.remove(); });
                }
            }
        }
    }

    /** Removes any tagged display entities that survived a crash. Call on enable. */
    public int sweepStrays() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getScoreboardTags().contains(TAG)) {
                    entity.remove();
                    removed++;
                }
            }
        }
        return removed;
    }

    public boolean isShowing(Player player) {
        return shown.containsKey(player.getUniqueId());
    }

    // ---- building blocks ---------------------------------------------------

    private String label(Marker marker, int index) {
        String colour = GlowPalette.chat(marker.getRole());
        String base = SetupStep.labelFor(marker.getRole());
        return switch (marker.getRole()) {
            case SPAWN, DM_SPAWN -> colour + base + " #" + index;
            case CHIP -> colour + base + " \u00B7 " + marker.getValue();
            default -> colour + base;
        };
    }

    private BlockDisplay spawnBlock(World world, int x, int y, int z, Material glass, Color glow, float scale) {
        Location location = new Location(world, x, y, z);
        BlockDisplay display = world.spawn(location, BlockDisplay.class, d -> {
            d.setBlock(glass.createBlockData());
            d.setGlowing(true);
            d.setGlowColorOverride(glow);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setViewRange(1.0f);
            d.setPersistent(false);
            d.setVisibleByDefault(false);
            d.addScoreboardTag(TAG);
            if (scale != 1.0f) {
                d.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0), new AxisAngle4f(), new Vector3f(scale, scale, scale), new AxisAngle4f()));
            }
        });
        return display;
    }

    private TextDisplay spawnLabel(World world, double x, double y, double z, String text) {
        Location location = new Location(world, x, y, z);
        return world.spawn(location, TextDisplay.class, d -> {
            d.setText(text);
            d.setBillboard(Display.Billboard.CENTER);
            d.setBackgroundColor(Color.fromARGB(0x66000000));
            d.setShadowed(true);
            d.setSeeThrough(true);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setViewRange(1.0f);
            d.setPersistent(false);
            d.setVisibleByDefault(false);
            d.addScoreboardTag(TAG);
        });
    }

    /**
     * Twelve thin bars along the region's edges. Each is a block display
     * stretched along one axis and squashed on the other two.
     */
    private List<Entity> spawnRegionEdges(World world, Cuboid region) {
        List<Entity> out = new ArrayList<>();
        float t = 0.08f;
        int x0 = region.getMinX(), y0 = region.getMinY(), z0 = region.getMinZ();
        int x1 = region.getMaxX() + 1, y1 = region.getMaxY() + 1, z1 = region.getMaxZ() + 1;
        float lx = x1 - x0, ly = y1 - y0, lz = z1 - z0;

        // 4 edges along X
        for (int[] c : new int[][]{{y0, z0}, {y0, z1}, {y1, z0}, {y1, z1}}) {
            out.add(edge(world, x0, c[0], c[1], lx, t, t));
        }
        // 4 along Y
        for (int[] c : new int[][]{{x0, z0}, {x0, z1}, {x1, z0}, {x1, z1}}) {
            out.add(edge(world, c[0], y0, c[1], t, ly, t));
        }
        // 4 along Z
        for (int[] c : new int[][]{{x0, y0}, {x0, y1}, {x1, y0}, {x1, y1}}) {
            out.add(edge(world, c[0], c[1], z0, t, t, lz));
        }
        return out;
    }

    private BlockDisplay edge(World world, double x, double y, double z, float sx, float sy, float sz) {
        // Centre the thin dimensions on the edge line.
        double ox = sx < 1 ? -sx / 2 : 0, oy = sy < 1 ? -sy / 2 : 0, oz = sz < 1 ? -sz / 2 : 0;
        Location location = new Location(world, x + ox, y + oy, z + oz);
        return world.spawn(location, BlockDisplay.class, d -> {
            d.setBlock(GlowPalette.REGION_GLASS.createBlockData());
            d.setGlowing(true);
            d.setGlowColorOverride(GlowPalette.REGION_EDGE);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setViewRange(2.0f);
            d.setPersistent(false);
            d.setVisibleByDefault(false);
            d.addScoreboardTag(TAG);
            d.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0), new AxisAngle4f(), new Vector3f(sx, sy, sz), new AxisAngle4f()));
        });
    }
}
