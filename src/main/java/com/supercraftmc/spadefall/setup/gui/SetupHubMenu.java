package com.supercraftmc.spadefall.setup.gui;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * The entry menu: every arena, plus "create new".
 */
public final class SetupHubMenu {

    private SetupHubMenu() { }

    public static final int CREATE_SLOT = 49;
    public static final int DEMO_SLOT = 47;

    public static void open(SpadefallPlugin plugin, Player player) {
        MenuHolder holder = new MenuHolder(MenuHolder.Kind.HUB);
        Inventory inv = Bukkit.createInventory(holder, 54, "\u00A7a\u00A7l\u2660 \u00A78Spadefall Setup");
        holder.setInventory(inv);

        List<Arena> arenas = new ArrayList<>(plugin.getArenaManager().all());
        int slot = 10;
        for (Arena arena : arenas) {
            if (slot >= 44) break;
            boolean ready = arena.isReady();
            Material icon = ready ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
            String state = ready ? "\u00A7aready" : "\u00A7cnot set up";
            inv.setItem(slot, Items.of(icon, "\u00A7f" + arena.getId(),
                    "\u00A77Status: " + state,
                    "\u00A77Maps: \u00A7f" + arena.getMapNames().size(),
                    "\u00A77Players now: \u00A7f" + arena.getPlayerCount(),
                    "",
                    "\u00A7eClick to edit"));
            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        if (arenas.isEmpty()) {
            inv.setItem(22, Items.of(Material.PAPER, "\u00A77No arenas yet",
                    "\u00A78Click the star below to make one."));
        }

        for (int i = 45; i < 54; i++) inv.setItem(i, Items.filler());
        inv.setItem(CREATE_SLOT, Items.of(Material.NETHER_STAR, "\u00A7a\u00A7lCreate new arena",
                "\u00A77You'll be asked for a name in chat."));
        inv.setItem(DEMO_SLOT, Items.of(Material.MAGMA_BLOCK, "\u00A76\u00A7lTry the bundled map",
                "\u00A77Mount St. Helens - a crater you",
                "\u00A77descend into the lava lake.",
                "",
                "\u00A77Pastes it where you stand and builds",
                "\u00A77a ready arena named \u00A7fdemo\u00A77.",
                "",
                "\u00A7eClick, then confirm in chat"));

        player.openInventory(inv);
    }
}
