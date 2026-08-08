package com.supercraftmc.spadefall.command.sub;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.command.Subcommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ArenaSubcommand implements Subcommand {

    private final SpadefallPlugin plugin;

    public ArenaSubcommand(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getName() { return "arena"; }
    @Override public String getUsage() { return "arena create|delete|list|setlobby|maps <name>"; }
    @Override public String getDescription() { return "manage arenas"; }
    @Override public String getPermission() { return "spadefall.admin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("\u00A7cUsage: /sf " + getUsage());
            return;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> create(sender, args);
            case "delete" -> delete(sender, args);
            case "list" -> list(sender);
            case "setlobby" -> setLobby(sender, args);
            case "maps" -> setMaps(sender, args);
            default -> sender.sendMessage("\u00A7cUsage: /sf " + getUsage());
        }
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("\u00A7cUsage: /sf arena create <name>");
            return;
        }
        Arena arena = plugin.getArenaManager().create(args[1]);
        if (arena == null) {
            plugin.getMessages().send(sender, "arena.already-exists", "arena", args[1]);
            return;
        }
        plugin.getMessages().send(sender, "arena.created", "arena", args[1]);
        sender.sendMessage("\u00A77Next: \u00A7f/sf arena maps " + args[1] + " <map> [map2...]");
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("\u00A7cUsage: /sf arena delete <name>");
            return;
        }
        if (plugin.getArenaManager().delete(args[1])) {
            plugin.getMessages().send(sender, "arena.deleted", "arena", args[1]);
        } else {
            plugin.getMessages().send(sender, "arena.not-found", "arena", args[1]);
        }
    }

    private void list(CommandSender sender) {
        plugin.getMessages().send(sender, "arena.list-header",
                "count", String.valueOf(plugin.getArenaManager().size()));
        for (Arena arena : plugin.getArenaManager().all()) {
            plugin.getMessages().sendBare(sender, "arena.list-entry",
                    "arena", arena.getId(),
                    "state", arena.getState().getDisplay(),
                    "players", String.valueOf(arena.getPlayerCount()),
                    "max", String.valueOf(arena.getMaxPlayers()));
        }
    }

    private void setLobby(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessages().send(sender, "general.player-only");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("\u00A7cUsage: /sf arena setlobby <name>");
            return;
        }
        Arena arena = plugin.getArenaManager().get(args[1]);
        if (arena == null) {
            plugin.getMessages().send(sender, "arena.not-found", "arena", args[1]);
            return;
        }
        arena.setLobbySpawn(player.getLocation());
        plugin.getArenaManager().save();
        sender.sendMessage("\u00A7aLobby spawn for \u00A7f" + arena.getId() + "\u00A7a set to your position.");
    }

    private void setMaps(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("\u00A7cUsage: /sf arena maps <arena> <map> [map2...]");
            return;
        }
        Arena arena = plugin.getArenaManager().get(args[1]);
        if (arena == null) {
            plugin.getMessages().send(sender, "arena.not-found", "arena", args[1]);
            return;
        }

        List<String> maps = new ArrayList<>(Arrays.asList(args).subList(2, args.length));
        List<String> missing = new ArrayList<>();
        for (String name : maps) {
            if (!plugin.getMapRegistry().contains(name)) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            sender.sendMessage("\u00A7cUnknown map(s): \u00A7f" + String.join(", ", missing));
            return;
        }

        arena.setMaps(maps);
        plugin.getArenaManager().save();
        sender.sendMessage("\u00A7aArena \u00A7f" + arena.getId() + "\u00A7a now uses: \u00A7f"
                + String.join(" \u00A78> \u00A7f", maps));
        sender.sendMessage("\u00A77Capacity: \u00A7f" + arena.getMaxPlayers()
                + " \u00A78(map allows " + (arena.getFirstMap() == null ? "?" : arena.getFirstMap().getCapacity())
                + ", config allows " + plugin.getConfigManager().getMaxPlayers() + ")");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String option : List.of("create", "delete", "list", "setlobby", "maps")) {
                if (option.startsWith(args[0].toLowerCase())) {
                    out.add(option);
                }
            }
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("create")) {
            for (Arena arena : plugin.getArenaManager().all()) {
                if (arena.getId().toLowerCase().startsWith(args[1].toLowerCase())) {
                    out.add(arena.getId());
                }
            }
        } else if (args.length >= 3 && args[0].equalsIgnoreCase("maps")) {
            String prefix = args[args.length - 1].toLowerCase();
            for (String name : plugin.getMapRegistry().names()) {
                if (name.startsWith(prefix)) {
                    out.add(name);
                }
            }
        }
        return out;
    }
}
