package com.supercraftmc.spadefall.map;

import com.supercraftmc.spadefall.util.Cuboid;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A map: its region and every marker inside it.
 *
 * Since slice 1.5 a map is a live, editable draft. Markers are added and
 * removed as the owner places them with the tool, the region can derive itself
 * from the markers, and the map is persisted on every change so an interrupted
 * setup session loses nothing. {@code registered} flips to true only once
 * validation has passed - an arena will not run on an unregistered map.
 *
 * Almost nothing here is configured by hand. Capacity, bounds, drop height and
 * the void plane are DERIVED from the markers, which is what lets a downloaded
 * map describe itself.
 */
public final class MapDefinition {

    public enum RegionMode { AUTO, MANUAL }

    private final String name;
    private String worldName;
    private Cuboid manualRegion;
    private RegionMode regionMode = RegionMode.AUTO;
    private int paddingHorizontal = 8;
    private int paddingVertical = 16;
    private boolean registered;
    private final Map<MarkerRole, List<Marker>> markers = new EnumMap<>(MarkerRole.class);

    public MapDefinition(String name, String worldName) {
        this.name = name;
        this.worldName = worldName;
        for (MarkerRole role : MarkerRole.values()) {
            markers.put(role, new ArrayList<>());
        }
    }

    /** Legacy constructor: a scanned map with a fixed region. */
    public MapDefinition(String name, Cuboid region) {
        this(name, region.getWorldName());
        this.manualRegion = region;
        this.regionMode = RegionMode.MANUAL;
    }

    // ---- markers -----------------------------------------------------------

    public void add(Marker marker) {
        List<Marker> list = markers.get(marker.getRole());
        list.remove(marker);
        list.add(marker);
    }

    /** Removes whatever marker sits at the position, any role. */
    public Marker removeAt(int x, int y, int z) {
        for (List<Marker> list : markers.values()) {
            for (int i = 0; i < list.size(); i++) {
                Marker marker = list.get(i);
                if (marker.getX() == x && marker.getY() == y && marker.getZ() == z) {
                    list.remove(i);
                    return marker;
                }
            }
        }
        return null;
    }

    public Marker findAt(int x, int y, int z) {
        for (List<Marker> list : markers.values()) {
            for (Marker marker : list) {
                if (marker.getX() == x && marker.getY() == y && marker.getZ() == z) {
                    return marker;
                }
            }
        }
        return null;
    }

    /** 1-based position of a marker within its role list, for labels like "Start #3". */
    public int indexOf(Marker marker) {
        return markers.get(marker.getRole()).indexOf(marker) + 1;
    }

    public List<Marker> get(MarkerRole role) {
        return Collections.unmodifiableList(markers.get(role));
    }

    public List<Marker> all() {
        List<Marker> out = new ArrayList<>();
        for (List<Marker> list : markers.values()) {
            out.addAll(list);
        }
        return out;
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

    // ---- region ------------------------------------------------------------

    /**
     * The effective region. In AUTO mode this is the bounding box of every
     * marker plus padding, recomputed on demand so it always tracks the latest
     * placements. In MANUAL mode it is whatever the owner selected.
     *
     * @return null when there is nothing to derive a region from
     */
    public Cuboid getRegion() {
        if (regionMode == RegionMode.MANUAL) {
            return manualRegion;
        }
        List<Marker> everything = all();
        if (everything.isEmpty()) {
            return null;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Marker marker : everything) {
            minX = Math.min(minX, marker.getX()); maxX = Math.max(maxX, marker.getX());
            minY = Math.min(minY, marker.getY()); maxY = Math.max(maxY, marker.getY());
            minZ = Math.min(minZ, marker.getZ()); maxZ = Math.max(maxZ, marker.getZ());
        }
        return new Cuboid(worldName,
                minX - paddingHorizontal, minY - paddingVertical, minZ - paddingHorizontal,
                maxX + paddingHorizontal, maxY + paddingVertical, maxZ + paddingHorizontal);
    }

    public boolean hasRegion() {
        return getRegion() != null;
    }

    public void setManualRegion(Cuboid region) {
        this.manualRegion = region;
        this.regionMode = RegionMode.MANUAL;
        if (region != null) {
            this.worldName = region.getWorldName();
        }
    }

    public void setAutoRegion(int paddingHorizontal, int paddingVertical) {
        this.regionMode = RegionMode.AUTO;
        this.paddingHorizontal = Math.max(0, paddingHorizontal);
        this.paddingVertical = Math.max(0, paddingVertical);
    }

    public RegionMode getRegionMode() { return regionMode; }
    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }

    // ---- derived -----------------------------------------------------------

    public String getName() { return name; }
    public boolean isRegistered() { return registered; }
    public void setRegistered(boolean registered) { this.registered = registered; }

    /** The map's own capacity: one player per spawn marker. */
    public int getCapacity() {
        return count(MarkerRole.SPAWN);
    }

    /**
     * The effective cap once the server's configured maximum is considered.
     * The smaller number always wins, so the two never need reconciling.
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
        if (lowest != Integer.MAX_VALUE) {
            return lowest;
        }
        Cuboid region = getRegion();
        return region == null ? 0 : region.getMinY();
    }

    /** Mean Y of the spawn markers. */
    public int getSpawnY() {
        List<Marker> spawns = markers.get(MarkerRole.SPAWN);
        if (spawns.isEmpty()) {
            Cuboid region = getRegion();
            return region == null ? 0 : region.getMaxY();
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

    public long getChipPoolValue() {
        long total = 0L;
        for (Marker marker : markers.get(MarkerRole.CHIP)) {
            total += Math.max(0, marker.getValue());
        }
        return total;
    }

    // ---- persistence -------------------------------------------------------

    public void write(ConfigurationSection section) {
        section.set("name", name);
        section.set("world", worldName);
        section.set("registered", registered);
        section.set("region-mode", regionMode.name());
        section.set("padding-horizontal", paddingHorizontal);
        section.set("padding-vertical", paddingVertical);
        if (manualRegion != null) {
            manualRegion.write(section.createSection("region"));
        }
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
        if (name == null) {
            return null;
        }
        ConfigurationSection regionSection = section.getConfigurationSection("region");
        Cuboid region = regionSection == null ? null : Cuboid.read(regionSection);

        String world = section.getString("world", region == null ? null : region.getWorldName());
        if (world == null) {
            return null;
        }

        MapDefinition definition = new MapDefinition(name, world);
        definition.manualRegion = region;

        // Pre-1.5 files have a region and no mode: treat them as manual.
        String mode = section.getString("region-mode", region != null ? "MANUAL" : "AUTO");
        try {
            definition.regionMode = RegionMode.valueOf(mode);
        } catch (IllegalArgumentException ex) {
            definition.regionMode = region != null ? RegionMode.MANUAL : RegionMode.AUTO;
        }
        definition.paddingHorizontal = section.getInt("padding-horizontal", 8);
        definition.paddingVertical = section.getInt("padding-vertical", 16);
        // Pre-1.5 maps were only saved after passing validation, so they count as registered.
        definition.registered = section.getBoolean("registered", true);

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
