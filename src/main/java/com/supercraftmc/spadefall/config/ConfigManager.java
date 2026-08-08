package com.supercraftmc.spadefall.config;

import com.supercraftmc.spadefall.SpadefallPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed accessors over config.yml.
 *
 * Everything the design calls a "tuning knob" lives here rather than as a
 * constant in code, because most of these numbers are playtest findings and
 * will move.
 */
public final class ConfigManager {

    private final SpadefallPlugin plugin;

    private int minPlayers;
    private int maxPlayers;
    private int maxConcurrentArenas;
    private int countdownSeconds;
    private int countdownSecondsWhenFull;
    private int mapsPerRound;
    private int descentTimeLimit;
    private int intermissionDuration;
    private int deathmatchTimeLimit;

    private double spadesPerPlayer;
    private int spadesMin;
    private int spadesMax;
    private double chipsPerPlayer;
    private int perkSlots;

    private int maxSizeX;
    private int maxSizeY;
    private int maxSizeZ;
    private int voidMargin;
    private int scanBlocksPerTick;

    private String lobbyWorld;
    private boolean startupSanityCheck;
    private boolean debug;

    public ConfigManager(SpadefallPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        lobbyWorld = c.getString("lobby-world", "");

        minPlayers = c.getInt("game.min-players", 4);
        maxPlayers = c.getInt("game.max-players", 16);
        maxConcurrentArenas = c.getInt("game.max-concurrent-arenas", 2);
        countdownSeconds = c.getInt("game.countdown-seconds", 45);
        countdownSecondsWhenFull = c.getInt("game.countdown-seconds-when-full", 10);
        mapsPerRound = c.getInt("game.maps-per-round", 3);
        descentTimeLimit = c.getInt("game.descent-time-limit", 180);
        intermissionDuration = c.getInt("game.intermission-duration", 30);
        deathmatchTimeLimit = c.getInt("game.deathmatch-time-limit", 180);

        spadesPerPlayer = c.getDouble("spades.per-player", 0.25D);
        spadesMin = c.getInt("spades.min", 2);
        spadesMax = c.getInt("spades.max", 24);
        chipsPerPlayer = c.getDouble("chips.per-player", 4.0D);
        perkSlots = c.getInt("perks.slots", 2);

        maxSizeX = c.getInt("maps.max-size-x", 256);
        maxSizeY = c.getInt("maps.max-size-y", 384);
        maxSizeZ = c.getInt("maps.max-size-z", 256);
        voidMargin = c.getInt("maps.void-margin", 20);
        scanBlocksPerTick = Math.max(1000, c.getInt("maps.scan-blocks-per-tick", 40000));

        startupSanityCheck = c.getBoolean("startup-sanity-check", true);
        debug = c.getBoolean("debug", false);
    }

    /**
     * How many spades a round should place for the given lobby size.
     * Scaled, then clamped - a fixed count is nonsense across a 4-to-64 range.
     */
    public int spadesFor(int playerCount) {
        int scaled = (int) Math.round(playerCount * spadesPerPlayer);
        return Math.max(spadesMin, Math.min(spadesMax, scaled));
    }

    public int chipsFor(int playerCount) {
        return Math.max(1, (int) Math.round(playerCount * chipsPerPlayer));
    }

    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getMaxConcurrentArenas() { return maxConcurrentArenas; }
    public int getCountdownSeconds() { return countdownSeconds; }
    public int getCountdownSecondsWhenFull() { return countdownSecondsWhenFull; }
    public int getMapsPerRound() { return mapsPerRound; }
    public int getDescentTimeLimit() { return descentTimeLimit; }
    public int getIntermissionDuration() { return intermissionDuration; }
    public int getDeathmatchTimeLimit() { return deathmatchTimeLimit; }
    public int getPerkSlots() { return perkSlots; }
    public int getMaxSizeX() { return maxSizeX; }
    public int getMaxSizeY() { return maxSizeY; }
    public int getMaxSizeZ() { return maxSizeZ; }
    public int getVoidMargin() { return voidMargin; }
    public int getScanBlocksPerTick() { return scanBlocksPerTick; }
    public String getLobbyWorld() { return lobbyWorld; }
    public boolean isStartupSanityCheck() { return startupSanityCheck; }
    public boolean isDebug() { return debug; }
}
