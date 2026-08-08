package com.supercraftmc.spadefall.arena;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.config.Messages;
import com.supercraftmc.spadefall.map.MapDefinition;
import com.supercraftmc.spadefall.arena.phase.Phase;
import com.supercraftmc.spadefall.arena.phase.WaitingPhase;
import org.bukkit.Bukkit;
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
 * Arenas are deliberately self-contained and hold no static state, so that
 * splitting them across servers behind a proxy later is a deployment change
 * rather than a rewrite.
 */
public final class Arena {

    private final SpadefallPlugin plugin;
    private final String id;
    private final List<String> mapNames = new ArrayList<>();
    private final Set<UUID> players = new LinkedHashSet<>();

    private Location lobbySpawn;
    private ArenaState state = ArenaState.WAITING;
    private Phase phase;
    private int currentMapIndex = -1;

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
    }

    public void transitionTo(Phase next) {
        if (phase != null) {
            phase.onExit(this);
        }
        this.phase = next;
        this.state = next.getState();
        next.onEnter(this);
    }

    public void stop(String reason) {
        if (reason != null && !reason.isEmpty()) {
            broadcast("&c" + reason);
        }
        for (UUID uuid : new ArrayList<>(players)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                removePlayer(player, true);
            }
        }
        players.clear();
        currentMapIndex = -1;
        transitionTo(new WaitingPhase());
    }

    // ---- players -----------------------------------------------------------

    public boolean addPlayer(Player player) {
        if (!state.isJoinable() || isFull()) {
            return false;
        }
        if (!players.add(player.getUniqueId())) {
            return false;
        }
        if (lobbySpawn != null) {
            player.teleport(lobbySpawn);
        }
        broadcast("&e" + player.getName() + " &7joined &8(&f" + getPlayerCount()
                + "&8/&f" + getMaxPlayers() + "&8)");
        return true;
    }

    public void removePlayer(Player player, boolean silent) {
        if (!players.remove(player.getUniqueId())) {
            return;
        }
        Location exit = plugin.getLobbyLocation();
        if (exit != null) {
            player.teleport(exit);
        }
        if (!silent) {
            broadcast("&e" + player.getName() + " &7left &8(&f" + getPlayerCount()
                    + "&8/&f" + getMaxPlayers() + "&8)");
        }
    }

    public boolean contains(Player player) {
        return players.contains(player.getUniqueId());
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

    public void broadcast(String message) {
        String rendered = plugin.getMessages().getPrefix() + Messages.colour(message);
        for (Player player : getOnlinePlayers()) {
            player.sendMessage(rendered);
        }
    }

    // ---- capacity ----------------------------------------------------------

    /**
     * The real cap: whichever is smaller, the map's own spawn count or the
     * server's configured maximum. The two never need reconciling by hand.
     */
    public int getMaxPlayers() {
        int configured = plugin.getConfigManager().getMaxPlayers();
        MapDefinition first = getFirstMap();
        if (first == null) {
            return configured;
        }
        return first.getEffectiveCapacity(configured);
    }

    public int getMinPlayers() {
        return plugin.getConfigManager().getMinPlayers();
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

    public List<String> getMapNames() {
        return Collections.unmodifiableList(mapNames);
    }

    public MapDefinition getFirstMap() {
        return mapNames.isEmpty() ? null : plugin.getMapRegistry().get(mapNames.get(0));
    }

    public MapDefinition getCurrentMap() {
        if (currentMapIndex < 0 || currentMapIndex >= mapNames.size()) {
            return null;
        }
        return plugin.getMapRegistry().get(mapNames.get(currentMapIndex));
    }

    public boolean advanceMap() {
        currentMapIndex++;
        return currentMapIndex < mapNames.size();
    }

    public void resetMapIndex() {
        currentMapIndex = -1;
    }

    // ---- accessors ---------------------------------------------------------

    public SpadefallPlugin getPlugin() { return plugin; }
    public String getId() { return id; }
    public ArenaState getState() { return state; }
    public Phase getPhase() { return phase; }
    public Location getLobbySpawn() { return lobbySpawn; }
    public void setLobbySpawn(Location lobbySpawn) { this.lobbySpawn = lobbySpawn; }

    public void setState(ArenaState state) { this.state = state; }
}
