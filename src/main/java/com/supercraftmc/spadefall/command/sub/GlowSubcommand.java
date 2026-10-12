package com.supercraftmc.spadefall.command.sub;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.command.Subcommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class GlowSubcommand implements Subcommand {

    private final SpadefallPlugin plugin;

    public GlowSubcommand(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getName() { return "glow"; }
    @Override public String getUsage() { return "glow"; }
    @Override public String getDescription() { return "toggle marker glow while editing"; }
    @Override public String getPermission() { return "spadefall.admin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessages().send(sender, "general.player-only");
            return;
        }
        if (!plugin.getSetupManager().isEditing(player)) {
            player.sendMessage("\u00A77Glow only shows while editing. \u00A7f/sf setup");
            return;
        }
        plugin.getSetupManager().toggleGlow(player);
    }
}
