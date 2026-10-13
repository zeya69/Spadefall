package com.supercraftmc.spadefall.game;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Everything about the round in progress. Created when the countdown ends,
 * thrown away on reset. Nothing in here persists - banked chips and stats are
 * written out at the end of the round, not kept here.
 *
 * Score and chips are tracked separately on purpose: score decides the winner
 * and is never spendable; chips are currency. Spending chips never touches
 * score.
 */
public final class RoundState {

    private final long startedAt = System.currentTimeMillis();
    private int mapIndex = -1;

    private final Map<UUID, Integer> score = new HashMap<>();
    private final Map<UUID, Integer> unbankedChips = new HashMap<>();
    private final Map<UUID, Integer> chipsCollected = new HashMap<>();

    /** Players who have reached the finish on the current map, in order. */
    private final List<UUID> finishedThisMap = new ArrayList<>();
    /** Who won each map, for stats. */
    private final List<UUID> mapWinners = new ArrayList<>();

    private final Map<UUID, Location> assignedStart = new HashMap<>();
    private final Map<UUID, Long> frozenUntil = new HashMap<>();
    private final Set<UUID> spectators = new HashSet<>();

    public long getStartedAt() { return startedAt; }

    public int getMapIndex() { return mapIndex; }
    public void setMapIndex(int mapIndex) { this.mapIndex = mapIndex; }

    // ---- score -------------------------------------------------------------

    public void addScore(UUID uuid, int points) {
        score.merge(uuid, points, Integer::sum);
    }

    public int getScore(UUID uuid) {
        return score.getOrDefault(uuid, 0);
    }

    /** Players sorted by score, highest first. */
    public List<Map.Entry<UUID, Integer>> standings(Set<UUID> among) {
        List<Map.Entry<UUID, Integer>> out = new ArrayList<>();
        for (UUID uuid : among) {
            out.add(new java.util.AbstractMap.SimpleEntry<>(uuid, getScore(uuid)));
        }
        out.sort(Comparator.comparingInt((Map.Entry<UUID, Integer> e) -> e.getValue()).reversed());
        return out;
    }

    // ---- chips -------------------------------------------------------------

    public void addChips(UUID uuid, int amount) {
        unbankedChips.merge(uuid, amount, Integer::sum);
        chipsCollected.merge(uuid, amount, Integer::sum);
    }

    public int getUnbankedChips(UUID uuid) {
        return unbankedChips.getOrDefault(uuid, 0);
    }

    public int getChipsCollected(UUID uuid) {
        return chipsCollected.getOrDefault(uuid, 0);
    }

    public Map<UUID, Integer> drainUnbanked() {
        Map<UUID, Integer> out = new LinkedHashMap<>(unbankedChips);
        unbankedChips.clear();
        return out;
    }

    // ---- per-map progress --------------------------------------------------

    public void beginMap() {
        finishedThisMap.clear();
        frozenUntil.clear();
        assignedStart.clear();
    }

    public boolean hasFinished(UUID uuid) {
        return finishedThisMap.contains(uuid);
    }

    /** @return 1-based placement */
    public int markFinished(UUID uuid) {
        if (!finishedThisMap.contains(uuid)) {
            finishedThisMap.add(uuid);
            if (finishedThisMap.size() == 1) {
                mapWinners.add(uuid);
            }
        }
        return finishedThisMap.indexOf(uuid) + 1;
    }

    public List<UUID> getFinishedThisMap() {
        return Collections.unmodifiableList(finishedThisMap);
    }

    public List<UUID> getMapWinners() {
        return Collections.unmodifiableList(mapWinners);
    }

    // ---- starts / freeze ---------------------------------------------------

    public void assignStart(UUID uuid, Location location) {
        assignedStart.put(uuid, location);
    }

    public Location getStart(UUID uuid) {
        return assignedStart.get(uuid);
    }

    public void freeze(UUID uuid, long millis) {
        frozenUntil.put(uuid, System.currentTimeMillis() + millis);
    }

    public boolean isFrozen(UUID uuid) {
        Long until = frozenUntil.get(uuid);
        return until != null && until > System.currentTimeMillis();
    }

    public void unfreezeAll() {
        frozenUntil.clear();
    }

    // ---- spectators --------------------------------------------------------

    public Set<UUID> getSpectators() {
        return spectators;
    }

    public boolean isSpectator(UUID uuid) {
        return spectators.contains(uuid);
    }
}
