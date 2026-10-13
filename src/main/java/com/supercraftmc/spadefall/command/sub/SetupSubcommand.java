package com.supercraftmc.spadefall.command.sub;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.command.Subcommand;
import com.supercraftmc.spadefall.setup.SetupStep;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /sf setup} - the front door.
 *
 *   /sf setup                open the hub, or the arena menu if already editing
 *   /sf setup <arena>        enter edit mode for an arena (created if new)
 *   /sf setup chat           the chat checklist instead of the GUI
 *   /sf setup done           leave edit mode
 *   /sf setup demo           paste the bundled St. Helens map and build an arena around it
 *   /sf setup step <STEP>    internal: what the chat [buttons] run
 *   /sf setup cancelname     internal: abandon a name prompt
 */
public final class SetupSubcommand implements Subcommand {

    private final SpadefallPlugin plugin;

    public SetupSubcommand(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getName() { return "setup"; }
    @Override public String getUsage() { return "setup [arena|chat|done|demo]"; }
    @Override public String getDescription() { return "set up an arena in-game"; }
    @Override public String getPermission() { return "spadefall.admin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessages().send(sender, "general.player-only");
            return;
        }
        var setup = plugin.getSetupManager();

        if (args.length == 0) {
            if (setup.isEditing(player)) {
                com.supercraftmc.spadefall.setup.gui.ArenaSetupMenu.open(plugin, player);
            } else {
                setup.openHub(player);
            }
            return;
        }

        switch (args[0].toLowerCase()) {
            case "chat" -> {
                if (setup.isEditing(player)) {
                    setup.sendMissing(player);
                } else if (args.length > 1) {
                    setup.enter(player, args[1], false);
                } else {
                    player.sendMessage("\u00A7cUsage: /sf setup chat <arena>");
                }
            }
            case "done", "exit", "leave" -> setup.exit(player);
            case "step" -> {
                if (args.length < 2) return;
                SetupStep step;
                try {
                    step = SetupStep.valueOf(args[1].toUpperCase());
                } catch (IllegalArgumentException ex) {
                    return;
                }
                setup.stepAction(player, step);
            }
            case "cancelname" -> setup.cancelPrompt(player);
            case "demo" -> plugin.getMapLoader().demo(player);
            case "create" -> {
                if (args.length < 2) {
                    setup.promptArenaName(player);
                } else {
                    setup.enter(player, args[1], true);
                }
            }
            default -> setup.enter(player, args[0], true);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String p = args[0].toLowerCase();
            for (String s : List.of("chat", "done", "create", "demo")) {
                if (s.startsWith(p)) out.add(s);
            }
            for (Arena arena : plugin.getArenaManager().all()) {
                if (arena.getId().toLowerCase().startsWith(p)) out.add(arena.getId());
            }
        }
        return out;
    }
}
