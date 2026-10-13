package com.supercraftmc.spadefall.command.sub;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.command.Subcommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Hard stop. Everyone is restored and sent back; the arena returns to waiting. */
public final class StopSubcommand implements Subcommand {

    private final SpadefallPlugin plugin;

    public StopSubcommand(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getName() { return "stop"; }
    @Override public String getUsage() { return "stop [arena]"; }
    @Override public String getDescription() { return "end a round and clear the arena"; }
    @Override public String getPermission() { return "spadefall.admin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Arena arena = null;
        if (args.length > 0) {
            arena = plugin.getArenaManager().get(args[0]);
            if (arena == null) {
                plugin.getMessages().send(sender, "arena.not-found", "arena", args[0]);
                return;
            }
        } else if (sender instanceof Player player) {
            arena = plugin.getArenaManager().findPlayerArena(player);
        }
        if (arena == null) {
            sender.sendMessage("\u00A7cUsage: /sf " + getUsage());
            return;
        }
        arena.stop("An admin stopped the round.");
        sender.sendMessage("\u00A7aStopped \u00A7f" + arena.getId() + "\u00A7a.");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (Arena arena : plugin.getArenaManager().all()) {
                if (arena.getId().toLowerCase().startsWith(args[0].toLowerCase())) out.add(arena.getId());
            }
        }
        return out;
    }
}
