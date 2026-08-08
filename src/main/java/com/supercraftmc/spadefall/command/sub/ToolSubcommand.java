package com.supercraftmc.spadefall.command.sub;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.command.Subcommand;
import com.supercraftmc.spadefall.map.MarkerRole;
import com.supercraftmc.spadefall.map.MarkerTool;
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
    @Override public String getUsage() { return "tool [role|value|mode] [arg]"; }
    @Override public String getDescription() { return "get the marker tool"; }
    @Override public String getPermission() { return "spadefall.admin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessages().send(sender, "general.player-only");
            return;
        }

        MarkerTool.Session session = plugin.getMarkerToolListener().session(player);

        if (args.length == 0) {
            player.getInventory().addItem(plugin.getMarkerTool().create());
            plugin.getMessages().send(sender, "tool.given");
            plugin.getMessages().send(sender, "tool.role-selected", "role", session.describe());
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
                plugin.getMessages().send(sender, "tool.role-selected", "role", session.describe());
            }
            case "value" -> {
                if (args.length < 2) {
                    sender.sendMessage("\u00A7cUsage: /sf tool value <1|5|25|100|500>");
                    return;
                }
                try {
                    session.setChipValue(Integer.parseInt(args[1]));
                    plugin.getMessages().send(sender, "tool.role-selected", "role", session.describe());
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
            default -> sender.sendMessage("\u00A7cUsage: /sf " + getUsage());
        }
    }

    private String roleList() {
        StringBuilder builder = new StringBuilder();
        for (MarkerRole role : MarkerRole.values()) {
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(role.getId());
        }
        return builder.toString();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String option : List.of("role", "value", "mode")) {
                if (option.startsWith(args[0].toLowerCase())) {
                    out.add(option);
                }
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "role" -> {
                    for (MarkerRole role : MarkerRole.values()) {
                        if (role.getId().startsWith(args[1].toLowerCase())) {
                            out.add(role.getId());
                        }
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
