package com.supercraftmc.spadefall.map;

import com.supercraftmc.spadefall.util.Cuboid;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A loaded map: its region and every marker found inside it.
 *
 * Almost nothing here is configured by hand. Capacity, bounds, drop height and
 * the void plane are all DERIVED from the markers and the region, which is what
 * lets a downloaded map describe itself.
 */
public final class MapDefinition {

    private final String name;
    private final Cuboid region;
    private final Map<MarkerRole, List<Marker>> markers = new EnumMap<>(MarkerRole.class);

    public MapDefinition(String name, Cuboid region) {
        this.name = name;
        this.region = region;
        for (MarkerRole role : MarkerRole.values()) {
            markers.put(role, new ArrayList<>());
        }
    }

    public void add(Marker marker) {
        markers.get(marker.getRole()).add(marker);
    }

    public List<Marker> get(MarkerRole role) {
        return Collections.unmodifiableList(markers.get(role));
    }

    public int count(MarkerRole role) {
        return markers.get(role).size();
    }

    public int totalMarkers() {
        int total = 0;
        for (List<Marker> list : markers.values()) {
            total += list.size();
        }
        return total;
    }

    public String getName() { return name; }
    public Cuboid getRegion() { return region; }

    /** The map's own capacity: one player per spawn marker. */
    public int getCapacity() {
        return count(MarkerRole.SPAWN);
    }

    /**
     * The effective cap once the server's configured maximum is considered.
     * The smaller number always wins, so the two never need reconciling by hand.
     */
    public int getEffectiveCapacity(int configuredMax) {
        return Math.min(getCapacity(), configuredMax);
    }

    /** Lowest Y of any finish marker, or the region floor if there are none. */
    public int getFinishY() {
        int lowest = Integer.MAX_VALUE;
        for (Marker marker : markers.get(MarkerRole.FINISH)) {
            lowest = Math.min(lowest, marker.getY());
        }
        return lowest == Integer.MAX_VALUE ? region.getMinY() : lowest;
    }

    /** Mean Y of the spawn markers. */
    public int getSpawnY() {
        List<Marker> spawns = markers.get(MarkerRole.SPAWN);
        if (spawns.isEmpty()) {
            return region.getMaxY();
        }
        long sum = 0L;
        for (Marker marker : spawns) {
            sum += marker.getY();
        }
        return (int) (sum / spawns.size());
    }

    public int getDropHeight() {
        return Math.max(0, getSpawnY() - getFinishY());
    }

    public int getVoidPlane(int voidMargin) {
        return getFinishY() - voidMargin;
    }

    /** Total face value of every chip candidate - useful for sanity reports. */
    public long getChipPoolValue() {
        long total = 0L;
        for (Marker marker : markers.get(MarkerRole.CHIP)) {
            total += Math.max(0, marker.getValue());
        }
        return total;
    }

    public void write(ConfigurationSection section) {
        section.set("name", name);
        region.write(section.createSection("region"));
        ConfigurationSection markerSection = section.createSection("markers");
        int index = 0;
        for (List<Marker> list : markers.values()) {
            for (Marker marker : list) {
                marker.write(markerSection.createSection(String.valueOf(index++)));
            }
        }
    }

    public static MapDefinition read(ConfigurationSection section) {
        String name = section.getString("name");
        ConfigurationSection regionSection = section.getConfigurationSection("region");
        if (name == null || regionSection == null) {
            return null;
        }
        Cuboid region = Cuboid.read(regionSection);
        if (region == null) {
            return null;
        }
        MapDefinition definition = new MapDefinition(name, region);
        ConfigurationSection markerSection = section.getConfigurationSection("markers");
        if (markerSection != null) {
            for (String key : markerSection.getKeys(false)) {
                ConfigurationSection entry = markerSection.getConfigurationSection(key);
                if (entry == null) {
                    continue;
                }
                Marker marker = Marker.read(entry);
                if (marker != null) {
                    definition.add(marker);
                }
            }
        }
        return definition;
    }
}
