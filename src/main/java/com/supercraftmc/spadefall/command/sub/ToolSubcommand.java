package com.supercraftmc.spadefall.command.sub;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.command.Subcommand;
import com.supercraftmc.spadefall.map.MapDefinition;
import com.supercraftmc.spadefall.map.MarkerRole;
import com.supercraftmc.spadefall.map.MarkerTool;
import com.supercraftmc.spadefall.setup.SetupSession;
import com.supercraftmc.spadefall.setup.SetupStep;
import com.supercraftmc.spadefall.setup.glow.GlowPalette;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class ToolSubcommand implements Subcommand {

    private final SpadefallPlugin plugin;

    public ToolSubcommand(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getName() { return "tool"; }
    @Override public String getUsage() { return "tool [role|value|mode|status|wand] [arg]"; }
    @Override public String getDescription() { return "get or configure the marker tool"; }
    @Override public String getPermission() { return "spadefall.admin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessages().send(sender, "general.player-only");
            return;
        }
        var setup = plugin.getSetupManager();
        SetupSession session = setup.session(player);

        if (args.length == 0) {
            player.getInventory().addItem(plugin.getMarkerTool().create());
            setup.updateToolItem(player);
            plugin.getMessages().send(sender, "tool.given");
            if (!setup.isEditing(player)) {
                player.sendMessage("\u00A77You're not editing an arena yet - \u00A7f/sf setup\u00A77 first.");
            }
            setup.actionBar(player);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "role" -> {
                if (args.length < 2) {
                    sender.sendMessage("\u00A7cUsage: /sf tool role <" + roleList() + ">");
                    return;
                }
                MarkerRole role = MarkerRole.byId(args[1]);
                if (role == null) {
                    sender.sendMessage("\u00A7cUnknown role. One of: \u00A7f" + roleList());
                    return;
                }
                session.setRole(role);
                setup.updateToolItem(player);
                player.sendMessage("\u00A77Role: " + GlowPalette.chat(role) + session.describeRole());
                setup.actionBar(player);
            }
            case "value" -> {
                if (args.length < 2) {
                    sender.sendMessage("\u00A7cUsage: /sf tool value <1|5|25|100|500>");
                    return;
                }
                try {
                    session.setChipValue(Integer.parseInt(args[1]));
                    session.setRole(MarkerRole.CHIP);
                    setup.updateToolItem(player);
                    player.sendMessage("\u00A77Role: " + GlowPalette.chat(MarkerRole.CHIP) + session.describeRole());
                    setup.actionBar(player);
                } catch (NumberFormatException ex) {
                    plugin.getMessages().send(sender, "general.invalid-number", "input", args[1]);
                }
            }
            case "mode" -> {
                if (args.length < 2) {
                    sender.sendMessage("\u00A7cUsage: /sf tool mode <register|stamp>");
                    return;
                }
                if (args[1].equalsIgnoreCase("stamp")) {
                    session.setMode(MarkerTool.Mode.STAMP);
                    plugin.getMessages().send(sender, "tool.mode-stamp");
                } else {
                    session.setMode(MarkerTool.Mode.REGISTER);
                    plugin.getMessages().send(sender, "tool.mode-register");
                }
            }
            case "wand" -> {
                player.getInventory().addItem(plugin.getMarkerTool().createWand());
                player.sendMessage("\u00A77Region wand: \u00A7fleft-click\u00A77 corner 1, \u00A7fright-click\u00A77 corner 2.");
            }
            case "status" -> status(player, session);
            default -> sender.sendMessage("\u00A7cUsage: /sf " + getUsage());
        }
    }

    private void status(Player player, SetupSession session) {
        MapDefinition map = plugin.getSetupManager().editingMap(player);
        player.sendMessage("\u00A7a\u00A7l\u2660 \u00A7fTool");
        player.sendMessage("\u00A77  Placing: " + GlowPalette.chat(session.getRole()) + session.describeRole());
        player.sendMessage("\u00A77  Mode: \u00A7f" + session.getMode().name().toLowerCase());
        if (map == null) {
            player.sendMessage("\u00A77  Not editing an arena.");
            return;
        }
        player.sendMessage("\u00A77  Map: \u00A7f" + map.getName());
        for (MarkerRole role : MarkerRole.values()) {
            player.sendMessage("    " + GlowPalette.chat(role) + SetupStep.displayFor(role)
                    + "\u00A77: \u00A7f" + map.count(role));
        }
    }

    private String roleList() {
        StringBuilder b = new StringBuilder();
        for (MarkerRole role : MarkerRole.values()) {
            if (b.length() > 0) b.append('|');
            b.append(role.getId());
        }
        return b.toString();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String o : List.of("role", "value", "mode", "status", "wand")) {
                if (o.startsWith(args[0].toLowerCase())) out.add(o);
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "role" -> {
                    for (MarkerRole role : MarkerRole.values()) {
                        if (role.getId().startsWith(args[1].toLowerCase())) out.add(role.getId());
                    }
                }
                case "value" -> out.addAll(List.of("1", "5", "25", "100", "500"));
                case "mode" -> out.addAll(List.of("register", "stamp"));
                default -> { }
            }
        }
        return out;
    }
}
