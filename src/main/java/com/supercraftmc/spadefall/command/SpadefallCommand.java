package com.supercraftmc.spadefall.command;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.command.sub.ArenaSubcommand;
import com.supercraftmc.spadefall.command.sub.ConfirmSubcommand;
import com.supercraftmc.spadefall.command.sub.DoctorSubcommand;
import com.supercraftmc.spadefall.command.sub.JoinSubcommand;
import com.supercraftmc.spadefall.command.sub.MapSubcommand;
import com.supercraftmc.spadefall.command.sub.ToolSubcommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Root of {@code /spadefall} (alias {@code /sf}).
 */
public final class SpadefallCommand implements CommandExecutor, TabCompleter {

    private final SpadefallPlugin plugin;
    private final Map<String, Subcommand> subcommands = new LinkedHashMap<>();

    public SpadefallCommand(SpadefallPlugin plugin) {
        this.plugin = plugin;
        register(new JoinSubcommand(plugin));
        register(new ArenaSubcommand(plugin));
        register(new MapSubcommand(plugin));
        register(new ToolSubcommand(plugin));
        register(new DoctorSubcommand(plugin));
        register(new ConfirmSubcommand(plugin, false));
        register(new ConfirmSubcommand(plugin, true));
    }

    private void register(Subcommand subcommand) {
        subcommands.put(subcommand.getName().toLowerCase(Locale.ROOT), subcommand);
        for (String alias : subcommand.getAliases()) {
            subcommands.put(alias.toLowerCase(Locale.ROOT), subcommand);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("spadefall.admin")) {
                plugin.getMessages().send(sender, "general.no-permission");
                return true;
            }
            plugin.reloadAll();
            plugin.getMessages().send(sender, "general.reloaded");
            return true;
        }

        Subcommand subcommand = subcommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subcommand == null) {
            plugin.getMessages().send(sender, "general.unknown-command");
            return true;
        }

        if (subcommand.getPermission() != null && !sender.hasPermission(subcommand.getPermission())) {
            plugin.getMessages().send(sender, "general.no-permission");
            return true;
        }

        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        try {
            subcommand.execute(sender, rest);
        } catch (RuntimeException ex) {
            sender.sendMessage(ChatColor.RED + "That command failed: " + ex.getMessage());
            plugin.getLogger().severe("Command /" + label + " " + String.join(" ", args) + " threw: " + ex);
            if (plugin.getConfigManager().isDebug()) {
                ex.printStackTrace();
            }
        }
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "\u2660 Spadefall");
        List<Subcommand> seen = new ArrayList<>();
        for (Subcommand subcommand : subcommands.values()) {
            if (seen.contains(subcommand)) {
                continue;
            }
            seen.add(subcommand);
            if (subcommand.getPermission() != null && !sender.hasPermission(subcommand.getPermission())) {
                continue;
            }
            sender.sendMessage(ChatColor.GRAY + "  /" + label + " " + ChatColor.WHITE
                    + subcommand.getUsage() + ChatColor.DARK_GRAY + " - " + subcommand.getDescription());
        }
        if (sender.hasPermission("spadefall.admin")) {
            sender.sendMessage(ChatColor.GRAY + "  /" + label + " " + ChatColor.WHITE + "reload"
                    + ChatColor.DARK_GRAY + " - reload configuration");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (Map.Entry<String, Subcommand> entry : subcommands.entrySet()) {
                Subcommand subcommand = entry.getValue();
                if (subcommand.getPermission() != null && !sender.hasPermission(subcommand.getPermission())) {
                    continue;
                }
                if (entry.getKey().startsWith(prefix)) {
                    out.add(entry.getKey());
                }
            }
            if ("reload".startsWith(prefix) && sender.hasPermission("spadefall.admin")) {
                out.add("reload");
            }
            return out;
        }

        Subcommand subcommand = subcommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subcommand == null) {
            return out;
        }
        if (subcommand.getPermission() != null && !sender.hasPermission(subcommand.getPermission())) {
            return out;
        }
        return subcommand.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
    }
}
