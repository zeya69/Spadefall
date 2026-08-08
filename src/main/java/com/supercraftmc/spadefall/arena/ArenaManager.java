package com.supercraftmc.spadefall.arena;

import com.supercraftmc.spadefall.SpadefallPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Owns every arena and drives the one-second tick.
 *
 * A single ticking task for all arenas rather than one task each: cheaper, and
 * it keeps arena ordering deterministic.
 */
public final class ArenaManager {

    private final SpadefallPlugin plugin;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private final File file;
    private BukkitTask ticker;

    public ArenaManager(SpadefallPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
    }

    public void start() {
        if (ticker != null) {
            ticker.cancel();
        }
        ticker = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Arena arena : new ArrayList<>(arenas.values())) {
                try {
                    arena.tick();
                } catch (RuntimeException ex) {
                    plugin.getLogger().severe("Arena '" + arena.getId() + "' threw during tick: " + ex);
                    if (plugin.getConfigManager().isDebug()) {
                        ex.printStackTrace();
                    }
                    arena.stop("An internal error stopped this round.");
                }
            }
        }, 20L, 20L);
    }

    public void shutdown() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
        for (Arena arena : arenas.values()) {
            arena.stop("The server is shutting down.");
        }
    }

    // ---- registry ----------------------------------------------------------

    public Arena create(String id) {
        String key = id.toLowerCase(Locale.ROOT);
        if (arenas.containsKey(key)) {
            return null;
        }
        Arena arena = new Arena(plugin, id);
        arenas.put(key, arena);
        save();
        return arena;
    }

    public boolean delete(String id) {
        Arena arena = arenas.remove(id.toLowerCase(Locale.ROOT));
        if (arena == null) {
            return false;
        }
        arena.stop("This arena was deleted.");
        save();
        return true;
    }

    public Arena get(String id) {
        return arenas.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Arena> all() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    public int size() {
        return arenas.size();
    }

    public Arena findPlayerArena(Player player) {
        for (Arena arena : arenas.values()) {
            if (arena.contains(player)) {
                return arena;
            }
        }
        return null;
    }

    /** The best arena to drop someone into: joinable, fullest first. */
    public Arena findBestJoinable() {
        Arena best = null;
        for (Arena arena : arenas.values()) {
            if (!arena.getState().isJoinable() || arena.isFull()) {
                continue;
            }
            if (best == null || arena.getPlayerCount() > best.getPlayerCount()) {
                best = arena;
            }
        }
        return best;
    }

    public int getRunningCount() {
        int running = 0;
        for (Arena arena : arenas.values()) {
            if (arena.getState().isRunning()) {
                running++;
            }
        }
        return running;
    }

    // ---- persistence -------------------------------------------------------

    public void load() {
        arenas.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("arenas");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            Arena arena = new Arena(plugin, section.getString("id", key));
            List<String> maps = section.getStringList("maps");
            arena.setMaps(maps);

            ConfigurationSection lobby = section.getConfigurationSection("lobby-spawn");
            if (lobby != null) {
                Location location = readLocation(lobby);
                if (location != null) {
                    arena.setLobbySpawn(location);
                }
            }

            if (maps.isEmpty()) {
                arena.setState(ArenaState.DISABLED);
                plugin.getLogger().warning("Arena '" + arena.getId()
                        + "' has no maps assigned and is disabled.");
            }
            arenas.put(key.toLowerCase(Locale.ROOT), arena);
        }
        plugin.getLogger().info("Loaded " + arenas.size() + " arena(s).");
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection root = yaml.createSection("arenas");
        for (Map.Entry<String, Arena> entry : arenas.entrySet()) {
            Arena arena = entry.getValue();
            ConfigurationSection section = root.createSection(entry.getKey());
            section.set("id", arena.getId());
            section.set("maps", new ArrayList<>(arena.getMapNames()));
            if (arena.getLobbySpawn() != null) {
                writeLocation(section.createSection("lobby-spawn"), arena.getLobbySpawn());
            }
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Could not create the plugin data folder");
                return;
            }
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save arenas.yml: " + ex.getMessage());
        }
    }

    private void writeLocation(ConfigurationSection section, Location location) {
        section.set("world", location.getWorld() == null ? null : location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
    }

    private Location readLocation(ConfigurationSection section) {
        String worldName = section.getString("world");
        if (worldName == null) {
            return null;
        }
        var world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World '" + worldName + "' is not loaded; skipping a saved location.");
            return null;
        }
        return new Location(world,
                section.getDouble("x"), section.getDouble("y"), section.getDouble("z"),
                (float) section.getDouble("yaw"), (float) section.getDouble("pitch"));
    }
}
