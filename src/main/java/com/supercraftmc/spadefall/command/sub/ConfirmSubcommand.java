package com.supercraftmc.spadefall.command.sub;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.command.Subcommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * The "Y / N" half of the validation prompt.
 *
 * Plugin enable is not an interactive context, so the confirm-or-abort question
 * lives here, where the owner actually is. Warnings are always overridable -
 * the requirement was that the user can continue with whatever they want.
 *
 * Registered twice, once per outcome, so that {@code /sf cancel} genuinely
 * cancels rather than sharing an instance with confirm.
 */
public final class ConfirmSubcommand implements Subcommand {

    private final SpadefallPlugin plugin;
    private final boolean cancel;

    public ConfirmSubcommand(SpadefallPlugin plugin, boolean cancel) {
        this.plugin = plugin;
        this.cancel = cancel;
    }

    @Override public String getName() { return cancel ? "cancel" : "confirm"; }
    @Override public String getUsage() { return cancel ? "cancel" : "confirm"; }
    @Override public String getDescription() {
        return cancel ? "abandon the pending action" : "proceed despite warnings";
    }
    @Override public String getPermission() { return "spadefall.admin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String key = sender instanceof Player player ? player.getUniqueId().toString() : "CONSOLE";
        Runnable pending = plugin.getPendingConfirmations().remove(key);

        if (pending == null) {
            plugin.getMessages().send(sender, "validation.nothing-pending");
            return;
        }
        if (cancel) {
            plugin.getMessages().send(sender, "validation.aborted");
            return;
        }
        pending.run();
    }
}
