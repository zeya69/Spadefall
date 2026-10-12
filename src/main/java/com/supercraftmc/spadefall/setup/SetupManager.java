package com.supercraftmc.spadefall.setup;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.arena.ArenaState;
import com.supercraftmc.spadefall.map.MapDefinition;
import com.supercraftmc.spadefall.map.Marker;
import com.supercraftmc.spadefall.map.MarkerRole;
import com.supercraftmc.spadefall.map.MarkerTool;
import com.supercraftmc.spadefall.setup.chat.ChatUi;
import com.supercraftmc.spadefall.setup.chat.ChatUi.Button;
import com.supercraftmc.spadefall.setup.glow.GlowPalette;
import com.supercraftmc.spadefall.setup.glow.MarkerGlowRenderer;
import com.supercraftmc.spadefall.setup.gui.ArenaSetupMenu;
import com.supercraftmc.spadefall.setup.gui.SetupHubMenu;
import com.supercraftmc.spadefall.util.Cuboid;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Structure;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Edit mode. Owns the sessions and routes every setup action - GUI click,
 * chat button, tool use, command - through one place so the three surfaces
 * can't drift apart.
 */
public final class SetupManager {

    private final SpadefallPlugin plugin;
    private final SetupEvaluator evaluator;
    private final MarkerGlowRenderer glow;
    private final Map<UUID, SetupSession> sessions = new HashMap<>();

    public SetupManager(SpadefallPlugin plugin) {
        this.plugin = plugin;
        this.evaluator = new SetupEvaluator(plugin);
        this.glow = new MarkerGlowRenderer(plugin);
    }

    // ---- sessions ----------------------------------------------------------

