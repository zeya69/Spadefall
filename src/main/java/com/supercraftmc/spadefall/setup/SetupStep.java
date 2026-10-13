package com.supercraftmc.spadefall.setup;

import com.supercraftmc.spadefall.map.MarkerRole;
import org.bukkit.Material;

/**
 * The steps of arena setup, in the order {@code /sf next} walks them.
 *
 * Player-facing names deliberately say "Player Start" rather than "spawn" so
 * nobody confuses a start slot with the lobby again. The file format keeps
 * {@code spadefall:spawn}; only the words people see changed.
 */
public enum SetupStep {

    LOBBY("Lobby", "Where players wait before a round. Stand there and click.",
            null, true, Material.LIME_BED, 10),
    REGION("Region", "The box the map lives in. Auto-sized from your markers by default.",
            null, true, Material.OAK_FENCE, 11),
    STARTS("Player Starts", "One per player slot. The count IS the map's capacity.",
            MarkerRole.SPAWN, true, Material.LIME_CONCRETE, 12),
    FINISH("Finish", "Where the descent ends. Players need somewhere to land.",
            MarkerRole.FINISH, true, Material.RED_CONCRETE, 13),
    SPADES("Spades", "Candidate spots, off the fast line. Each round draws a few.",
            MarkerRole.SPADE, true, Material.PURPLE_CONCRETE, 14),
    CHIPS("Chips", "Candidate pickups with a denomination. Whites on the route, purples on ledges.",
            MarkerRole.CHIP, true, Material.GOLD_BLOCK, 15),
    DM_SPAWNS("Deathmatch Spawns", "Where survivors start the deathmatch. Final map only.",
            MarkerRole.DM_SPAWN, true, Material.ORANGE_CONCRETE, 16),
    CHESTS("Chests", "Optional loot points.",
            MarkerRole.CHEST, false, Material.CHEST, 19),
    REGISTER("Register", "Validates and enables the arena. Unlocks when there are no errors.",
            null, true, Material.NETHER_STAR, 49);

    private final String display;
    private final String help;
    private final MarkerRole role;
    private final boolean required;
    private final Material icon;
    private final int slot;

    SetupStep(String display, String help, MarkerRole role, boolean required, Material icon, int slot) {
        this.display = display;
        this.help = help;
        this.role = role;
        this.required = required;
        this.icon = icon;
        this.slot = slot;
    }

    public String getDisplay() { return display; }
    public String getHelp() { return help; }
    public MarkerRole getRole() { return role; }
    public boolean isRequired() { return required; }
    public Material getIcon() { return icon; }
    public int getSlot() { return slot; }

    public static SetupStep forRole(MarkerRole role) {
        for (SetupStep step : values()) {
            if (step.role == role) {
                return step;
            }
        }
        return null;
    }

    /** The player-facing name for a role - "Player Start", not "spawn". */
    public static String displayFor(MarkerRole role) {
        SetupStep step = forRole(role);
        return step == null ? role.getId() : step.getDisplay();
    }

    /** Singular form for per-marker labels: "Start #3", "Chip", "Spade". */
    public static String labelFor(MarkerRole role) {
        return switch (role) {
            case SPAWN -> "Start";
            case FINISH -> "Finish";
            case SPADE -> "\u2660 Spade";
            case CHIP -> "Chip";
            case DM_SPAWN -> "DM";
            case CHEST -> "Chest";
            case LOBBY -> "Lobby";
        };
    }
}
