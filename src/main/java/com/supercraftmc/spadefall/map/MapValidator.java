package com.supercraftmc.spadefall.map;

import com.supercraftmc.spadefall.config.ConfigManager;
import com.supercraftmc.spadefall.util.Cuboid;

/**
 * Checks a scanned map and reports what is wrong with it.
 *
 * The split between error and warning is the whole point of this class:
 *
 *   ERROR   - the map physically cannot run. Refused.
 *   WARNING - it will run, but probably not the way the owner intended.
 *             Always overridable.
 *
 * The canonical warning is "64 spawns but only 3 spade candidates" - perfectly
 * playable, almost certainly not what anyone meant.
 */
public final class MapValidator {

    private final ConfigManager config;

    public MapValidator(ConfigManager config) {
        this.config = config;
    }

    public ValidationReport validate(MapDefinition map, boolean isFinalMap) {
        ValidationReport report = new ValidationReport(map.getName());

        validateStructure(map, report, isFinalMap);
        validateSize(map.getRegion(), report);
        validateDensity(map, report);

        return report;
    }

    // ---- structural: these make the map unrunnable -------------------------

    private void validateStructure(MapDefinition map, ValidationReport report, boolean isFinalMap) {
        int spawns = map.count(MarkerRole.SPAWN);
        int finishes = map.count(MarkerRole.FINISH);

        if (finishes == 0) {
            report.error("No 'finish' marker - players would have nowhere to land");
        }
        if (spawns < 2) {
            report.error("Only " + spawns + " spawn marker(s); at least 2 are required");
        }

        if (spawns > 0 && finishes > 0) {
            int finishY = map.getFinishY();
            int badSpawns = 0;
            for (Marker spawn : map.get(MarkerRole.SPAWN)) {
                if (spawn.getY() <= finishY) {
                    badSpawns++;
                }
            }
            if (badSpawns > 0) {
                report.error(badSpawns + " spawn marker(s) are at or below the finish - "
                        + "players would start already finished");
            }
        }

        if (isFinalMap && map.count(MarkerRole.DM_SPAWN) < 2) {
            report.error("Final map needs at least 2 'dm_spawn' markers for the deathmatch");
        }

        for (Marker chip : map.get(MarkerRole.CHIP)) {
            if (chip.getValue() <= 0) {
                report.warn("Chip marker at " + chip.getX() + "," + chip.getY() + "," + chip.getZ()
                        + " has no valid denomination - expected e.g. spadefall:chip:25");
            }
        }
    }

    private void validateSize(Cuboid region, ValidationReport report) {
        if (region.getSizeX() > config.getMaxSizeX()
                || region.getSizeY() > config.getMaxSizeY()
                || region.getSizeZ() > config.getMaxSizeZ()) {
            report.error("Region is " + region.getSizeX() + "x" + region.getSizeY() + "x" + region.getSizeZ()
                    + ", larger than the configured maximum of "
                    + config.getMaxSizeX() + "x" + config.getMaxSizeY() + "x" + config.getMaxSizeZ());
        }
    }

    // ---- density: playable, but probably not intended ----------------------

    private void validateDensity(MapDefinition map, ValidationReport report) {
        int capacity = map.getCapacity();
        if (capacity == 0) {
            return;
        }

        int spadesNeeded = config.spadesFor(capacity);
        int spadeCandidates = map.count(MarkerRole.SPADE);

        if (spadeCandidates == 0) {
            report.warn("No spade candidates - the card table can never be reached on this map");
        } else if (spadeCandidates < spadesNeeded) {
            report.warn(capacity + " spawn markers but only " + spadeCandidates
                    + " spade candidate(s); at full capacity this map wants " + spadesNeeded);
        } else if (spadeCandidates < spadesNeeded * 2) {
            report.warn("Only " + spadeCandidates + " spade candidates for " + spadesNeeded
                    + " placements - spade positions will repeat between rounds");
        }

        int chipsNeeded = config.chipsFor(capacity);
        int chipCandidates = map.count(MarkerRole.CHIP);

        if (chipCandidates == 0) {
            report.warn("No chip candidates - nothing to collect on the way down");
        } else if (chipCandidates < chipsNeeded) {
            report.warn(chipCandidates + " chip candidate(s); at full capacity this map wants "
                    + chipsNeeded);
        }

        int drop = map.getDropHeight();
        if (drop < 30) {
            report.warn("Drop height is only " + drop + " blocks - very short for a descent map");
        }

        if (map.count(MarkerRole.SPAWN) > 0 && map.getCapacity() < config.getMinPlayers()) {
            report.warn("Map capacity (" + map.getCapacity() + ") is below the configured minimum of "
                    + config.getMinPlayers() + " players - this map can never start a round");
        }
    }
}
