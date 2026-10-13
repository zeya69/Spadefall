package com.supercraftmc.spadefall.game;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.arena.Arena;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Spectators cannot interfere, cannot be seen, and cannot talk to active
 * players. Three layers because no single one is enough:
 *
 *   gamemode SPECTATOR - no collision, no interaction, can't pick up chips
 *   hidePlayer         - gone from tab and from other plugins' eyes too
 *   chat split         - handled in GameListener: spectator chat stays
 *                        between spectators; they still see the game's chat
 */
public final class SpectatorService {

    private final SpadefallPlugin plugin;

    public SpectatorService(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    public void makeSpectator(Arena arena, Player player, Location where) {
        RoundState round = arena.getRound();
        if (round == null) return;
        round.getSpectators().add(player.getUniqueId());

        player.setGameMode(GameMode.SPECTATOR);
        player.setFallDistance(0f);
        if (where != null) {
            player.teleport(where);
        }

        for (Player active : arena.getActivePlayers()) {
            active.hidePlayer(plugin, player);
        }
        for (Player other : arena.getOnlinePlayers()) {
            if (round.isSpectator(other.getUniqueId())) {
                other.showPlayer(plugin, player);
                player.showPlayer(plugin, other);
            }
        }
    }

    /** Puts a spectator back to being an ordinary arena member (not restoring their saved state). */
    public void unspectate(Arena arena, Player player) {
        RoundState round = arena.getRound();
        if (round != null) {
            round.getSpectators().remove(player.getUniqueId());
        }
        for (Player other : arena.getOnlinePlayers()) {
            other.showPlayer(plugin, player);
            player.showPlayer(plugin, other);
        }
    }

    /** When a player leaves the arena entirely, make them visible to everyone again. */
    public void revealToAll(Arena arena, Player player) {
        for (Player other : arena.getOnlinePlayers()) {
            other.showPlayer(plugin, player);
            player.showPlayer(plugin, other);
        }
    }
}
