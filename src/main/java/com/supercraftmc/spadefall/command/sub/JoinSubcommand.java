package com.supercraftmc.spadefall.command.sub;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.command.Subcommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class JoinSubcommand implements Subcommand {

    private final SpadefallPlugin plugin;

    public JoinSubcommand(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getName() { return "join"; }
    @Override public String getUsage() { return "join [arena]"; }
    @Override public String getDescription() { return "join a game"; }
    @Override public String getPermission() { return "spadefall.play"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessages().send(sender, "general.player-only");
            return;
        }

        Arena existing = plugin.getArenaManager().findPlayerArena(player);
        if (existing != null) {
            existing.removePlayer(player, false);
        }

        Arena target;
        if (args.length > 0) {
            target = plugin.getArenaManager().get(args[0]);
            if (target == null) {
                plugin.getMessages().send(sender, "arena.not-found", "arena", args[0]);
                return;
            }
        } else {
            target = plugin.getArenaManager().findBestJoinable();
            if (target == null) {
                sender.sendMessage("\u00A7cNo arena is currently accepting players.");
                return;
            }
        }

        if (!target.addPlayer(player)) {
            sender.sendMessage("\u00A7cCould not join \u00A7f" + target.getId()
                    + "\u00A7c - it is full or already running.");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (Arena arena : plugin.getArenaManager().all()) {
                if (arena.getId().toLowerCase().startsWith(args[0].toLowerCase())) {
                    out.add(arena.getId());
                }
            }
        }
        return out;
    }
}
