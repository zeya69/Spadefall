package com.supercraftmc.spadefall.game;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.arena.ArenaState;
import com.supercraftmc.spadefall.arena.phase.DescentPhase;
import com.supercraftmc.spadefall.map.MapDefinition;
import com.supercraftmc.spadefall.util.Cuboid;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Iterator;

/**
 * The in-round rules: finish detection, falling out, freezes, deaths, quits,
 * and keeping spectator chat away from the players still in it.
 */
public final class GameListener implements Listener {

    private final SpadefallPlugin plugin;

    public GameListener(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null) return;
        Location from = event.getFrom();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return; // head turn only
        }

        Player player = event.getPlayer();
        Arena arena = plugin.getArenaManager().findPlayerArena(player);
        if (arena == null || arena.getState() != ArenaState.DESCENT) return;
        RoundState round = arena.getRound();
        if (round == null || round.isSpectator(player.getUniqueId())) return;

        // Frozen: hold position but allow looking around.
        if (round.isFrozen(player.getUniqueId())) {
            Location hold = from.clone();
            hold.setYaw(to.getYaw());
            hold.setPitch(to.getPitch());
            event.setTo(hold);
            return;
        }

        if (!(arena.getPhase() instanceof DescentPhase descent)) return;
        MapDefinition map = arena.getCurrentMap();
        if (map == null) return;
        Cuboid region = map.getRegion();
        if (region == null) return;

        boolean insideXZ = to.getBlockX() >= region.getMinX() && to.getBlockX() <= region.getMaxX()
                && to.getBlockZ() >= region.getMinZ() && to.getBlockZ() <= region.getMaxZ();

        // The finish is a Y level, not a point: dropping to it anywhere inside the region counts.
        if (insideXZ && to.getY() <= map.getFinishY() + 0.5D) {
            descent.onFinish(arena, player);
            return;
        }

        // Missed the tower entirely, or fell through the world.
        int voidPlane = map.getVoidPlane(plugin.getConfigManager().getVoidMargin());
        if (to.getY() < voidPlane || (!insideXZ && to.getY() < map.getFinishY())) {
            descent.onFellOut(arena, player);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Arena arena = plugin.getArenaManager().findPlayerArena(player);
        if (arena == null) return;
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setDeathMessage(null);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.getArenaManager().findPlayerArena(player);
        if (arena == null) return;
        RoundState round = arena.getRound();
        if (arena.getState() == ArenaState.DESCENT && round != null && round.getStart(player.getUniqueId()) != null) {
            event.setRespawnLocation(round.getStart(player.getUniqueId()));
            if (arena.getPhase() instanceof DescentPhase descent) {
                plugin.getServer().getScheduler().runTask(plugin, () -> descent.onFellOut(arena, player));
            }
        } else if (arena.getLobbySpawn() != null) {
            event.setRespawnLocation(arena.getLobbySpawn());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Arena arena = plugin.getArenaManager().findPlayerArena(event.getPlayer());
        if (arena != null) {
            arena.removePlayer(event.getPlayer(), false);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getPlayerStates().recoverOnJoin(event.getPlayer());
    }

    /**
     * Spectator chat stays between spectators. Spectators still see everything
     * active players say, so they can follow the game.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        Arena arena = plugin.getArenaManager().findPlayerArena(sender);
        if (arena == null) return;
        RoundState round = arena.getRound();
        if (round == null || !round.isSpectator(sender.getUniqueId())) return;

        event.setFormat("\u00A78[\u00A77Spec\u00A78] \u00A77%1$s\u00A78: \u00A77%2$s");
        Iterator<Player> it = event.getRecipients().iterator();
        while (it.hasNext()) {
            Player recipient = it.next();
            if (recipient.equals(sender)) continue;
            if (!round.isSpectator(recipient.getUniqueId())) {
                it.remove();
            }
        }
    }
}
