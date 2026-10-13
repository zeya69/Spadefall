package com.supercraftmc.spadefall.setup.glow;

import com.supercraftmc.spadefall.map.MarkerRole;
import org.bukkit.Color;
import org.bukkit.Material;

/**
 * Role -> colour. Green, gold and dark purple were the brief; the rest fill
 * the set. Both the glass block and the glow outline use the same colour so
 * a marker reads the same up close and at a distance.
 */
public final class GlowPalette {

    private GlowPalette() { }

    public static Color glow(MarkerRole role) {
        return switch (role) {
            case SPAWN -> Color.fromRGB(0x55FF55);
            case FINISH -> Color.fromRGB(0xFF5555);
            case SPADE -> Color.fromRGB(0x8A2BE2);
            case CHIP -> Color.fromRGB(0xFFC300);
            case DM_SPAWN -> Color.fromRGB(0xFF8C00);
            case CHEST -> Color.fromRGB(0x00CED1);
            case LOBBY -> Color.fromRGB(0x4169E1);
        };
    }

    public static Material glass(MarkerRole role) {
        return switch (role) {
            case SPAWN -> Material.LIME_STAINED_GLASS;
            case FINISH -> Material.RED_STAINED_GLASS;
            case SPADE -> Material.PURPLE_STAINED_GLASS;
            case CHIP -> Material.YELLOW_STAINED_GLASS;
            case DM_SPAWN -> Material.ORANGE_STAINED_GLASS;
            case CHEST -> Material.CYAN_STAINED_GLASS;
            case LOBBY -> Material.BLUE_STAINED_GLASS;
        };
    }

    /** Legacy chat colour matching the glow, for labels. */
    public static String chat(MarkerRole role) {
        return switch (role) {
            case SPAWN -> "\u00A7a";
            case FINISH -> "\u00A7c";
            case SPADE -> "\u00A75";
            case CHIP -> "\u00A76";
            case DM_SPAWN -> "\u00A76";
            case CHEST -> "\u00A7b";
            case LOBBY -> "\u00A79";
        };
    }

    public static final Color REGION_EDGE = Color.fromRGB(0xFFFFFF);
    public static final Material REGION_GLASS = Material.WHITE_STAINED_GLASS;
}
