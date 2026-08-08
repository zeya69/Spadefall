package com.supercraftmc.spadefall.command.sub;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.command.Subcommand;
import com.supercraftmc.spadefall.map.MapDefinition;
import com.supercraftmc.spadefall.map.Marker;
import com.supercraftmc.spadefall.map.MarkerRole;
import com.supercraftmc.spadefall.map.ValidationReport;
import com.supercraftmc.spadefall.util.Cuboid;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /sf map ...} - scanning, listing and saving maps.
 */
public final class MapSubcommand implements Subcommand {

    private final SpadefallPlugin plugin;

    public MapSubcommand(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getName() { return "map"; }
    @Override public String getUsage() { return "map pos1|pos2|scan|save|list|info|delete"; }
    @Override public String getDescription() { return "define and scan maps"; }
    @Override public String getPermission() { return "spadefall.admin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("\u00A7cUsage: /sf " + getUsage());
            return;
        }
        switch (args[0].toLowerCase()) {
            case "pos1" -> setCorner(sender, 1);
            case "pos2" -> setCorner(sender, 2);
            case "scan" -> scan(sender, args);
            case "save" -> save(sender, args);
            case "list" -> list(sender);
            case "info" -> info(sender, args);
            case "delete" -> delete(sender, args);
            default -> sender.sendMessage("\u00A7cUsage: /sf " + getUsage());
        }
    }

    private void setCorner(CommandSender sender, int which) {
        if (!(sender instanceof Player player)) {
            plugin.getMessages().send(sender, "general.player-only");
            return;
        }
        Location location = player.getLocation();
        if (which == 1) {
            plugin.getSelectionPos1().put(player.getUniqueId(), location);
        } else {
            plugin.getSelectionPos2().put(player.getUniqueId(), location);
        }
        sender.sendMessage("\u00A7aPosition " + which + " set to \u00A7f"
                + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
    }

    private void scan(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessages().send(sender, "general.player-only");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("\u00A7cUsage: /sf map scan <name>");
            return;
        }

        Location a = plugin.getSelectionPos1().get(player.getUniqueId());
        Location b = plugin.getSelectionPos2().get(player.getUniqueId());
        if (a == null || b == null) {
            sender.sendMessage("\u00A7cSet both corners first: \u00A7f/sf map pos1\u00A7c and \u00A7f/sf map pos2");
            return;
        }
        if (a.getWorld() == null || b.getWorld() == null || !a.getWorld().equals(b.getWorld())) {
            sender.sendMessage("\u00A7cBoth corners must be in the same world.");
            return;
        }

        Cuboid region = Cuboid.between(a, b);
        String name = args[1];

        plugin.getMessages().send(sender, "map.scanning");
        sender.sendMessage("\u00A78  region " + region + " \u00A78(" + region.getVolume() + " blocks)");

        plugin.getMarkerScanner().scan(name, region,
                percent -> { },
                definition -> onScanned(sender, definition),
                reason -> plugin.getMessages().send(sender, "map.load-failed",
                        "map", name, "reason", reason));
    }

    private void onScanned(CommandSender sender, MapDefinition definition) {
        // Fold in anything the player placed with the marker tool in REGISTER mode.
        if (sender instanceof Player player) {
            List<Marker> pending = plugin.getPendingMarkers().get(player.getUniqueId());
            if (pending != null && !pending.isEmpty()) {
                int added = 0;
                for (Marker marker : pending) {
                    if (definition.getRegion().contains(marker.getX(), marker.getY(), marker.getZ())) {
                        definition.add(marker);
                        added++;
                    }
                }
                if (added > 0) {
                    sender.sendMessage("\u00A77Merged \u00A7f" + added
                            + "\u00A77 marker(s) placed with the tool.");
                }
                pending.clear();
            }
        }

        plugin.getMessages().send(sender, "map.loaded",
                "map", definition.getName(),
                "markers", String.valueOf(definition.totalMarkers()),
                "capacity", String.valueOf(definition.getCapacity()));

        summarise(sender, definition);

        ValidationReport report = plugin.getMapValidator().validate(definition, true);
        printReport(sender, report);

        if (report.hasErrors()) {
            sender.sendMessage("\u00A7cMap not saved - fix the errors above and rescan.");
            return;
        }

        if (report.needsConfirmation()) {
            plugin.getPendingConfirmations().put(senderKey(sender), () -> {
                plugin.getMapRegistry().register(definition);
                sender.sendMessage("\u00A7aSaved \u00A7f" + definition.getName() + "\u00A7a anyway.");
            });
            plugin.getMessages().sendBare(sender, "validation.confirm");
            return;
        }

        plugin.getMapRegistry().register(definition);
        sender.sendMessage("\u00A7aSaved \u00A7f" + definition.getName() + "\u00A7a.");
    }

