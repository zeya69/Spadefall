package com.supercraftmc.spadefall.arena.phase;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.arena.ArenaState;
import com.supercraftmc.spadefall.game.RoundState;
import com.supercraftmc.spadefall.map.MapDefinition;
import com.supercraftmc.spadefall.map.Marker;
import com.supercraftmc.spadefall.map.MarkerRole;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One map's race to the bottom.
 *
 * Players are scattered across the start markers, held for a short "get
 * ready" freeze, then released. Reaching the finish Y inside the region ends
 * the map for that player and awards placement points; they spectate from
 * the finish until everyone's down or the timer runs out. Nobody is
 * eliminated - players who don't make it in time take last-place points and
 * carry on to the next map.
 */
public final class DescentPhase implements Phase {

    private final int mapIndex;
    private int remaining;
    private int freezeTicks;
    private int finishedGrace = -1;
    private List<Integer> placementPoints;
    private int timeoutPoints;
    private int settleSeconds;
    private final Map<UUID, Integer> onFloorFor = new HashMap<>();

    public DescentPhase(int mapIndex) {
        this.mapIndex = mapIndex;
    }

    @Override
    public ArenaState getState() {
        return ArenaState.DESCENT;
    }

    @Override
    public void onEnter(Arena arena) {
        SpadefallPlugin plugin = arena.getPlugin();
        RoundState round = arena.getRound();
        MapDefinition map = arena.getMap(mapIndex);
        if (round == null || map == null) {
            arena.stop("Map " + (mapIndex + 1) + " is missing - round aborted.");
            return;
        }

        round.setMapIndex(mapIndex);
        round.beginMap();
        remaining = plugin.getConfigManager().getDescentTimeLimit();
        freezeTicks = plugin.getConfig().getInt("game.start-freeze-seconds", 3);
        placementPoints = plugin.getConfig().getIntegerList("scoring.placement");
        if (placementPoints.isEmpty()) placementPoints = List.of(500, 300, 200, 150, 100, 75, 50, 25);
        timeoutPoints = plugin.getConfig().getInt("scoring.timeout", 10);
        settleSeconds = Math.max(1, plugin.getConfig().getInt("game.finish-settle-seconds", 1));
        onFloorFor.clear();

        // Everyone who spectated the last map is back in.
        for (Player p : arena.getOnlinePlayers()) {
            plugin.getSpectators().unspectate(arena, p);
            p.setGameMode(org.bukkit.GameMode.ADVENTURE);
            p.setHealth(p.getMaxHealth());
            p.setFoodLevel(20);
            p.setFallDistance(0f);
            p.getInventory().clear();
        }

        // Scatter across the starts.
        World world = Bukkit.getWorld(map.getWorldName());
        List<Marker> starts = new ArrayList<>(map.get(MarkerRole.SPAWN));
        Collections.shuffle(starts);
        List<Player> players = arena.getActivePlayers();
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            Marker m = starts.get(i % starts.size());
            Location start = m.toLocation(world);
            round.assignStart(p.getUniqueId(), start);
            p.teleport(start);
            round.freeze(p.getUniqueId(), freezeTicks * 1000L);
        }

        plugin.getPickups().spawnChips(arena, map, players.size());