    public SetupSession session(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), SetupSession::new);
    }

    public boolean isEditing(Player player) {
        SetupSession s = sessions.get(player.getUniqueId());
        return s != null && s.isEditing();
    }

    public Arena editingArena(Player player) {
        SetupSession s = sessions.get(player.getUniqueId());
        return s == null || s.getArenaId() == null ? null : plugin.getArenaManager().get(s.getArenaId());
    }

    public MapDefinition editingMap(Player player) {
        SetupSession s = sessions.get(player.getUniqueId());
        return s == null || s.getMapName() == null ? null : plugin.getMapRegistry().get(s.getMapName());
    }

    public boolean isFinalMap(Arena arena, MapDefinition map) {
        List<String> names = arena.getMapNames();
        return !names.isEmpty() && names.get(names.size() - 1).equalsIgnoreCase(map.getName());
    }

    // ---- enter / exit ------------------------------------------------------

    /** Enters edit mode for an arena, creating it and a first map if needed. */
    public void enter(Player player, String arenaId, boolean openGui) {
        Arena arena = plugin.getArenaManager().get(arenaId);
        if (arena == null) {
            arena = plugin.getArenaManager().create(arenaId);
            if (arena == null) {
                player.sendMessage("\u00A7cCould not create arena " + arenaId);
                return;
            }
            player.sendMessage("\u00A7aCreated arena \u00A7f" + arena.getId() + "\u00A7a.");
        }
        if (arena.getState().isRunning()) {
            player.sendMessage("\u00A7cThat arena has a round in progress. Stop it first.");
            return;
        }

        if (arena.getMapNames().isEmpty()) {
            MapDefinition map = newMapFor(arena, player);
            arena.addMap(map.getName());
            plugin.getArenaManager().save();
        }

        SetupSession session = session(player);
        session.setArenaId(arena.getId());
        session.setMapName(arena.getMapNames().get(0));
        session.setGlow(plugin.getConfig().getBoolean("setup.glow-on-enter", true));

        arena.setState(ArenaState.DISABLED);

        refreshGlow(player);
        updateToolItem(player);

        player.sendMessage("");
        player.sendMessage("\u00A7a\u00A7l\u2660 \u00A7fEditing \u00A7a" + arena.getId()
                + " \u00A77\u00B7 map \u00A7f" + session.getMapName());
        if (openGui) {
            ArenaSetupMenu.open(plugin, player);
        } else {
            sendMissing(player);
        }
    }

    public void exit(Player player) {
        SetupSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.isEditing()) {
            return;
        }
        Arena arena = editingArena(player);
        glow.clear(player);
        session.clear();
        updateToolItem(player);
        if (arena != null) {
            arena.setState(arena.isReady() ? ArenaState.WAITING : ArenaState.DISABLED);
            player.sendMessage("\u00A77Left edit mode. \u00A7f" + arena.getId() + "\u00A77 is "
                    + (arena.isReady() ? "\u00A7aready" : "\u00A7cnot ready yet") + "\u00A77.");
        }
    }

    public void onQuit(Player player) {
        glow.clear(player);
        sessions.remove(player.getUniqueId());
    }

    public void shutdown() {
        glow.clearAll();
        sessions.clear();
    }

    public MarkerGlowRenderer getGlow() {
        return glow;
    }

    private MapDefinition newMapFor(Arena arena, Player player) {
        String base = arena.getId().toLowerCase(Locale.ROOT) + "_";
        int n = arena.getMapNames().size() + 1;
        String name = base + n;
        while (plugin.getMapRegistry().contains(name)) {
            name = base + (++n);
        }
        MapDefinition map = new MapDefinition(name, player.getWorld().getName());
        map.setAutoRegion(plugin.getConfig().getInt("setup.region.auto-padding-horizontal", 8),
                plugin.getConfig().getInt("setup.region.auto-padding-vertical", 16));
        plugin.getMapRegistry().put(map);
        return map;
    }

    /** Adds another map to the arena being edited and switches to it. */
    public void addMap(Player player) {
        Arena arena = editingArena(player);
        if (arena == null) return;
        MapDefinition map = newMapFor(arena, player);
        arena.addMap(map.getName());
        plugin.getArenaManager().save();
        session(player).setMapName(map.getName());
        refreshGlow(player);
        player.sendMessage("\u00A7aAdded map \u00A7f" + map.getName() + "\u00A7a. Now editing it.");
    }

    public void cycleMap(Player player) {
        Arena arena = editingArena(player);
        if (arena == null) return;
        List<String> names = arena.getMapNames();
        SetupSession session = session(player);
        int i = names.indexOf(session.getMapName());
        session.setMapName(names.get((i + 1) % names.size()));
        refreshGlow(player);
        player.sendMessage("\u00A77Now editing map \u00A7f" + session.getMapName()
                + " \u00A78(" + (names.indexOf(session.getMapName()) + 1) + "/" + names.size() + ")");
    }

    // ---- evaluation --------------------------------------------------------

    public List<StepStatus> evaluate(Player player) {
        Arena arena = editingArena(player);
        MapDefinition map = editingMap(player);
        if (arena == null || map == null) return List.of();
        return evaluator.evaluate(arena, map, isFinalMap(arena, map));
    }

    public SetupEvaluator getEvaluator() {
        return evaluator;
    }

    // ---- marker placement (called by the tool) -----------------------------

    public void place(Player player, Block clicked) {
        MapDefinition map = editingMap(player);
        SetupSession session = session(player);
        if (map == null) {
            player.sendMessage("\u00A7cYou're not editing an arena. \u00A77Run \u00A7f/sf setup\u00A77 first.");
            return;
        }
        Block target = clicked.getRelative(0, 1, 0);
        if (!target.getWorld().getName().equals(map.getWorldName())) {
            player.sendMessage("\u00A7cThis map lives in world \u00A7f" + map.getWorldName() + "\u00A7c.");
            return;
        }

        Marker existing = map.findAt(target.getX(), target.getY(), target.getZ());
        if (existing != null) {
            inspect(player, existing, map);
            return;
        }

        int value = session.getRole().isValued() ? session.getChipValue() : 0;
        Marker marker = new Marker(session.getRole(), target.getX(), target.getY(), target.getZ(), value);
        map.add(marker);

        if (session.getMode() == MarkerTool.Mode.STAMP) {
            target.setType(Material.STRUCTURE_BLOCK, false);
            BlockState state = target.getState();
            if (state instanceof Structure structure) {
                structure.setStructureName(session.getRole().toStructureName(value));
                structure.update(true, false);
            }
        }

        map.setRegistered(false);
        plugin.getMapRegistry().put(map);
        refreshGlow(player);

        int index = map.indexOf(marker);
        int count = map.count(marker.getRole());
        player.sendMessage(GlowPalette.chat(marker.getRole()) + "+ " + labelOf(marker, index)
                + " \u00A78at " + target.getX() + ", " + target.getY() + ", " + target.getZ()
                + " \u00A77(" + count + " placed)");
        actionBar(player);
    }

    public void remove(Player player, Block clicked) {
        MapDefinition map = editingMap(player);
        if (map == null) return;
        Block target = clicked.getRelative(0, 1, 0);
        Marker removed = map.removeAt(target.getX(), target.getY(), target.getZ());
        if (removed == null) {
            // Maybe they clicked the marker block itself rather than the block under it.
            removed = map.removeAt(clicked.getX(), clicked.getY(), clicked.getZ());
            target = clicked;
        }
        if (removed == null) {
            player.sendMessage("\u00A77No marker there.");
            return;
        }
        if (target.getType() == Material.STRUCTURE_BLOCK) {
            BlockState state = target.getState();
            if (state instanceof Structure structure && MarkerRole.parse(structure.getStructureName()) != null) {
                target.setType(Material.AIR, false);
            }
        }
        map.setRegistered(false);
        plugin.getMapRegistry().put(map);
        refreshGlow(player);
        player.sendMessage("\u00A7c- " + labelOf(removed, 0) + " \u00A77(" + map.count(removed.getRole()) + " left)");
        actionBar(player);
    }

    public void inspect(Player player, Block clicked) {
        MapDefinition map = editingMap(player);
        if (map == null) return;
        Marker marker = map.findAt(clicked.getX(), clicked.getY() + 1, clicked.getZ());
        if (marker == null) marker = map.findAt(clicked.getX(), clicked.getY(), clicked.getZ());
        if (marker == null) {
            player.sendMessage("\u00A77No marker there.");
            return;
        }
        inspect(player, marker, map);
    }

    private void inspect(Player player, Marker marker, MapDefinition map) {
        player.sendMessage(GlowPalette.chat(marker.getRole()) + labelOf(marker, map.indexOf(marker))
                + " \u00A77is already here. \u00A78Shift+left-click to remove.");
    }

    private String labelOf(Marker marker, int index) {
        String base = SetupStep.labelFor(marker.getRole());
        return switch (marker.getRole()) {
            case SPAWN -> "Player Start" + (index > 0 ? " #" + index : "");
            case DM_SPAWN -> "Deathmatch Spawn" + (index > 0 ? " #" + index : "");
            case CHIP -> "Chip " + marker.getValue();
            default -> base.replace("\u2660 ", "");
        };
    }

    // ---- step actions (GUI click / chat [Fix] / /sf setup step) -----------

    public void stepAction(Player player, SetupStep step) {
        Arena arena = editingArena(player);
        MapDefinition map = editingMap(player);
        if (arena == null || map == null) {
            player.sendMessage("\u00A7cYou're not editing an arena.");
            return;
        }
        switch (step) {
            case LOBBY -> {
                arena.setLobbySpawn(player.getLocation());
                plugin.getArenaManager().save();
                player.sendMessage("\u00A7aLobby set to where you're standing.");
            }
            case REGION -> {
                map.setAutoRegion(plugin.getConfig().getInt("setup.region.auto-padding-horizontal", 8),
                        plugin.getConfig().getInt("setup.region.auto-padding-vertical", 16));
                plugin.getMapRegistry().put(map);
                refreshGlow(player);
                Cuboid r = map.getRegion();
                player.sendMessage(r == null
                        ? "\u00A7eRegion is auto - it will size itself once you place a marker."
                        : "\u00A7aRegion set to auto: \u00A7f" + r.getSizeX() + "\u00D7" + r.getSizeY() + "\u00D7" + r.getSizeZ());
            }
            case REGISTER -> register(player);
            default -> {
                if (step.getRole() != null) {
                    giveTool(player, step.getRole());
                }
            }
        }
    }

    public void giveTool(Player player, MarkerRole role) {
        SetupSession session = session(player);
        session.setRole(role);
        boolean has = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (plugin.getMarkerTool().isTool(item)) { has = true; break; }
        }
        if (!has) {
            player.getInventory().addItem(plugin.getMarkerTool().create());
        }
        updateToolItem(player);
        player.closeInventory();
        player.sendMessage("\u00A77Tool set to " + GlowPalette.chat(role) + session.describeRole()
                + "\u00A77. Right-click a block to place.");
        actionBar(player);
    }

    public void setManualRegion(Player player, Location a, Location b) {
        MapDefinition map = editingMap(player);
        if (map == null) return;
        map.setManualRegion(Cuboid.between(a, b));
        plugin.getMapRegistry().put(map);
        refreshGlow(player);
        Cuboid r = map.getRegion();
        player.sendMessage("\u00A7aManual region: \u00A7f" + r.getSizeX() + "\u00D7" + r.getSizeY() + "\u00D7" + r.getSizeZ());
    }

    /** Imports structure-block markers already in the region into the draft. */
    public void importStamped(Player player) {
        MapDefinition map = editingMap(player);
        if (map == null) return;
        Cuboid region = map.getRegion();
        if (region == null) {
            player.sendMessage("\u00A7cNo region to scan yet. Set one manually first (shift-click Region).");
            return;
        }
        player.sendMessage("\u00A77Scanning region for stamped markers...");
        plugin.getMarkerScanner().scan(map.getName() + "_import", region, null, scanned -> {
            int added = 0;
            for (Marker m : scanned.all()) {
                if (map.findAt(m.getX(), m.getY(), m.getZ()) == null) {
                    map.add(m);
                    added++;
                }
            }
            if (added > 0) {
                map.setRegistered(false);
                plugin.getMapRegistry().put(map);
                refreshGlow(player);
            }
            player.sendMessage("\u00A7aImported \u00A7f" + added + "\u00A7a marker(s).");
        }, reason -> player.sendMessage("\u00A7cScan failed: " + reason));
    }

    public void register(Player player) {
        Arena arena = editingArena(player);
        MapDefinition map = editingMap(player);
        if (arena == null || map == null) return;

        List<StepStatus> statuses = evaluate(player);
        if (!evaluator.canRegister(statuses)) {
            player.sendMessage("\u00A7cCan't register yet:");
            for (StepStatus s : statuses) {
                if (s.blocksRegister()) {
                    player.sendMessage("  " + s.glyph() + " \u00A7f" + s.getStep().getDisplay() + "\u00A77: " + s.getSummary());
                }
            }
            return;
        }

        boolean warnings = false;
        for (StepStatus s : statuses) {
            if (s.getLevel() == StepStatus.Level.WARNING && s.getStep() != SetupStep.REGISTER) warnings = true;
        }

        Runnable doRegister = () -> {
            map.setRegistered(true);
            plugin.getMapRegistry().put(map);
            arena.setState(arena.isReady() ? ArenaState.WAITING : ArenaState.DISABLED);
            plugin.getArenaManager().save();
            player.sendMessage("\u00A7a\u00A7l\u2714 \u00A7fMap \u00A7a" + map.getName() + "\u00A7f registered.");
            if (arena.isReady()) {
                player.sendMessage("\u00A7aArena \u00A7f" + arena.getId() + "\u00A7a is ready. Players can \u00A7f/sf join " + arena.getId());
            } else {
                player.sendMessage("\u00A7eArena has other maps still unregistered. Cycle to them in the GUI.");
            }
        };

        if (warnings) {
            String key = player.getUniqueId().toString();
            plugin.getPendingConfirmations().put(key, doRegister);
            ChatUi.send(player, "\u00A7eThere are warnings. Register anyway?",
                    Button.green("Yes, register", "/sf confirm", "Register despite the warnings"),
                    Button.grey("Cancel", "/sf cancel", "Keep editing"));
        } else {
            doRegister.run();
        }
    }

    // ---- glow / tool / action bar -----------------------------------------

    public void refreshGlow(Player player) {
        SetupSession session = session(player);
        MapDefinition map = editingMap(player);
        if (session.isGlow() && map != null) {
            glow.render(player, map);
        } else {
            glow.clear(player);
        }
    }

    public void toggleGlow(Player player) {
        SetupSession session = session(player);
        session.setGlow(!session.isGlow());
        refreshGlow(player);
        player.sendMessage("\u00A77Glow " + (session.isGlow() ? "\u00A7aon" : "\u00A7coff"));
    }

    /** Renames the tool in the player's inventory to show the selected role. */
    public void updateToolItem(Player player) {
        SetupSession session = session(player);
        for (ItemStack item : player.getInventory().getContents()) {
            if (!plugin.getMarkerTool().isTool(item)) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            String name = "\u00A7a\u00A7l\u2660 \u00A7fMarker Tool";
            if (session.isEditing()) {
                name += " \u00A78\u00B7 " + GlowPalette.chat(session.getRole()) + session.describeRole();
            }
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
    }

    public void actionBar(Player player) {
        SetupSession session = session(player);
        MapDefinition map = editingMap(player);
        if (map == null) return;
        String text = "\u00A77Placing: " + GlowPalette.chat(session.getRole()) + session.describeRole()
                + " \u00A78\u00B7 \u00A7f" + map.count(session.getRole()) + "\u00A77 placed";
        try {
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(text));
        } catch (Throwable ignored) {
            // cosmetic
        }
    }

    // ---- chat output -------------------------------------------------------

    public void sendNext(Player player) {
        List<StepStatus> statuses = evaluate(player);
        if (statuses.isEmpty()) {
            noSession(player);
            return;
        }
        StepStatus next = evaluator.next(statuses);
        if (next == null) {
            player.sendMessage("\u00A7a\u00A7l\u2714 \u00A7fNothing left. This map is registered and ready.");
            return;
        }
        int index = next.getStep().ordinal() + 1;
        player.sendMessage("");
        player.sendMessage("\u00A7a\u00A7l\u2660 Next: \u00A7f" + next.getStep().getDisplay()
                + " \u00A78(step " + index + " of " + SetupStep.values().length + ")");
        player.sendMessage("\u00A77  " + next.getStep().getHelp());
        if (next.getDetail() != null) {
            player.sendMessage("\u00A78  " + next.getDetail());
        }
        String action = "/sf setup step " + next.getStep().name();
        Button primary = switch (next.getStep()) {
            case LOBBY -> Button.green("Set lobby here", action, "Use where you're standing");
            case REGION -> Button.green("Use auto region", action, "Size from your markers");
            case REGISTER -> Button.green("Register", action, "Validate and enable");
            default -> Button.green("Give me the tool", action, "Hands you the tool set to " + next.getStep().getDisplay());
        };
        ChatUi.send(player, "", primary,
                Button.of("Show the GUI", "/sf setup", "Open the setup inventory"),
                Button.grey("Everything", "/sf missing", "Full checklist"));
    }

    public void sendMissing(Player player) {
        List<StepStatus> statuses = evaluate(player);
        if (statuses.isEmpty()) {
            noSession(player);
            return;
        }
        Arena arena = editingArena(player);
        MapDefinition map = editingMap(player);
        player.sendMessage("");
        player.sendMessage("\u00A7a\u00A7l\u2660 Setup: \u00A7f" + arena.getId() + " \u00A78\u00B7 map " + map.getName()
                + " (" + (arena.getMapNames().indexOf(map.getName()) + 1) + "/" + arena.getMapNames().size() + ")");
        for (StepStatus s : statuses) {
            String line = "  " + s.glyph() + " \u00A7f" + s.getStep().getDisplay() + "\u00A77: " + s.getSummary();
            if (s.isIncomplete() && s.getStep() != SetupStep.REGISTER) {
                ChatUi.send(player, line, Button.of("Fix", "/sf setup step " + s.getStep().name(), s.getStep().getHelp()));
            } else if (s.getStep() == SetupStep.REGISTER && s.getLevel() != StepStatus.Level.DONE
                    && evaluator.canRegister(statuses)) {
                ChatUi.send(player, line, Button.green("Register", "/sf setup step REGISTER", "Validate and enable"));
            } else {
                ChatUi.line(player, line);
            }
        }
    }

    private void noSession(Player player) {
        ChatUi.send(player, "\u00A77You're not editing an arena.",
                Button.green("Open setup", "/sf setup", "Pick or create an arena"));
    }

    public void sendJoinHint(Player player) {
        if (!player.hasPermission("spadefall.admin")) return;
        if (!plugin.getConfig().getBoolean("setup.join-hint", true)) return;
        if (plugin.getArenaManager().size() > 0) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            ChatUi.send(player, "\u00A7a\u00A7l\u2660 \u00A7fSpadefall has no arenas yet.",
                    Button.green("Set one up", "/sf setup", "Opens the setup menu"));
        }, 40L);
    }

    // ---- name prompts ------------------------------------------------------

    public void promptArenaName(Player player) {
        session(player).setPrompt(SetupSession.Prompt.ARENA_NAME);
        player.closeInventory();
        ChatUi.send(player, "\u00A7eType a name for the new arena in chat. \u00A78Letters, numbers, dashes.",
                Button.grey("Cancel", "/sf setup cancelname", "Stop waiting for a name"));
    }

    public void cancelPrompt(Player player) {
        SetupSession s = sessions.get(player.getUniqueId());
        if (s != null && s.getPrompt() != null) {
            s.setPrompt(null);
            player.sendMessage("\u00A77Cancelled.");
        }
    }

    /** @return true when the chat line was consumed as a name */
    public boolean handleChat(Player player, String message) {
        SetupSession s = sessions.get(player.getUniqueId());
        if (s == null || s.getPrompt() == null) return false;
        SetupSession.Prompt prompt = s.getPrompt();
        s.setPrompt(null);

        String name = message.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        if (name.isEmpty() || name.length() > 32) {
            player.sendMessage("\u00A7cThat's not a usable name. Letters, numbers, dashes, under 32 characters.");
            return true;
        }
        if (prompt == SetupSession.Prompt.ARENA_NAME) {
            if (plugin.getArenaManager().get(name) != null) {
                player.sendMessage("\u00A7cAn arena named \u00A7f" + name + "\u00A7c already exists.");
                return true;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> enter(player, name, true));
        }
        return true;
    }

    public void openHub(Player player) {
        SetupHubMenu.open(plugin, player);
    }
}