    private void save(CommandSender sender, String[] args) {
        sender.sendMessage("\u00A77Maps are saved automatically after a successful scan.");
    }

    private void summarise(CommandSender sender, MapDefinition definition) {
        sender.sendMessage("\u00A78  spawns \u00A7f" + definition.count(MarkerRole.SPAWN)
                + "  \u00A78finish \u00A7f" + definition.count(MarkerRole.FINISH)
                + "  \u00A78spade \u00A7f" + definition.count(MarkerRole.SPADE)
                + "  \u00A78chip \u00A7f" + definition.count(MarkerRole.CHIP)
                + "  \u00A78dm \u00A7f" + definition.count(MarkerRole.DM_SPAWN));
        sender.sendMessage("\u00A78  drop \u00A7f" + definition.getDropHeight()
                + "\u00A78 blocks, finish Y \u00A7f" + definition.getFinishY()
                + "\u00A78, void plane Y \u00A7f"
                + definition.getVoidPlane(plugin.getConfigManager().getVoidMargin()));
    }

    private void printReport(CommandSender sender, ValidationReport report) {
        plugin.getMessages().sendBare(sender, "validation.header", "map", report.getMapName());
        if (report.isClean()) {
            plugin.getMessages().sendBare(sender, "validation.ok");
            return;
        }
        for (String error : report.getErrors()) {
            plugin.getMessages().sendBare(sender, "validation.error", "message", error);
        }
        for (String warning : report.getWarnings()) {
            plugin.getMessages().sendBare(sender, "validation.warning", "message", warning);
        }
    }

    private void list(CommandSender sender) {
        sender.sendMessage("\u00A77Maps (\u00A7f" + plugin.getMapRegistry().size() + "\u00A77):");
        for (MapDefinition definition : plugin.getMapRegistry().all()) {
            sender.sendMessage("\u00A78 - \u00A7f" + definition.getName()
                    + " \u00A78cap \u00A7f" + definition.getCapacity()
                    + " \u00A78drop \u00A7f" + definition.getDropHeight()
                    + " \u00A78markers \u00A7f" + definition.totalMarkers());
        }
    }

    private void info(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("\u00A7cUsage: /sf map info <name>");
            return;
        }
        MapDefinition definition = plugin.getMapRegistry().get(args[1]);
        if (definition == null) {
            plugin.getMessages().send(sender, "map.not-found", "map", args[1]);
            return;
        }
        sender.sendMessage("\u00A7a\u00A7l" + definition.getName());
        sender.sendMessage("\u00A78  region \u00A7f" + definition.getRegion());
        summarise(sender, definition);
        sender.sendMessage("\u00A78  chip pool value \u00A7f" + definition.getChipPoolValue());
        printReport(sender, plugin.getMapValidator().validate(definition, true));
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("\u00A7cUsage: /sf map delete <name>");
            return;
        }
        if (plugin.getMapRegistry().remove(args[1])) {
            sender.sendMessage("\u00A7aDeleted map \u00A7f" + args[1]);
        } else {
            plugin.getMessages().send(sender, "map.not-found", "map", args[1]);
        }
    }

    private String senderKey(CommandSender sender) {
        return sender instanceof Player player ? player.getUniqueId().toString() : "CONSOLE";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String option : List.of("pos1", "pos2", "scan", "list", "info", "delete")) {
                if (option.startsWith(args[0].toLowerCase())) {
                    out.add(option);
                }
            }
        } else if (args.length == 2 && List.of("info", "delete").contains(args[0].toLowerCase())) {
            for (String name : plugin.getMapRegistry().names()) {
                if (name.startsWith(args[1].toLowerCase())) {
                    out.add(name);
                }
            }
        }
        return out;
    }
}
