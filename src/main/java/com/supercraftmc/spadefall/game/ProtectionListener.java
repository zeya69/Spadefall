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
            if (arena.getRound() != null) {
                arena.getRound().noteDamage(player.getUniqueId());
            }
            FallMode mode = fallMode();
            double before = event.getDamage();
            if (!mode.enabled) {
                event.setCancelled(true);
            } else if (mode.cap > 0.0D) {
                double damage = Math.min(before, mode.cap);
                // Never lethal in capped mode: a bad landing costs time, never the round.
                if (player.getHealth() - damage <= 0.5D) {
                    damage = Math.max(0.0D, player.getHealth() - 1.0D);
                }
                event.setDamage(damage);
            } else if (mode.multiplier != 1.0D) {
                event.setDamage(before * Math.max(0.0D, mode.multiplier));
            }
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("[debug] fall damage " + player.getName()
                        + " fallDistance=" + String.format("%.1f", player.getFallDistance())
                        + " raw=" + String.format("%.1f", before)
                        + " final=" + String.format("%.1f", event.getDamage())
                        + " health=" + String.format("%.1f", player.getHealth())
                        + " mode=" + mode + " cancelled=" + event.isCancelled());
            }
            return;
        }
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("[debug] blocked " + event.getCause() + " damage to " + player.getName()
                    + " in " + arena.getId() + " (" + arena.getState() + ")");
        }
        event.setCancelled(true);
    }

    /**
     * Reads the fall-damage settings, accepting the boolean form, the words
     * yes/no/on/off, and the pre-0.5.1 strings ON/OFF/CAPPED - because YAML
     * reads a bare ON as a boolean and that cost us an evening.
     */
    private FallMode fallMode() {
        Object raw = plugin.getConfig().get("game.fall-damage", Boolean.TRUE);
        String word = String.valueOf(raw).trim().toUpperCase();
        double cap = plugin.getConfig().getDouble("game.fall-damage-cap", 0.0D);
        double mult = plugin.getConfig().getDouble("game.fall-damage-multiplier", 1.0D);
        boolean enabled = switch (word) {
            case "FALSE", "NO", "OFF", "NONE", "0" -> false;
            default -> true;
        };
        if (word.equals("CAPPED") && cap <= 0.0D) {
            cap = 6.0D; // legacy CAPPED with no explicit cap
        }
        return new FallMode(enabled, cap, mult);
    }

    private record FallMode(boolean enabled, double cap, double multiplier) {
        @Override public String toString() {
            return !enabled ? "off" : cap > 0 ? "capped@" + cap : "on x" + multiplier;
        }
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
