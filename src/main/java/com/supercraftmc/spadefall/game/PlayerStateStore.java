package com.supercraftmc.spadefall.game;

import com.supercraftmc.spadefall.SpadefallPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Saves everything about a player when they join an arena and puts it back
 * when they leave: inventory, armour, gamemode, location, health, food, XP,
 * flight. Written to disk as well as memory, so a crash mid-round can't eat
 * anyone's inventory - the file is restored on their next login.
 *
 * This is the single most important piece of a public minigame plugin. A
 * player who loses their diamonds to a server restart does not come back.
 */
public final class PlayerStateStore {

    private final SpadefallPlugin plugin;
    private final File folder;
    private final Map<UUID, YamlConfiguration> memory = new HashMap<>();

    public PlayerStateStore(SpadefallPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "playerdata");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create playerdata folder; crash recovery disabled.");
        }
    }

    public void save(Player player) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("name", player.getName());
        yaml.set("saved-at", System.currentTimeMillis());
        yaml.set("gamemode", player.getGameMode().name());
        yaml.set("health", player.getHealth());
        yaml.set("food", player.getFoodLevel());
        yaml.set("saturation", (double) player.getSaturation());
        yaml.set("level", player.getLevel());
        yaml.set("exp", (double) player.getExp());
        yaml.set("fire-ticks", player.getFireTicks());
        yaml.set("allow-flight", player.getAllowFlight());
        yaml.set("flying", player.isFlying());
        yaml.set("inventory", Arrays.asList(player.getInventory().getContents()));
        yaml.set("armor", Arrays.asList(player.getInventory().getArmorContents()));
        yaml.set("offhand", player.getInventory().getItemInOffHand());
        writeLocation(yaml, "location", player.getLocation());

        memory.put(player.getUniqueId(), yaml);
        try {
            yaml.save(file(player.getUniqueId()));
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not write player state for " + player.getName() + ": " + ex.getMessage());
        }
    }

    /** @return true if there was something to restore */
    public boolean restore(Player player) {
        YamlConfiguration yaml = memory.remove(player.getUniqueId());
        File file = file(player.getUniqueId());
        if (yaml == null && file.exists()) {
            yaml = YamlConfiguration.loadConfiguration(file);
        }
        if (yaml == null) {
            return false;
        }

        player.getInventory().clear();
        player.getInventory().setContents(toArray(yaml.getList("inventory"), 41));
        player.getInventory().setArmorContents(toArray(yaml.getList("armor"), 4));
        ItemStack offhand = yaml.getItemStack("offhand");
        player.getInventory().setItemInOffHand(offhand == null ? new ItemStack(org.bukkit.Material.AIR) : offhand);

        try {
            player.setGameMode(GameMode.valueOf(yaml.getString("gamemode", "SURVIVAL")));
        } catch (IllegalArgumentException ignored) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        double maxHealth = player.getMaxHealth();
        player.setHealth(Math.max(1.0D, Math.min(maxHealth, yaml.getDouble("health", maxHealth))));
        player.setFoodLevel(yaml.getInt("food", 20));
        player.setSaturation((float) yaml.getDouble("saturation", 5.0D));
        player.setLevel(yaml.getInt("level", 0));
        player.setExp((float) yaml.getDouble("exp", 0.0D));
        player.setFireTicks(yaml.getInt("fire-ticks", 0));
        player.setFallDistance(0f);
        player.setAllowFlight(yaml.getBoolean("allow-flight", false));
        player.setFlying(yaml.getBoolean("flying", false) && player.getAllowFlight());

        Location location = readLocation(yaml, "location");
        if (location != null) {
            player.teleport(location);
        }

        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete player state file for " + player.getName());
        }
        return true;
    }

    /** Called on login: if a state file survived a crash, put the player back. */
    public void recoverOnJoin(Player player) {
        if (file(player.getUniqueId()).exists() && !memory.containsKey(player.getUniqueId())) {
            if (restore(player)) {
                player.sendMessage("\u00A77Your inventory was restored after an interrupted Spadefall round.");
            }
        }
    }

    public boolean has(UUID uuid) {
        return memory.containsKey(uuid) || file(uuid).exists();
    }

    private File file(UUID uuid) {
        return new File(folder, uuid + ".yml");
    }

    @SuppressWarnings("unchecked")
    private ItemStack[] toArray(List<?> list, int size) {
        ItemStack[] out = new ItemStack[size];
        if (list == null) return out;
        for (int i = 0; i < Math.min(size, list.size()); i++) {
            Object o = list.get(i);
            out[i] = o instanceof ItemStack stack ? stack : null;
        }
        return out;
    }

    private void writeLocation(YamlConfiguration yaml, String path, Location l) {
        if (l.getWorld() == null) return;
        yaml.set(path + ".world", l.getWorld().getName());
        yaml.set(path + ".x", l.getX());
        yaml.set(path + ".y", l.getY());
        yaml.set(path + ".z", l.getZ());
        yaml.set(path + ".yaw", (double) l.getYaw());
        yaml.set(path + ".pitch", (double) l.getPitch());
    }

    private Location readLocation(YamlConfiguration yaml, String path) {
        String worldName = yaml.getString(path + ".world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, yaml.getDouble(path + ".x"), yaml.getDouble(path + ".y"), yaml.getDouble(path + ".z"),
                (float) yaml.getDouble(path + ".yaw"), (float) yaml.getDouble(path + ".pitch"));
    }

    public List<UUID> pendingFiles() {
        List<UUID> out = new ArrayList<>();
        File[] files = folder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return out;
        for (File f : files) {
            try {
                out.add(UUID.fromString(f.getName().replace(".yml", "")));
            } catch (IllegalArgumentException ignored) { }
        }
        return out;
    }
}
