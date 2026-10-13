package com.supercraftmc.spadefall.setup;

import com.supercraftmc.spadefall.map.MarkerRole;
import com.supercraftmc.spadefall.map.MarkerTool;

import java.util.UUID;

/**
 * One player's edit-mode state: which arena and map they're on, what the tool
 * will place, and whether glow is showing. Transient by design - resets on
 * logout, and nothing in here is a source of truth. The maps themselves are
 * persisted on every change.
 */
public final class SetupSession {

    private final UUID player;
    private String arenaId;
    private String mapName;
    private boolean glow;

    private MarkerRole role = MarkerRole.SPAWN;
    private int chipValue = 25;
    private MarkerTool.Mode mode = MarkerTool.Mode.REGISTER;

    /** When non-null, the player's next chat line is captured as this kind of name. */
    private Prompt prompt;

    public enum Prompt { ARENA_NAME, MAP_NAME }

    public SetupSession(UUID player) {
        this.player = player;
    }

    public UUID getPlayer() { return player; }

    public String getArenaId() { return arenaId; }
    public void setArenaId(String arenaId) { this.arenaId = arenaId; }

    public String getMapName() { return mapName; }
    public void setMapName(String mapName) { this.mapName = mapName; }

    public boolean isEditing() { return arenaId != null; }

    public boolean isGlow() { return glow; }
    public void setGlow(boolean glow) { this.glow = glow; }

    public MarkerRole getRole() { return role; }
    public void setRole(MarkerRole role) { this.role = role; }

    public int getChipValue() { return chipValue; }
    public void setChipValue(int chipValue) { this.chipValue = Math.max(1, chipValue); }

    public MarkerTool.Mode getMode() { return mode; }
    public void setMode(MarkerTool.Mode mode) { this.mode = mode; }

    public Prompt getPrompt() { return prompt; }
    public void setPrompt(Prompt prompt) { this.prompt = prompt; }

    public void cycleRole() {
        MarkerRole[] values = MarkerRole.values();
        role = values[(role.ordinal() + 1) % values.length];
    }

    /** What the tool will place, for messages: "Player Start", "Chip 25". */
    public String describeRole() {
        String display = SetupStep.displayFor(role);
        // Steps are plural; the tool places one at a time.
        if (role == MarkerRole.SPAWN) display = "Player Start";
        if (role == MarkerRole.DM_SPAWN) display = "Deathmatch Spawn";
        if (role == MarkerRole.SPADE) display = "Spade";
        if (role == MarkerRole.CHIP) display = "Chip " + chipValue;
        if (role == MarkerRole.CHEST) display = "Chest";
        if (role == MarkerRole.LOBBY) display = "Lobby";
        return display;
    }

    public void clear() {
        arenaId = null;
        mapName = null;
        glow = false;
        prompt = null;
    }
}
