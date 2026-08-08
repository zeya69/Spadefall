package com.supercraftmc.spadefall.command.sub;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.command.Subcommand;
import com.supercraftmc.spadefall.map.MapDefinition;
import com.supercraftmc.spadefall.map.ValidationReport;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /sf doctor} - re-runs validation across every map and arena.
 *
 * The point of a public plugin having this is that most support requests are
 * really configuration problems, and this turns "it doesn't work" into a list.
 */
public final class DoctorSubcommand implements Subcommand {

    private final SpadefallPlugin plugin;

    public DoctorSubcommand(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getName() { return "doctor"; }
    @Override public String getUsage() { return "doctor [map]"; }
    @Override public String getDescription() { return "check maps and config for problems"; }
    @Override public String getPermission() { return "spadefall.admin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length > 0) {
            MapDefinition definition = plugin.getMapRegistry().get(args[0]);
            if (definition == null) {
                plugin.getMessages().send(sender, "map.not-found", "map", args[0]);
                return;
            }
            report(sender, plugin.getMapValidator().validate(definition, true));
            return;
        }

        sender.sendMessage("\u00A7a\u00A7l\u2660 Spadefall doctor");

        if (plugin.getMapRegistry().size() == 0) {
            sender.sendMessage("\u00A7e  \u26A0 No maps defined. Start with /sf map pos1, pos2, scan <name>.");
        }
        if (plugin.getArenaManager().size() == 0) {
            sender.sendMessage("\u00A7e  \u26A0 No arenas defined. Start with /sf arena create <name>.");
        }

        for (MapDefinition definition : plugin.getMapRegistry().all()) {
            report(sender, plugin.getMapValidator().validate(definition, true));
        }

        for (var arena : plugin.getArenaManager().all()) {
            if (arena.getMapNames().isEmpty()) {
                sender.sendMessage("\u00A7c  \u2718 Arena \u00A7f" + arena.getId()
                        + "\u00A7c has no maps assigned.");
            } else if (arena.getMapNames().size() < plugin.getConfigManager().getMapsPerRound()) {
                sender.sendMessage("\u00A7e  \u26A0 Arena \u00A7f" + arena.getId() + "\u00A7e has "
                        + arena.getMapNames().size() + " map(s) but maps-per-round is "
                        + plugin.getConfigManager().getMapsPerRound() + ".");
            }
        }

        heapAdvice(sender);
    }

    private void heapAdvice(CommandSender sender) {
        long maxHeapMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
        int arenas = plugin.getConfigManager().getMaxConcurrentArenas();
        int players = plugin.getConfigManager().getMaxPlayers();

        sender.sendMessage("\u00A78  heap \u00A7f" + maxHeapMb + "MB\u00A78, max arenas \u00A7f"
                + arenas + "\u00A78, max players \u00A7f" + players);

        // Rough guide: an arena with a modest map wants about 700MB of headroom.
        long wanted = (long) arenas * 700L;
        if (wanted > maxHeapMb) {
            sender.sendMessage("\u00A7e  \u26A0 " + arenas + " concurrent arenas on " + maxHeapMb
                    + "MB is optimistic. Consider max-concurrent-arenas: "
                    + Math.max(1, (int) (maxHeapMb / 700L)) + ".");
        }
    }

    private void report(CommandSender sender, ValidationReport validation) {
        plugin.getMessages().sendBare(sender, "validation.header", "map", validation.getMapName());
        if (validation.isClean()) {
            plugin.getMessages().sendBare(sender, "validation.ok");
            return;
        }
        for (String error : validation.getErrors()) {
            plugin.getMessages().sendBare(sender, "validation.error", "message", error);
        }
        for (String warning : validation.getWarnings()) {
            plugin.getMessages().sendBare(sender, "validation.warning", "message", warning);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String name : plugin.getMapRegistry().names()) {
                if (name.startsWith(args[0].toLowerCase())) {
                    out.add(name);
                }
            }
        }
        return out;
    }
}
