package com.supercraftmc.spadefall.arena;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.arena.phase.DescentPhase;
import com.supercraftmc.spadefall.arena.phase.Phase;
import com.supercraftmc.spadefall.arena.phase.ResultsPhase;
import com.supercraftmc.spadefall.arena.phase.WaitingPhase;
import com.supercraftmc.spadefall.config.Messages;
import com.supercraftmc.spadefall.game.PickupManager;
import com.supercraftmc.spadefall.game.RoundState;
import com.supercraftmc.spadefall.map.MapDefinition;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * One running instance of the game.
 *
 * Arenas are self-contained and hold no static state, so splitting them
 * across servers behind a proxy later is a deployment change, not a rewrite.
 */
public final class Arena {

    private final SpadefallPlugin plugin;
    private final String id;
    private final List<String> mapNames = new ArrayList<>();
    private final Set<UUID> players = new LinkedHashSet<>();
    private final List<PickupManager.Pickup> pickups = new ArrayList<>();

    private Location lobbySpawn;
    private ArenaState state = ArenaState.WAITING;
    private Phase phase;
    private RoundState round;
    private boolean stopping;

    // Per-arena overrides; null means "use config.yml".
    private Integer minPlayersOverride;
    private Boolean autoStartOverride;
    private Integer countdownOverride;
    private Boolean deathmatchOverride;

    public Arena(SpadefallPlugin plugin, String id) {
        this.plugin = plugin;
        this.id = id;
        this.phase = new WaitingPhase();
    }

    // ---- lifecycle ---------------------------------------------------------

    /** Ticked once per second by {@link ArenaManager}. */
    public void tick() {
        if (state == ArenaState.DISABLED || phase == null) {
            return;
        }
        Phase next = phase.tick(this);
        if (next != null) {
            transitionTo(next);
        }
        plugin.getScoreboard().update(this);
    }

    public void transitionTo(Phase next) {
        if (phase != null) {
            phase.onExit(this);
        }
        this.phase = next;
        this.state = next.getState();
        next.onEnter(this);
    }

    /** Countdown finished or an admin forced it: begin the round. */
    public void startRound() {
        if (!isReady() || getPlayerCount() < 2) {
            broadcast("&cNot enough players to start.");
            transitionTo(new WaitingPhase());
            return;
        }
        this.round = new RoundState();
        transitionTo(new DescentPhase(0));
    }

    public void endRound() {
        transitionTo(new ResultsPhase());
    }