        arena.broadcast("&a&lMap " + (mapIndex + 1) + "&7/" + arena.getMapNames().size()
                + " &8\u00B7 &f" + map.getName() + " &8\u00B7 &7drop &f" + map.getDropHeight());
        arena.title("&a&l\u2660 GET READY", "&7Drop in " + freezeTicks + "...", 5, 40, 10);
    }

    @Override
    public Phase tick(Arena arena) {
        RoundState round = arena.getRound();
        if (round == null) {
            return new WaitingPhase();
        }

        if (freezeTicks > 0) {
            freezeTicks--;
            if (freezeTicks == 0) {
                round.unfreezeAll();
                arena.title("&a&lGO!", "&7Get down.", 0, 20, 10);
                for (Player p : arena.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.6f);
                }
            } else {
                arena.title("&e" + freezeTicks, "", 0, 20, 5);
            }
            return null;
        }

        remaining--;
        checkFinishes(arena, round);

        // Everyone down early? Give it a beat so the last finisher sees their placement.
        if (countUnfinished(arena, round) == 0) {
            if (finishedGrace < 0) finishedGrace = 3;
            finishedGrace--;
            if (finishedGrace <= 0) return advance(arena, round);
            return null;
        }

        if (remaining <= 0) {
            // Time's up: unfinished players take last-place points and come along.
            for (Player p : arena.getActivePlayers()) {
                if (!round.hasFinished(p.getUniqueId())) {
                    round.markFinished(p.getUniqueId());
                    round.addScore(p.getUniqueId(), timeoutPoints);
                    p.sendMessage("\u00A7cTime's up. \u00A77+" + timeoutPoints + " points.");
                }
            }
            return advance(arena, round);
        }

        if (remaining == 30 || remaining == 10 || remaining <= 5) {
            arena.broadcast("&7" + remaining + "s left");
        }
        return null;
    }

    /**
     * Finishing means LANDING, not crossing. A player counts as down once
     * they've been on the ground at or below the line, alive, for
     * finish-settle-seconds. A dive from the rim that kills on impact never
     * gets here - the death respawns them at their start first.
     */
    private void checkFinishes(Arena arena, RoundState round) {
        MapDefinition map = arena.getCurrentMap();
        if (map == null) return;
        var region = map.getRegion();
        if (region == null) return;
        double line = map.getFinishY() + 0.5D;

        for (Player p : arena.getActivePlayers()) {
            UUID id = p.getUniqueId();
            if (round.hasFinished(id) || p.isDead()) {
                onFloorFor.remove(id);
                continue;
            }
            var loc = p.getLocation();
            boolean insideXZ = loc.getBlockX() >= region.getMinX() && loc.getBlockX() <= region.getMaxX()
                    && loc.getBlockZ() >= region.getMinZ() && loc.getBlockZ() <= region.getMaxZ();
            boolean landed = p.isOnGround() && insideXZ && loc.getY() <= line
                    && loc.getBlock().getType() != org.bukkit.Material.LAVA;
            if (!landed) {
                onFloorFor.remove(id);
                continue;
            }
            int seconds = onFloorFor.merge(id, 1, Integer::sum);
            if (seconds >= settleSeconds) {
                onFloorFor.remove(id);
                onFinish(arena, p);
            }
        }
    }

    private int countUnfinished(Arena arena, RoundState round) {
        int n = 0;
        for (Player p : arena.getActivePlayers()) {
            if (!round.hasFinished(p.getUniqueId())) n++;
        }
        return n;
    }

    private Phase advance(Arena arena, RoundState round) {
        boolean last = mapIndex + 1 >= arena.getMapNames().size();
        return last ? new ResultsPhase() : new BreatherPhase(mapIndex + 1);
    }

    @Override
    public void onExit(Arena arena) {
        arena.getPlugin().getPickups().clear(arena);
    }

    @Override
    public int getRemainingSeconds() {
        return remaining;
    }

    // ---- called by GameListener --------------------------------------------

    /** A player crossed the finish line. */
    public void onFinish(Arena arena, Player player) {
        RoundState round = arena.getRound();
        if (round == null || round.hasFinished(player.getUniqueId()) || freezeTicks > 0) return;

        int place = round.markFinished(player.getUniqueId());
        int points = place <= placementPoints.size()
                ? placementPoints.get(place - 1)
                : Math.max(timeoutPoints + 5, placementPoints.get(placementPoints.size() - 1) / 2);
        round.addScore(player.getUniqueId(), points);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, place == 1 ? 1.2f : 1f);
        player.sendTitle(com.supercraftmc.spadefall.config.Messages.colour(place == 1 ? "&6&l1st!" : "&a&l#" + place),
                com.supercraftmc.spadefall.config.Messages.colour("&7+" + points + " points"), 5, 40, 10);
        arena.broadcast((place == 1 ? "&6&l\u2660 " : "&7") + player.getName()
                + (place == 1 ? " &egot down first!" : " &7finished &f#" + place));

        // Spectate from above the finish until the map ends.
        Location where = player.getLocation().clone().add(0, 3, 0);
        arena.getPlugin().getSpectators().makeSpectator(arena, player, where);
    }

    /** A player fell out of the region or died: back to their start with a penalty. */
    public void onFellOut(Arena arena, Player player) {
        RoundState round = arena.getRound();
        if (round == null || round.hasFinished(player.getUniqueId())) return;
        Location start = round.getStart(player.getUniqueId());
        if (start == null) return;
        int penalty = arena.getPlugin().getConfig().getInt("game.void-penalty-seconds", 3);
        player.setFallDistance(0f);
        player.teleport(start);
        round.freeze(player.getUniqueId(), penalty * 1000L);
        player.sendTitle(com.supercraftmc.spadefall.config.Messages.colour("&c&lFell out"),
                com.supercraftmc.spadefall.config.Messages.colour("&7Back to the start \u00B7 " + penalty + "s"), 0, 30, 10);
    }

    public boolean isFrozen() {
        return freezeTicks > 0;
    }
}
