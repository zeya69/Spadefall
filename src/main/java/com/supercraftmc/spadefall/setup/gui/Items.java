package com.supercraftmc.spadefall.setup.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Tiny item builder so the menus stay readable. */
final class Items {

    private Items() { }

    static ItemStack of(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lines = new ArrayList<>();
            for (String l : lore) {
                if (l != null) lines.addAll(Arrays.asList(l.split("\n")));
            }
            meta.setLore(lines);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                    org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    static ItemStack filler() {
        return of(Material.BLACK_STAINED_GLASS_PANE, " ");
    }
}
