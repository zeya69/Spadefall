package com.supercraftmc.spadefall.map;

import com.supercraftmc.spadefall.SpadefallPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Holds every loaded map and persists them to maps.yml.
 *
 * Scanning a region is expensive, so results are cached here and written to
 * disk. A map is rescanned only when the owner asks for it.
 */
public final class MapRegistry {

    private final SpadefallPlugin plugin;
    private final Map<String, MapDefinition> maps = new LinkedHashMap<>();
    private final File file;

    public MapRegistry(SpadefallPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "maps.yml");
    }

    public void load() {
        maps.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("maps");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            MapDefinition definition = MapDefinition.read(section);
            if (definition == null) {
                plugin.getLogger().warning("Skipping malformed map entry '" + key + "' in maps.yml");
                continue;
            }
            maps.put(key.toLowerCase(Locale.ROOT), definition);
        }
        plugin.getLogger().info("Loaded " + maps.size() + " map(s).");
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection root = yaml.createSection("maps");
        for (Map.Entry<String, MapDefinition> entry : maps.entrySet()) {
            entry.getValue().write(root.createSection(entry.getKey()));
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Could not create the plugin data folder");
                return;
            }
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save maps.yml: " + ex.getMessage());
        }
    }

    public void register(MapDefinition definition) {
        maps.put(definition.getName().toLowerCase(Locale.ROOT), definition);
        save();
    }

    public boolean remove(String name) {
        boolean removed = maps.remove(name.toLowerCase(Locale.ROOT)) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public MapDefinition get(String name) {
        return maps.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean contains(String name) {
        return maps.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public Collection<MapDefinition> all() {
        return Collections.unmodifiableCollection(maps.values());
    }

    public Collection<String> names() {
        return Collections.unmodifiableCollection(maps.keySet());
    }

    public int size() {
        return maps.size();
    }
}
