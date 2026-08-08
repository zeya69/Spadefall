package com.supercraftmc.spadefall;

import com.supercraftmc.spadefall.arena.ArenaManager;
import com.supercraftmc.spadefall.command.SpadefallCommand;
import com.supercraftmc.spadefall.config.ConfigManager;
import com.supercraftmc.spadefall.config.Messages;
import com.supercraftmc.spadefall.listener.MarkerToolListener;
import com.supercraftmc.spadefall.map.MapRegistry;
import com.supercraftmc.spadefall.map.MapValidator;
import com.supercraftmc.spadefall.map.Marker;
import com.supercraftmc.spadefall.map.MarkerScanner;
import com.supercraftmc.spadefall.map.MarkerTool;
import com.supercraftmc.spadefall.storage.Database;
import com.supercraftmc.spadefall.storage.StorageException;
import com.supercraftmc.spadefall.storage.dao.PlayerDao;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Spadefall - a descent race minigame.
 *
 * Slice 1: configuration, storage, the arena/phase machine, and the map
 * pipeline (marker scanning, validation, and the marker tool). Gameplay lands
 * in later slices; the machine below is deliberately exercisable without it.
 *
 * https://supercraft-mc.com
 */
public final class SpadefallPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private Messages messages;
    private Database database;
    private PlayerDao playerDao;

    private MapRegistry mapRegistry;
    private MapValidator mapValidator;
    private MarkerScanner markerScanner;
    private MarkerTool markerTool;
    private MarkerToolListener markerToolListener;
    private ArenaManager arenaManager;

    /** Region selection corners, per player. Transient by design. */
    private final Map<UUID, Location> selectionPos1 = new HashMap<>();
    private final Map<UUID, Location> selectionPos2 = new HashMap<>();

    /** Markers placed with the tool in REGISTER mode, awaiting a scan. */
    private final Map<UUID, List<Marker>> pendingMarkers = new HashMap<>();

    /** Actions waiting on /sf confirm. Keyed by player UUID or "CONSOLE". */
    private final Map<String, Runnable> pendingConfirmations = new HashMap<>();

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();

        this.configManager = new ConfigManager(this);
        this.messages = new Messages(this);

        this.database = new Database(this);
        try {
            database.connect();
        } catch (StorageException ex) {
            getLogger().severe("Storage failed to initialise: " + ex.getMessage());
            getLogger().severe("Spadefall cannot run without a database. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.playerDao = new PlayerDao(database);

        this.mapRegistry = new MapRegistry(this);
        this.mapValidator = new MapValidator(configManager);
        this.markerScanner = new MarkerScanner(this);
        this.markerTool = new MarkerTool(this);
        this.arenaManager = new ArenaManager(this);

        mapRegistry.load();
        arenaManager.load();
        arenaManager.start();

        this.markerToolListener = new MarkerToolListener(this);
        getServer().getPluginManager().registerEvents(markerToolListener, this);

        PluginCommand command = getCommand("spadefall");
        if (command == null) {
            getLogger().severe("The 'spadefall' command is missing from plugin.yml. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        SpadefallCommand executor = new SpadefallCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        scheduleHistoryPrune();

        if (configManager.isStartupSanityCheck()) {
            runSanityCheck();
        }

        getLogger().info("Spadefall enabled in " + (System.currentTimeMillis() - start) + "ms "
                + "(" + mapRegistry.size() + " maps, " + arenaManager.size() + " arenas)");
    }

    @Override
    public void onDisable() {
        if (arenaManager != null) {
            arenaManager.shutdown();
            arenaManager.save();
        }
        if (mapRegistry != null) {
            mapRegistry.save();
        }
        if (database != null) {
            database.close();
        }
        getLogger().info("Spadefall disabled.");
    }

    /**
     * Compares configured limits against the heap and warns when they look
     * optimistic. It never blocks - a public plugin's job here is to tell the
     * owner what they are doing before it goes wrong, not to overrule them.
     */
    private void runSanityCheck() {
        long maxHeapMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
        int arenas = configManager.getMaxConcurrentArenas();
        int players = configManager.getMaxPlayers();

        long wantedMb = (long) arenas * 700L;
        if (wantedMb > maxHeapMb) {
            getLogger().warning("--------------------------------------------------");
            getLogger().warning("max-concurrent-arenas is " + arenas + " and max-players is "
                    + players + ", but this server has only " + maxHeapMb + "MB of heap.");
            getLogger().warning("That is likely to run out of memory under load.");
            getLogger().warning("Suggested max-concurrent-arenas: "
                    + Math.max(1, (int) (maxHeapMb / 700L)));
            getLogger().warning("Run /sf doctor for the full picture.");
            getLogger().warning("--------------------------------------------------");
        }

        if (configManager.getMinPlayers() > configManager.getMaxPlayers()) {
            getLogger().warning("min-players (" + configManager.getMinPlayers()
                    + ") is greater than max-players (" + configManager.getMaxPlayers()
                    + "); no round will ever start.");
        }
    }

    private void scheduleHistoryPrune() {
        int retention = getConfig().getInt("storage.history-retention-days", 30);
        if (retention <= 0) {
            return;
        }
        // Once a day, off the main thread.
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                int removed = database.pruneHistory(retention);
                if (removed > 0 && configManager.isDebug()) {
                    getLogger().info("Pruned " + removed + " old match history row(s).");
                }
            } catch (StorageException ex) {
                getLogger().warning("History prune failed: " + ex.getMessage());
            }
        }, 20L * 60L, 20L * 60L * 60L * 24L);
    }

    public void reloadAll() {
        configManager.reload();
        messages.reload();
        mapRegistry.load();
        getLogger().info("Configuration reloaded.");
    }

    /** Where players are sent when they leave an arena. */
    public Location getLobbyLocation() {
        String configured = configManager.getLobbyWorld();
        World world = (configured == null || configured.isEmpty())
                ? Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0)
                : Bukkit.getWorld(configured);
        return world == null ? null : world.getSpawnLocation();
    }

    public ConfigManager getConfigManager() { return configManager; }
    public Messages getMessages() { return messages; }
    public Database getDatabase() { return database; }
    public PlayerDao getPlayerDao() { return playerDao; }
    public MapRegistry getMapRegistry() { return mapRegistry; }
    public MapValidator getMapValidator() { return mapValidator; }
    public MarkerScanner getMarkerScanner() { return markerScanner; }
    public MarkerTool getMarkerTool() { return markerTool; }
    public MarkerToolListener getMarkerToolListener() { return markerToolListener; }
    public ArenaManager getArenaManager() { return arenaManager; }

    public Map<UUID, Location> getSelectionPos1() { return selectionPos1; }
    public Map<UUID, Location> getSelectionPos2() { return selectionPos2; }
    public Map<UUID, List<Marker>> getPendingMarkers() { return pendingMarkers; }
    public Map<String, Runnable> getPendingConfirmations() { return pendingConfirmations; }
}
