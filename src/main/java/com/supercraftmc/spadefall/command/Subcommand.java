package com.supercraftmc.spadefall.command;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public interface Subcommand {

    String getName();

    String getUsage();

    String getDescription();

    /** Null means everyone may run it. */
    String getPermission();

    void execute(CommandSender sender, String[] args);

    default List<String> getAliases() {
        return Collections.emptyList();
    }

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
