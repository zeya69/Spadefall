package com.supercraftmc.spadefall.game;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.arena.ArenaState;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

/**
 * What a player in an arena may not do. Everything here is cancelled in every
 * state unless noted. Fall damage during the descent is the one deliberate
 * exception, governed by config.
 */
public final class ProtectionListener implements Listener {

    private final SpadefallPlugin plugin;

    public ProtectionListener(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    private Arena arenaOf(Player player) {
        return plugin.getArenaManager().findPlayerArena(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (arenaOf(event.getPlayer()) != null) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (arenaOf(event.getPlayer()) != null) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (arenaOf(event.getPlayer()) != null) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && arenaOf(player) != null) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && arenaOf(player) != null) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Arena arena = arenaOf(player);
        if (arena == null) return;

        // Falling out of the world is always allowed to kill; the respawn
        // handler puts them back at their start or the lobby.
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            return;
        }

        // Only fall damage, only during the descent, and only as config allows.
        if (arena.getState() == ArenaState.DESCENT && event.getCause() == EntityDamageEvent.DamageCause.FALL
                && arena.isActive(player)) {
            String mode = plugin.getConfig().getString("game.fall-damage", "ON").toUpperCase();
            if (!event.isCancelled() && arena.getRound() != null) {
                arena.getRound().noteDamage(player.getUniqueId());
            }
            switch (mode) {
                case "OFF" -> event.setCancelled(true);
                case "ON" -> {
                    double mult = plugin.getConfig().getDouble("game.fall-damage-multiplier", 1.0D);
                    if (mult != 1.0D) event.setDamage(event.getDamage() * Math.max(0.0D, mult));
                }
                default -> {
                    double cap = plugin.getConfig().getDouble("game.fall-damage-cap", 6.0D);
                    double damage = Math.min(event.getDamage(), cap);
                    // Never lethal: a bad landing costs time, never the round.
                    if (player.getHealth() - damage <= 0.5D) {
                        damage = Math.max(0.0D, player.getHealth() - 1.0D);
                    }
                    event.setDamage(damage);
                }
            }
            return;
        }
        event.setCancelled(true);
    }

    /** No PvP until the deathmatch exists. Also protects spectators absolutely. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker = null;
        if (event.getDamager() instanceof Player p) attacker = p;
        else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) attacker = p;

        boolean victimInArena = event.getEntity() instanceof Player v && arenaOf(v) != null;
        boolean attackerInArena = attacker != null && arenaOf(attacker) != null;
        if (victimInArena || attackerInArena) {
            event.setCancelled(true);
        }
    }
}
