package com.supercraftmc.spadefall.map.schematic;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.arena.ArenaState;
import com.supercraftmc.spadefall.map.MapDefinition;
import com.supercraftmc.spadefall.map.Marker;
import com.supercraftmc.spadefall.map.MarkerRole;
import com.supercraftmc.spadefall.map.ValidationReport;
import com.supercraftmc.spadefall.setup.chat.ChatUi;
import com.supercraftmc.spadefall.setup.chat.ChatUi.Button;
import com.supercraftmc.spadefall.util.Cuboid;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Turns a schematic file into a registered map - and, for the bundled demo,
 * a whole arena. This is the "thirty seconds from installed to playable" path.
 */
public final class MapLoader {

    public static final String DEMO_FILE = "st-helens.schem";
    public static final String DEMO_ARENA = "demo";

    private final SpadefallPlugin plugin;
    private final SchematicPaster paster;

    public MapLoader(SpadefallPlugin plugin) {
        this.plugin = plugin;
        this.paster = new SchematicPaster(plugin);
    }

    public File mapsFolder() {
        File folder = new File(plugin.getDataFolder(), "maps");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create the maps folder.");
        }
        return folder;
    }

    /**
     * Copies the bundled demo schematic out of the jar, ALWAYS overwriting.
     * An updated jar must bring its updated map with it. Anyone who wants to
     * customise the demo should copy it under a different name - the bundled
     * file is the bundled file.
     */
    public File ensureDemoFile() throws IOException {
        File target = new File(mapsFolder(), DEMO_FILE);
        try (InputStream in = plugin.getResource("maps/" + DEMO_FILE)) {
            if (in == null) throw new IOException("Bundled map is missing from the jar");
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    // ---- the demo --------------------------------------------------------

    /**
     * Pastes the crater with its rim at the player's feet, then builds the
     * arena around it. Asks for confirmation first because it overwrites a
     * 71x112x71 box.
     */
    public void demo(Player player) {
        Schematic schematic;
        try {
            File file = ensureDemoFile();
            try (InputStream in = new FileInputStream(file)) {
                schematic = Schematic.read(in);
            }
        } catch (IOException ex) {
            player.sendMessage("\u00A7cCould not read the bundled map: " + ex.getMessage());
            return;
        }

        // Rim is at schematic Y 104; the start pads at 105. Put the rim under the player's feet.
        int rimY = 105;
        Location feet = player.getLocation();
        int oy = feet.getBlockY() - rimY;
        int minY = feet.getWorld().getMinHeight();
        boolean clamped = false;
        if (oy < minY) {
            oy = minY;
            clamped = true;
        }
        Location origin = new Location(feet.getWorld(),
                feet.getBlockX() - schematic.getWidth() / 2, oy, feet.getBlockZ() - schematic.getLength() / 2);

        if (clamped) {
            player.sendMessage("\u00A7eYou're too low for the rim to be at your feet - the crater will be placed "
                    + "from Y " + minY + " and its rim will be above you. Stand higher for a better fit.");
        }

        String key = player.getUniqueId().toString();
        plugin.getPendingConfirmations().put(key, () -> runDemo(player, schematic, origin));
        ChatUi.send(player, "\u00A7eThis pastes a \u00A7f" + schematic.getWidth() + "\u00D7" + schematic.getHeight()
                        + "\u00D7" + schematic.getLength() + "\u00A7e crater centred on you, overwriting whatever's there.",
                Button.green("Do it", "/sf confirm", "Paste the demo map here"),
                Button.grey("Cancel", "/sf cancel", "Never mind"));
    }

    private void runDemo(Player player, Schematic schematic, Location origin) {
        Arena existing = plugin.getArenaManager().get(DEMO_ARENA);
        if (existing != null && existing.getState().isRunning()) {
            player.sendMessage("\u00A7cThe demo arena has a round in progress. \u00A77/sf stop demo\u00A7c first.");
            return;
        }
        Location lobby = player.getLocation().clone();
        player.sendMessage("\u00A77Pasting Mount St. Helens...");

        paster.paste(schematic, origin,
                percent -> { if (percent % 25 == 0) player.sendMessage("\u00A78  " + percent + "%"); },
                result -> {
                    MapDefinition map = buildMap(uniqueMapName(DEMO_ARENA + "_1"), origin, schematic, result);
                    ValidationReport report = plugin.getMapValidator().validate(map, true);
                    if (report.hasErrors()) {
                        player.sendMessage("\u00A7cThe pasted map failed validation:");
                        for (String e : report.getErrors()) player.sendMessage("\u00A7c  \u2718 " + e);
                        plugin.getMapRegistry().put(map);
                        return;
                    }
                    plugin.getMapRegistry().register(map);

                    Arena arena = plugin.getArenaManager().get(DEMO_ARENA);
                    if (arena == null) {
                        arena = plugin.getArenaManager().create(DEMO_ARENA);
                    } else {
                        arena.stop(null);
                    }
                    arena.setMaps(java.util.List.of(map.getName()));
                    // Prefer the map's own lobby marker; fall back to where the player stood, snapped down.
                    Location lobbySpot = lobby;
                    if (map.count(MarkerRole.LOBBY) > 0) {
                        Marker lm = map.get(MarkerRole.LOBBY).get(0);
                        lobbySpot = lm.toLocation(origin.getWorld());
                    }
                    arena.setLobbySpawn(com.supercraftmc.spadefall.util.Locations.snapToGround(lobbySpot, 64));
                    arena.setState(ArenaState.WAITING);
                    plugin.getArenaManager().save();

                    player.sendMessage("");
                    player.sendMessage("\u00A7a\u00A7l\u2660 \u00A7fMount St. Helens is in. Arena \u00A7ademo\u00A7f is ready.");
                    player.sendMessage("\u00A77  " + result.placed + " blocks \u00B7 " + map.count(MarkerRole.SPAWN)
                            + " starts \u00B7 " + map.count(MarkerRole.CHIP) + " chips \u00B7 "
                            + map.count(MarkerRole.SPADE) + " spades \u00B7 drop " + map.getDropHeight());
                    if (result.unknownStates > 0) {
                        player.sendMessage("\u00A7e  " + result.unknownStates + " block type(s) this server didn't know were replaced with stone.");
                    }
                    player.sendMessage("\u00A77  This is the bundled demo. It's named \u00A7fdemo\u00A77 on purpose.");
                    ChatUi.send(player, "",
                            Button.green("Join it", "/sf join demo", "Jump in"),
                            Button.of("Edit it", "/sf setup demo", "Open the setup GUI with glow"));
                },
                reason -> player.sendMessage("\u00A7cPaste failed: " + reason));
    }

    // ---- any schematic -----------------------------------------------------

    /** Pastes a file from the maps folder with its bottom at the player's feet, centred horizontally. */
    public void load(Player player, String fileName, String mapName) {
        final String resolvedName = fileName.toLowerCase(Locale.ROOT).endsWith(".schem") ? fileName : fileName + ".schem";
        File file = new File(mapsFolder(), resolvedName);
        if (!file.exists()) {
            player.sendMessage("\u00A7cNo file \u00A7f" + resolvedName + "\u00A7c in plugins/Spadefall/maps/");
            return;
        }
        Schematic schematic;
        try (InputStream in = new FileInputStream(file)) {
            schematic = Schematic.read(in);
        } catch (IOException ex) {
            player.sendMessage("\u00A7cCould not read " + resolvedName + ": " + ex.getMessage());
            return;
        }
        if (schematic.getMarkers().isEmpty()) {
            player.sendMessage("\u00A7eThat schematic has no Spadefall markers. It'll paste, but you'll need to "
                    + "mark it up with the tool afterwards.");
        }
        Location feet = player.getLocation();
        Location origin = new Location(feet.getWorld(),
                feet.getBlockX() - schematic.getWidth() / 2, feet.getBlockY(), feet.getBlockZ() - schematic.getLength() / 2);
        String finalName = uniqueMapName(mapName);

        String key = player.getUniqueId().toString();
        plugin.getPendingConfirmations().put(key, () -> {
            player.sendMessage("\u00A77Pasting " + resolvedName + "...");
            paster.paste(schematic, origin,
                    percent -> { if (percent % 25 == 0) player.sendMessage("\u00A78  " + percent + "%"); },
                    result -> {
                        MapDefinition map = buildMap(finalName, origin, schematic, result);
                        ValidationReport report = plugin.getMapValidator().validate(map, true);
                        if (report.hasErrors()) {
                            plugin.getMapRegistry().put(map);
                            player.sendMessage("\u00A7eMap \u00A7f" + finalName + "\u00A7e saved as a draft - it still needs:");
                            for (String e : report.getErrors()) player.sendMessage("\u00A7c  \u2718 " + e);
                            player.sendMessage("\u00A77Add it to an arena and open \u00A7f/sf setup\u00A77 to finish it.");
                        } else {
                            plugin.getMapRegistry().register(map);
                            player.sendMessage("\u00A7aMap \u00A7f" + finalName + "\u00A7a pasted and registered. "
                                    + "\u00A77/sf arena maps <arena> " + finalName);
                        }
                    },
                    reason -> player.sendMessage("\u00A7cPaste failed: " + reason));
        });
        ChatUi.send(player, "\u00A7eThis pastes \u00A7f" + schematic.getWidth() + "\u00D7" + schematic.getHeight()
                        + "\u00D7" + schematic.getLength() + "\u00A7e with its base at your feet, overwriting what's there.",
                Button.green("Do it", "/sf confirm", "Paste here"),
                Button.grey("Cancel", "/sf cancel", "Never mind"));
    }

    // ---- helpers -----------------------------------------------------------

    private MapDefinition buildMap(String name, Location origin, Schematic schematic, SchematicPaster.Result result) {
        Cuboid region = new Cuboid(origin.getWorld().getName(),
                origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(),
                origin.getBlockX() + schematic.getWidth() - 1,
                origin.getBlockY() + schematic.getHeight() - 1,
                origin.getBlockZ() + schematic.getLength() - 1);
        MapDefinition map = new MapDefinition(name, region);
        for (Marker m : result.markers) {
            map.add(m);
        }
        return map;
    }

    private String uniqueMapName(String base) {
        String name = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        if (name.isEmpty()) name = "map";
        String candidate = name;
        int n = 1;
        while (plugin.getMapRegistry().contains(candidate)) {
            candidate = name + "_" + (++n);
        }
        return candidate;
    }
}
