package com.supercraftmc.spadefall.command.sub;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.command.Subcommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class NextSubcommand implements Subcommand {

    private final SpadefallPlugin plugin;

    public NextSubcommand(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getName() { return "next"; }
    @Override public String getUsage() { return "next [arena]"; }
    @Override public String getDescription() { return "the one thing to do next"; }
    @Override public String getPermission() { return "spadefall.admin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessages().send(sender, "general.player-only");
            return;
        }
        if (args.length > 0 && !plugin.getSetupManager().isEditing(player)) {
            plugin.getSetupManager().enter(player, args[0], false);
        }
        plugin.getSetupManager().sendNext(player);
    }
}
