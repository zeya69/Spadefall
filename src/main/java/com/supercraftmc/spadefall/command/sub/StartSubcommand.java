package com.supercraftmc.spadefall.command.sub;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.arena.ArenaState;
import com.supercraftmc.spadefall.command.Subcommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Force start: skips the countdown, or starts an arena with auto-start off. */
public final class StartSubcommand implements Subcommand {

    private final SpadefallPlugin plugin;

    public StartSubcommand(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getName() { return "start"; }
    @Override public String getUsage() { return "start [arena]"; }
    @Override public String getDescription() { return "force a round to start now"; }
    @Override public String getPermission() { return "spadefall.admin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Arena arena = resolve(sender, args);
        if (arena == null) return;

        if (arena.getState() != ArenaState.WAITING && arena.getState() != ArenaState.COUNTDOWN) {
            sender.sendMessage("\u00A7c" + arena.getId() + " is " + arena.getState().getDisplay().toLowerCase() + ".");
            return;
        }
        if (!arena.isReady()) {
            sender.sendMessage("\u00A7c" + arena.getId() + " isn't set up. \u00A77/sf setup " + arena.getId());
            return;
        }
        if (arena.getPlayerCount() < 2) {
            sender.sendMessage("\u00A7cNeed at least 2 players to start. \u00A77(" + arena.getPlayerCount() + " in lobby)");
            return;
        }
        arena.broadcast("&eAn admin started the round.");
        arena.startRound();
    }

    private Arena resolve(CommandSender sender, String[] args) {
        Arena arena = null;
        if (args.length > 0) {
            arena = plugin.getArenaManager().get(args[0]);
            if (arena == null) {
                plugin.getMessages().send(sender, "arena.not-found", "arena", args[0]);
                return null;
            }
        } else if (sender instanceof Player player) {
            arena = plugin.getArenaManager().findPlayerArena(player);
        }
        if (arena == null) {
            sender.sendMessage("\u00A7cUsage: /sf " + getUsage());
        }
        return arena;
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
