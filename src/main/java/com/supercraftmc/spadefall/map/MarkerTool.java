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
 *              can be exported and shared as self-describing. This is how a
 *              community map library actually starts: download a map, mark it
 *              up once, republish it.
 */
public final class MarkerTool {

    public enum Mode { REGISTER, STAMP }

    public static final Material TOOL_MATERIAL = Material.GOLDEN_SHOVEL;

    private final NamespacedKey toolKey;

    public MarkerTool(SpadefallPlugin plugin) {
        this.toolKey = new NamespacedKey(plugin, "marker_tool");
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(TOOL_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("\u00A7a\u00A7l\u2660 \u00A7fSpadefall Marker Tool");
            List<String> lore = new ArrayList<>();
            lore.add("\u00A77Right-click  \u00A78place marker");
            lore.add("\u00A77Left-click   \u00A78remove marker");
            lore.add("\u00A77Shift+Right  \u00A78change role");
            lore.add("");
            lore.add("\u00A78Selected role shows on your action bar.");
            meta.setLore(lore);
            meta.setUnbreakable(true);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(toolKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isTool(ItemStack item) {
        if (item == null || item.getType() != TOOL_MATERIAL || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(toolKey, PersistentDataType.BYTE);
    }

    /** Per-player tool state. Deliberately transient - resets on restart. */
    public static final class Session {
        private MarkerRole role = MarkerRole.SPAWN;
        private int chipValue = 25;
        private Mode mode = Mode.REGISTER;
        private String editingMap;

        public MarkerRole getRole() { return role; }
        public void setRole(MarkerRole role) { this.role = role; }
        public int getChipValue() { return chipValue; }
        public void setChipValue(int chipValue) { this.chipValue = Math.max(1, chipValue); }
        public Mode getMode() { return mode; }
        public void setMode(Mode mode) { this.mode = mode; }
        public String getEditingMap() { return editingMap; }
        public void setEditingMap(String editingMap) { this.editingMap = editingMap; }

        public void cycleRole() {
            MarkerRole[] values = MarkerRole.values();
            role = values[(role.ordinal() + 1) % values.length];
        }

        public String describe() {
            return role.isValued()
                    ? role.getId() + ":" + chipValue
                    : role.getId();
        }
    }
}
