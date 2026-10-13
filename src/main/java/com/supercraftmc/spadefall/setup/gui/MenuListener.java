package com.supercraftmc.spadefall.setup.gui;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.setup.SetupStep;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Routes clicks in our menus. Every click is cancelled - nothing in these
 * inventories is ever meant to move.
 */
public final class MenuListener implements Listener {

    private final SpadefallPlugin plugin;

    public MenuListener(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() != event.getInventory()) {
            return; // click was in the player's own inventory
        }
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType().isAir()) {
            return;
        }
        int slot = event.getRawSlot();
        boolean shift = event.isShiftClick();

        switch (holder.getKind()) {
            case HUB -> hub(player, slot);
            case ARENA -> arena(player, slot, shift);
        }
    }

    private void hub(Player player, int slot) {
        if (slot == SetupHubMenu.CREATE_SLOT) {
            plugin.getSetupManager().promptArenaName(player);
            return;
        }
        // Arenas were laid out in order from slot 10, skipping the edges.
        List<Arena> arenas = new ArrayList<>(plugin.getArenaManager().all());
        int s = 10;
        for (Arena arena : arenas) {
            if (s == slot) {
                player.closeInventory();
                plugin.getSetupManager().enter(player, arena.getId(), true);
                return;
            }
            s++;
            if (s % 9 == 8) s += 2;
        }
    }

    private void arena(Player player, int slot, boolean shift) {
        var setup = plugin.getSetupManager();

        for (SetupStep step : SetupStep.values()) {
            if (step.getSlot() != slot) continue;
            if (step == SetupStep.REGION && shift) {
                player.closeInventory();
                player.getInventory().addItem(plugin.getMarkerTool().createWand());
                player.sendMessage("\u00A77Region wand: \u00A7fleft-click\u00A77 corner 1, \u00A7fright-click\u00A77 corner 2.");
                return;
            }
            if (step == SetupStep.REGISTER) {
                player.closeInventory();
            }
            setup.stepAction(player, step);
            if (step == SetupStep.LOBBY || step == SetupStep.REGION) {
                ArenaSetupMenu.open(plugin, player);
            }
            return;
        }

        switch (slot) {
            case ArenaSetupMenu.SLOT_MAP -> {
                if (shift) setup.addMap(player); else setup.cycleMap(player);
                ArenaSetupMenu.open(plugin, player);
            }
            case ArenaSetupMenu.SLOT_SETTINGS -> {
                var arena = setup.editingArena(player);
                if (arena != null) {
                    arena.setAutoStartOverride(!arena.isAutoStart());
                    plugin.getArenaManager().save();
                    player.sendMessage("\u00A77Auto-start " + (arena.isAutoStart() ? "\u00A7aon" : "\u00A7coff")
                            + "\u00A77 for \u00A7f" + arena.getId());
                }
                ArenaSetupMenu.open(plugin, player);
            }
            case ArenaSetupMenu.SLOT_IMPORT -> {
                player.closeInventory();
                setup.importStamped(player);
            }
            case ArenaSetupMenu.SLOT_GLOW -> {
                setup.toggleGlow(player);
                ArenaSetupMenu.open(plugin, player);
            }
            case ArenaSetupMenu.SLOT_NEXT -> {
                player.closeInventory();
                setup.sendNext(player);
            }
            case ArenaSetupMenu.SLOT_MISSING -> {
                player.closeInventory();
                setup.sendMissing(player);
            }
            case ArenaSetupMenu.SLOT_DONE -> {
                player.closeInventory();
                setup.exit(player);
            }
            default -> { }
        }
    }
}
