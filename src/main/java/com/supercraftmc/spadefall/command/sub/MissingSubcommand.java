package com.supercraftmc.spadefall.command.sub;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.command.Subcommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MissingSubcommand implements Subcommand {

    private final SpadefallPlugin plugin;

    public MissingSubcommand(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getName() { return "missing"; }
    @Override public String getUsage() { return "missing [arena]"; }
    @Override public String getDescription() { return "full setup checklist"; }
    @Override public String getPermission() { return "spadefall.admin"; }

    @Override
    public java.util.List<String> getAliases() { return java.util.List.of("checklist", "status"); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessages().send(sender, "general.player-only");
            return;
        }
        if (args.length > 0 && !plugin.getSetupManager().isEditing(player)) {
            plugin.getSetupManager().enter(player, args[0], false);
            return; // enter() already prints the checklist
        }
        plugin.getSetupManager().sendMissing(player);
    }
}
