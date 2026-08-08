package com.supercraftmc.spadefall.listener;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.map.Marker;
import com.supercraftmc.spadefall.map.MarkerRole;
import com.supercraftmc.spadefall.map.MarkerTool;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Structure;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Drives the marker tool.
 *
 * Held state lives here rather than on the item so that the tool can be given
 * away, duplicated or dropped without any of that mattering.
 */
public final class MarkerToolListener implements Listener {

    private final SpadefallPlugin plugin;
    private final Map<UUID, MarkerTool.Session> sessions = new HashMap<>();

    public MarkerToolListener(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    public MarkerTool.Session session(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), id -> new MarkerTool.Session());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getMarkerTool().isTool(event.getItem())) {
            return;
        }
        if (!player.hasPermission("spadefall.admin")) {
            return;
        }

        Block block = event.getClickedBlock();
        MarkerTool.Session session = session(player);
        Action action = event.getAction();

        boolean rightClick = action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR;

        // Shift + right-click anywhere cycles the role, block or no block.
        if (player.isSneaking() && rightClick) {
            event.setCancelled(true);
            session.cycleRole();
            actionBar(player, plugin.getMessages().get("tool.role-selected", "role", session.describe()));
            return;
        }

        if (block == null) {
            return;
        }

        switch (action) {
            case RIGHT_CLICK_BLOCK -> {
                event.setCancelled(true);
                place(player, session, block);
            }
            case LEFT_CLICK_BLOCK -> {
                event.setCancelled(true);
                remove(player, session, block);
            }
            default -> { }
        }
    }

    private void place(Player player, MarkerTool.Session session, Block block) {
        // Markers sit in the block ABOVE the clicked face, so builders can mark
        // a ledge by clicking the ledge rather than the air over it.
        Block target = block.getRelative(0, 1, 0);

        int value = session.getRole().isValued() ? session.getChipValue() : 0;
        Marker marker = new Marker(session.getRole(),
                target.getX(), target.getY(), target.getZ(), value);

        if (session.getMode() == MarkerTool.Mode.STAMP) {
            target.setType(Material.STRUCTURE_BLOCK, false);
            BlockState state = target.getState();
            if (state instanceof Structure structure) {
                structure.setStructureName(session.getRole().toStructureName(value));
                structure.update(true, false);
            }
        }

        plugin.getPendingMarkers()
                .computeIfAbsent(player.getUniqueId(), id -> new java.util.ArrayList<>())
                .add(marker);

        plugin.getMessages().send(player, "tool.placed",
                "role", session.describe(),
                "x", String.valueOf(target.getX()),
                "y", String.valueOf(target.getY()),
                "z", String.valueOf(target.getZ()));
    }

    private void remove(Player player, MarkerTool.Session session, Block block) {
        Block target = block.getRelative(0, 1, 0);

        var pending = plugin.getPendingMarkers().get(player.getUniqueId());
        boolean removed = false;

        if (pending != null) {
            removed = pending.removeIf(m ->
                    m.getX() == target.getX() && m.getY() == target.getY() && m.getZ() == target.getZ());
        }

        if (target.getType() == Material.STRUCTURE_BLOCK) {
            BlockState state = target.getState();
            if (state instanceof Structure structure
                    && MarkerRole.parse(structure.getStructureName()) != null) {
                target.setType(Material.AIR, false);
                removed = true;
            }
        }

        if (removed) {
            plugin.getMessages().send(player, "tool.removed",
                    "role", session.describe(),
                    "x", String.valueOf(target.getX()),
                    "y", String.valueOf(target.getY()),
                    "z", String.valueOf(target.getZ()));
        } else {
            plugin.getMessages().send(player, "tool.none-here");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Action bar, without binding to Paper's Adventure API.
     *
     * Adventure is Paper-only, so calling it directly would throw on Spigot the
     * first time someone cycled the tool role. The BungeeCord chat API below
     * ships with both, and the fallback covers anything else - a cosmetic
     * action bar is never worth an exception.
     */
    private void actionBar(Player player, String message) {
        try {
            player.spigot().sendMessage(
                    net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
        } catch (Throwable ignored) {
            player.sendMessage(message);
        }
    }
}
