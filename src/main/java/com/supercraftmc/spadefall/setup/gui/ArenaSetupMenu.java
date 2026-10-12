package com.supercraftmc.spadefall.setup.gui;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.map.MapDefinition;
import com.supercraftmc.spadefall.setup.SetupSession;
import com.supercraftmc.spadefall.setup.SetupStep;
import com.supercraftmc.spadefall.setup.StepStatus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * The setup inventory. One item per step, coloured by status.
 *
 * Layout (54 slots):
 *   row 1  : 10-16  Lobby Region Starts Finish Spades Chips DM
 *   row 2  : 19 Chests · 21 Map switcher · 23 Import stamped
 *   row 5  : 45 Glow · 47 Next · 49 Register · 51 Missing · 53 Done
 */
public final class ArenaSetupMenu {

    private ArenaSetupMenu() { }

    public static final int SLOT_MAP = 21;
    public static final int SLOT_IMPORT = 23;
    public static final int SLOT_GLOW = 45;
    public static final int SLOT_NEXT = 47;
    public static final int SLOT_MISSING = 51;
    public static final int SLOT_DONE = 53;

    public static void open(SpadefallPlugin plugin, Player player) {
        Arena arena = plugin.getSetupManager().editingArena(player);
        MapDefinition map = plugin.getSetupManager().editingMap(player);
        if (arena == null || map == null) {
            SetupHubMenu.open(plugin, player);
            return;
        }
        SetupSession session = plugin.getSetupManager().session(player);
        List<StepStatus> statuses = plugin.getSetupManager().evaluate(player);

        MenuHolder holder = new MenuHolder(MenuHolder.Kind.ARENA);
        Inventory inv = Bukkit.createInventory(holder, 54,
                "\u00A7a\u00A7l\u2660 \u00A78" + arena.getId() + " \u00B7 " + map.getName());
        holder.setInventory(inv);

        for (int i = 0; i < 54; i++) inv.setItem(i, Items.filler());

        for (StepStatus status : statuses) {
            inv.setItem(status.getStep().getSlot(), stepItem(status, map));
        }

        int mapIndex = arena.getMapNames().indexOf(map.getName()) + 1;
        inv.setItem(SLOT_MAP, Items.of(Material.FILLED_MAP,
                "\u00A7fMap \u00A77" + mapIndex + "/" + arena.getMapNames().size() + " \u00A78\u00B7 \u00A7f" + map.getName(),
                "\u00A77Markers: \u00A7f" + map.totalMarkers(),
                "\u00A77Registered: " + (map.isRegistered() ? "\u00A7ayes" : "\u00A7cno"),
                "",
                "\u00A7eClick \u00A77to switch to the next map",
                "\u00A7eShift-click \u00A77to add another map"));

        inv.setItem(SLOT_IMPORT, Items.of(Material.STRUCTURE_BLOCK, "\u00A7fImport stamped markers",
                "\u00A77Reads structure blocks already in the",
                "\u00A77region into this map. For downloaded maps.",
                "",
                "\u00A7eClick to scan"));

        inv.setItem(SLOT_GLOW, Items.of(session.isGlow() ? Material.GLOWSTONE : Material.GRAY_DYE,
                "\u00A7fGlow: " + (session.isGlow() ? "\u00A7aON" : "\u00A7cOFF"),
                "\u00A77Shows every marker in the world,",
                "\u00A77colour-coded, with labels.",
                "",
                "\u00A7eClick to toggle"));

        inv.setItem(SLOT_NEXT, Items.of(Material.COMPASS, "\u00A7fWhat's next?",
                "\u00A77The one thing to do next.", "", "\u00A7eClick"));

        inv.setItem(SLOT_MISSING, Items.of(Material.WRITABLE_BOOK, "\u00A7fWhat's missing?",
                "\u00A77The full checklist.", "", "\u00A7eClick"));

        inv.setItem(SLOT_DONE, Items.of(Material.BARRIER, "\u00A7fDone",
                "\u00A77Leave edit mode.", "\u00A78Glow turns off. Progress is saved."));

        player.openInventory(inv);
    }

    private static ItemStack stepItem(StepStatus status, MapDefinition map) {
        SetupStep step = status.getStep();
        Material material = switch (status.getLevel()) {
            case DONE -> Material.LIME_STAINED_GLASS_PANE;
            case WARNING -> Material.YELLOW_STAINED_GLASS_PANE;
            case ERROR -> Material.RED_STAINED_GLASS_PANE;
            case OPTIONAL -> Material.GRAY_STAINED_GLASS_PANE;
        };
        // Register is special: it only "lights up" when clickable.
        if (step == SetupStep.REGISTER) {
            material = status.blocksRegister() ? Material.RED_STAINED_GLASS_PANE
                    : status.getLevel() == StepStatus.Level.DONE ? Material.LIME_STAINED_GLASS_PANE
                    : Material.NETHER_STAR;
        } else if (status.getLevel() == StepStatus.Level.DONE) {
            material = step.getIcon();
        }

        String action = switch (step) {
            case LOBBY -> "\u00A7eClick \u00A77to set it where you stand";
            case REGION -> "\u00A7eClick \u00A77for auto  \u00A78\u00B7  \u00A7eShift-click \u00A77for the wand";
            case REGISTER -> status.blocksRegister() ? "\u00A78Fix the errors first" : "\u00A7eClick \u00A77to register";
            default -> "\u00A7eClick \u00A77to get the tool set to " + step.getDisplay();
        };

        return Items.of(material,
                status.colour() + "\u00A7l" + step.getDisplay() + " " + status.glyph(),
                "\u00A77" + status.getSummary(),
                status.getDetail() == null ? null : "\u00A78" + status.getDetail(),
                "",
                "\u00A78" + step.getHelp(),
                "",
                action);
    }
}
