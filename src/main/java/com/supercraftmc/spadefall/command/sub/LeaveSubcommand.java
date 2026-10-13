package com.supercraftmc.spadefall.command.sub;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.command.Subcommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class LeaveSubcommand implements Subcommand {

    private final SpadefallPlugin plugin;

    public LeaveSubcommand(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getName() { return "leave"; }
    @Override public String getUsage() { return "leave"; }
    @Override public String getDescription() { return "leave your current game"; }
    @Override public String getPermission() { return "spadefall.play"; }

    @Override
    public java.util.List<String> getAliases() { return java.util.List.of("quit", "lobby"); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessages().send(sender, "general.player-only");
            return;
        }
        Arena arena = plugin.getArenaManager().findPlayerArena(player);
        if (arena == null) {
            sender.sendMessage("\u00A77You're not in a game.");
            return;
        }
        arena.removePlayer(player, false);
    }
}
