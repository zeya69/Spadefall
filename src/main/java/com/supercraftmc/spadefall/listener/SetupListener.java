package com.supercraftmc.spadefall.listener;

import com.supercraftmc.spadefall.SpadefallPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Setup-mode glue: name prompts in chat, the first-run hint for ops, and
 * cleanup when an editor leaves.
 */
public final class SetupListener implements Listener {

    private final SpadefallPlugin plugin;

    public SetupListener(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        // handleChat is cheap and thread-safe for its check; the actual work is
        // bounced back to the main thread inside it.
        if (plugin.getSetupManager().handleChat(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getSetupManager().sendJoinHint(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getSetupManager().onQuit(event.getPlayer());
    }
}
