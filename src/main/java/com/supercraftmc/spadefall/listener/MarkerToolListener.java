package com.supercraftmc.spadefall.listener;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.setup.SetupManager;
import com.supercraftmc.spadefall.setup.SetupSession;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Drives the marker tool and the region wand. All real work is delegated to
 * {@link SetupManager} so the tool, the GUI and chat can never disagree.
 */
public final class MarkerToolListener implements Listener {

    private final SpadefallPlugin plugin;

    public MarkerToolListener(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!player.hasPermission("spadefall.admin")) {
            return;
        }

        if (plugin.getMarkerTool().isWand(item)) {
            wand(event, player);
            return;
        }
        if (!plugin.getMarkerTool().isTool(item)) {
            return;
        }

        SetupManager setup = plugin.getSetupManager();
        SetupSession session = setup.session(player);
        Action action = event.getAction();
        boolean rightClick = action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR;

        // Shift + right-click anywhere cycles the role, block or no block.
        if (player.isSneaking() && rightClick) {
            event.setCancelled(true);
            session.cycleRole();
            setup.updateToolItem(player);
            setup.actionBar(player);
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            if (rightClick && !setup.isEditing(player)) {
                event.setCancelled(true);
                setup.openHub(player);
            }
            return;
        }

        switch (action) {
            case RIGHT_CLICK_BLOCK -> {
                event.setCancelled(true);
                setup.place(player, block);
            }
            case LEFT_CLICK_BLOCK -> {
                event.setCancelled(true);
                if (player.isSneaking()) {
                    setup.remove(player, block);
                } else {
                    setup.inspect(player, block);
                }
            }
            default -> { }
        }
    }

    private void wand(PlayerInteractEvent event, Player player) {
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        event.setCancelled(true);
        Location location = block.getLocation();
        switch (event.getAction()) {
            case LEFT_CLICK_BLOCK -> {
                plugin.getSelectionPos1().put(player.getUniqueId(), location);
                player.sendMessage("\u00A7aCorner 1: \u00A7f" + location.getBlockX() + ", "
                        + location.getBlockY() + ", " + location.getBlockZ());
            }
            case RIGHT_CLICK_BLOCK -> {
                plugin.getSelectionPos2().put(player.getUniqueId(), location);
                player.sendMessage("\u00A7aCorner 2: \u00A7f" + location.getBlockX() + ", "
                        + location.getBlockY() + ", " + location.getBlockZ());
            }
            default -> { return; }
        }
        Location a = plugin.getSelectionPos1().get(player.getUniqueId());
        Location b = plugin.getSelectionPos2().get(player.getUniqueId());
        if (a != null && b != null && plugin.getSetupManager().isEditing(player)) {
            plugin.getSetupManager().setManualRegion(player, a, b);
        }
    }

    /** Shift + scroll wheel cycles the role while the tool is held. */
    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        ItemStack held = player.getInventory().getItem(event.getPreviousSlot());
        if (!plugin.getMarkerTool().isTool(held) || !plugin.getSetupManager().isEditing(player)) {
            return;
        }
        event.setCancelled(true);
        plugin.getSetupManager().session(player).cycleRole();
        plugin.getSetupManager().updateToolItem(player);
        plugin.getSetupManager().actionBar(player);
    }
}
