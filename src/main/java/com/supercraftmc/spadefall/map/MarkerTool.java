package com.supercraftmc.spadefall.map;

import com.supercraftmc.spadefall.SpadefallPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * The golden shovel that turns any downloaded map into a Spadefall map.
 *
 * A spade, obviously.
 *
 * Two modes exist for a reason:
 *
 *   REGISTER - positions are recorded in our own data and the world is left
 *              untouched. For maps you do not want to modify.
 *   STAMP    - real structure blocks are written into the world, so the map
 *              can be exported and shared as self-describing.
 *
 * Per-player state (selected role, mode, chip value) lives in the player's
 * SetupSession, not on the item, so the tool can be dropped, duplicated or
 * handed over without any of that mattering.
 */
public final class MarkerTool {

    public enum Mode { REGISTER, STAMP }

    public static final Material TOOL_MATERIAL = Material.GOLDEN_SHOVEL;
    public static final Material WAND_MATERIAL = Material.GOLDEN_AXE;

    private final NamespacedKey toolKey;
    private final NamespacedKey wandKey;

    public MarkerTool(SpadefallPlugin plugin) {
        this.toolKey = new NamespacedKey(plugin, "marker_tool");
        this.wandKey = new NamespacedKey(plugin, "region_wand");
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(TOOL_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("\u00A7a\u00A7l\u2660 \u00A7fMarker Tool");
            List<String> lore = new ArrayList<>();
            lore.add("\u00A77Right-click         \u00A78place marker");
            lore.add("\u00A77Left-click          \u00A78what's here?");
            lore.add("\u00A77Shift + Left-click  \u00A78remove marker");
            lore.add("\u00A77Shift + Right-click \u00A78next role");
            lore.add("\u00A77Shift + scroll      \u00A78next role");
            lore.add("");
            lore.add("\u00A78Selected role shows on your action bar.");
            meta.setLore(lore);
            meta.setUnbreakable(true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE,
                    org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(toolKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** The manual region selector, for when auto-region isn't right. */
    public ItemStack createWand() {
        ItemStack item = new ItemStack(WAND_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("\u00A7a\u00A7l\u2660 \u00A7fRegion Wand");
            List<String> lore = new ArrayList<>();
            lore.add("\u00A77Left-click   \u00A78corner 1");
            lore.add("\u00A77Right-click  \u00A78corner 2");
            lore.add("");
            lore.add("\u00A78Sets a manual region for the map you're editing.");
            meta.setLore(lore);
            meta.setUnbreakable(true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE,
                    org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isTool(ItemStack item) {
        return has(item, TOOL_MATERIAL, toolKey);
    }

    public boolean isWand(ItemStack item) {
        return has(item, WAND_MATERIAL, wandKey);
    }

    private boolean has(ItemStack item, Material material, NamespacedKey key) {
        if (item == null || item.getType() != material || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(key, PersistentDataType.BYTE);
    }
}