    /** Hard stop: restores everyone, clears the round, back to WAITING. */
    public void stop(String reason) {
        if (stopping) {
            return;
        }
        stopping = true;
        if (reason != null && !reason.isEmpty()) {
            broadcast("&c" + reason);
        }
        plugin.getPickups().clear(this);
        for (UUID uuid : new ArrayList<>(players)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                removePlayer(player, true);
            } else {
                players.remove(uuid);
            }
        }
        players.clear();
        round = null;
        transitionTo(new WaitingPhase());
        if (!isReady()) {
            state = ArenaState.DISABLED;
        }
        stopping = false;
    }

    // ---- players -----------------------------------------------------------

    public boolean addPlayer(Player player) {
        if (!state.isJoinable() || isFull()) {
            return false;
        }
        if (!players.add(player.getUniqueId())) {
            return false;
        }
        plugin.getPlayerStates().save(player);

        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(10f);
        player.setFireTicks(0);
        player.setFallDistance(0f);
        player.setAllowFlight(false);
        player.setFlying(false);
        if (lobbySpawn != null) {
            player.teleport(lobbySpawn);
        }

        broadcast("&e" + player.getName() + " &7joined &8(&f" + getPlayerCount()
                + "&8/&f" + getMaxPlayers() + "&8)");
        plugin.getScoreboard().update(this);
        return true;
    }

    public void removePlayer(Player player, boolean silent) {
        if (!players.remove(player.getUniqueId())) {
            return;
        }
        if (round != null) {
            plugin.getSpectators().revealToAll(this, player);
            round.getSpectators().remove(player.getUniqueId());
        }
        plugin.getScoreboard().remove(player);
        if (!plugin.getPlayerStates().restore(player)) {
            // Nothing saved - fall back to somewhere sane.
            player.setGameMode(GameMode.SURVIVAL);
            Location exit = plugin.getLobbyLocation();
            if (exit != null) {
                player.teleport(exit);
            }
        }
        if (!silent) {
            broadcast("&e" + player.getName() + " &7left &8(&f" + getPlayerCount()
                    + "&8/&f" + getMaxPlayers() + "&8)");
            player.sendMessage(Messages.colour("&7You left &f" + id + "&7."));
        }
        // A round with nobody active left in it is over.
        if (!stopping && state.isRunning() && getActiveCount() == 0) {
            stop("Everyone left - round ended.");
        }
        plugin.getScoreboard().update(this);
    }

    public boolean contains(Player player) {
        return players.contains(player.getUniqueId());
    }

    public Set<UUID> getPlayerIds() {
        return Collections.unmodifiableSet(players);
    }

    public List<Player> getOnlinePlayers() {
        List<Player> online = new ArrayList<>(players.size());
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                online.add(player);
            }
        }
        return online;
    }

    /** Online players who are still in the game - not spectating. */
    public List<Player> getActivePlayers() {
        List<Player> out = new ArrayList<>();
        for (Player player : getOnlinePlayers()) {
            if (round == null || !round.isSpectator(player.getUniqueId())) {
                out.add(player);
            }
        }
        return out;
    }

    public int getActiveCount() {
        return getActivePlayers().size();
    }

    public boolean isActive(Player player) {
        return contains(player) && (round == null || !round.isSpectator(player.getUniqueId()));
    }

    public void broadcast(String message) {
        String rendered = plugin.getMessages().getPrefix() + Messages.colour(message);
        for (Player player : getOnlinePlayers()) {
            player.sendMessage(rendered);
        }
    }

    public void title(String top, String bottom, int fadeIn, int stay, int fadeOut) {
        for (Player player : getOnlinePlayers()) {
            player.sendTitle(Messages.colour(top), Messages.colour(bottom), fadeIn, stay, fadeOut);
        }
    }

    // ---- capacity & settings -----------------------------------------------

    /** The real cap: the smaller of map capacity and configured maximum. */
    public int getMaxPlayers() {
        int configured = plugin.getConfigManager().getMaxPlayers();
        MapDefinition first = getFirstMap();
        return first == null ? configured : first.getEffectiveCapacity(configured);
    }

    public int getMinPlayers() {
        int min = minPlayersOverride != null ? minPlayersOverride : plugin.getConfigManager().getMinPlayers();
        return Math.max(2, Math.min(min, getMaxPlayers()));
    }

    public boolean isAutoStart() {
        return autoStartOverride != null ? autoStartOverride : plugin.getConfig().getBoolean("game.auto-start", true);
    }

    public int getCountdownSeconds() {
        return countdownOverride != null ? countdownOverride : plugin.getConfigManager().getCountdownSeconds();
    }

    public Integer getMinPlayersOverride() { return minPlayersOverride; }
    public void setMinPlayersOverride(Integer v) { this.minPlayersOverride = v; }
    public Boolean getAutoStartOverride() { return autoStartOverride; }
    public void setAutoStartOverride(Boolean v) { this.autoStartOverride = v; }
    public Integer getCountdownOverride() { return countdownOverride; }
    public void setCountdownOverride(Integer v) { this.countdownOverride = v; }
    public Boolean getDeathmatchOverride() { return deathmatchOverride; }
    public void setDeathmatchOverride(Boolean v) { this.deathmatchOverride = v; }

    /** Deathmatch after the last descent. Off by default - the descent is the game. */
    public boolean isDeathmatch() {
        return deathmatchOverride != null ? deathmatchOverride : plugin.getConfig().getBoolean("game.deathmatch", false);
    }

    public boolean isFull() {
        return getPlayerCount() >= getMaxPlayers();
    }

    public int getPlayerCount() {
        return players.size();
    }

    // ---- maps --------------------------------------------------------------

    public void setMaps(List<String> names) {
        mapNames.clear();
        mapNames.addAll(names);
    }

    public void addMap(String name) {
        if (!mapNames.contains(name)) {
            mapNames.add(name);
        }
    }

    public List<String> getMapNames() {
        return Collections.unmodifiableList(mapNames);
    }

    public MapDefinition getFirstMap() {
        return mapNames.isEmpty() ? null : plugin.getMapRegistry().get(mapNames.get(0));
    }

    public MapDefinition getMap(int index) {
        if (index < 0 || index >= mapNames.size()) {
            return null;
        }
        return plugin.getMapRegistry().get(mapNames.get(index));
    }

    public MapDefinition getCurrentMap() {
        return round == null ? null : getMap(round.getMapIndex());
    }

    /** True when every assigned map exists and has passed validation. */
    public boolean isReady() {
        if (mapNames.isEmpty()) {
            return false;
        }
        for (String name : mapNames) {
            MapDefinition map = plugin.getMapRegistry().get(name);
            if (map == null || !map.isRegistered()) {
                return false;
            }
        }
        return true;
    }

    // ---- accessors ---------------------------------------------------------

    public SpadefallPlugin getPlugin() { return plugin; }
    public String getId() { return id; }
    public ArenaState getState() { return state; }
    public void setState(ArenaState state) { this.state = state; }
    public Phase getPhase() { return phase; }
    public RoundState getRound() { return round; }
    public List<PickupManager.Pickup> getPickups() { return pickups; }
    public Location getLobbySpawn() { return lobbySpawn; }
    public void setLobbySpawn(Location lobbySpawn) { this.lobbySpawn = lobbySpawn; }
}
