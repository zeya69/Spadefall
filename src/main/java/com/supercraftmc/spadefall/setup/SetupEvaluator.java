package com.supercraftmc.spadefall.setup;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.config.ConfigManager;
import com.supercraftmc.spadefall.map.MapDefinition;
import com.supercraftmc.spadefall.map.MarkerRole;
import com.supercraftmc.spadefall.setup.StepStatus.Level;
import com.supercraftmc.spadefall.util.Cuboid;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns an arena plus the map being edited into a status per setup step.
 *
 * This is the validator's logic re-cut as a to-do list: the GUI colours its
 * items from it, {@code /sf missing} prints it, and {@code /sf next} takes the
 * first incomplete entry.
 */
public final class SetupEvaluator {

    private final SpadefallPlugin plugin;

    public SetupEvaluator(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    public List<StepStatus> evaluate(Arena arena, MapDefinition map, boolean isFinalMap) {
        ConfigManager config = plugin.getConfigManager();
        List<StepStatus> out = new ArrayList<>();

        // Lobby
        if (arena.getLobbySpawn() == null) {
            out.add(new StepStatus(SetupStep.LOBBY, Level.ERROR, "not set",
                    "Stand where players should wait and click."));
        } else {
            out.add(new StepStatus(SetupStep.LOBBY, Level.DONE, "set", null));
        }

        // Region
        Cuboid region = map.getRegion();
        if (region == null) {
            out.add(new StepStatus(SetupStep.REGION, Level.ERROR, "nothing to size from",
                    "Auto mode needs at least one marker. Place one, or shift-click for the manual wand."));
        } else if (region.getSizeX() > config.getMaxSizeX() || region.getSizeY() > config.getMaxSizeY()
                || region.getSizeZ() > config.getMaxSizeZ()) {
            out.add(new StepStatus(SetupStep.REGION, Level.ERROR,
                    region.getSizeX() + "\u00D7" + region.getSizeY() + "\u00D7" + region.getSizeZ() + ", too large",
                    "Exceeds max-map-size in config. Tighten the markers or raise the limit."));
        } else {
            String mode = map.getRegionMode() == MapDefinition.RegionMode.AUTO ? "auto" : "manual";
            out.add(new StepStatus(SetupStep.REGION, Level.DONE,
                    mode + " " + region.getSizeX() + "\u00D7" + region.getSizeY() + "\u00D7" + region.getSizeZ(), null));
        }

        // Player starts
        int starts = map.count(MarkerRole.SPAWN);
        int belowFinish = 0;
        if (map.count(MarkerRole.FINISH) > 0) {
            int finishY = map.getFinishY();
            for (var m : map.get(MarkerRole.SPAWN)) {
                if (m.getY() <= finishY) belowFinish++;
            }
        }
        if (starts < 2) {
            out.add(new StepStatus(SetupStep.STARTS, Level.ERROR, starts + " placed, need at least 2",
                    "One per player slot. The count sets the map's capacity."));
        } else if (belowFinish > 0) {
            out.add(new StepStatus(SetupStep.STARTS, Level.ERROR,
                    belowFinish + " at or below the finish",
                    "Players would begin already finished. Move those starts up."));
        } else if (starts < config.getMinPlayers()) {
            out.add(new StepStatus(SetupStep.STARTS, Level.WARNING,
                    starts + " placed, below min-players (" + config.getMinPlayers() + ")",
                    "This map can never start a round until it has more starts or min-players is lowered."));
        } else {
            out.add(new StepStatus(SetupStep.STARTS, Level.DONE,
                    starts + " placed \u00B7 capacity " + map.getEffectiveCapacity(config.getMaxPlayers()), null));
        }

        // Finish
        int finishes = map.count(MarkerRole.FINISH);
        if (finishes == 0) {
            out.add(new StepStatus(SetupStep.FINISH, Level.ERROR, "not placed",
                    "Players need somewhere to land. Place at least one."));
        } else if (map.getDropHeight() < 30) {
            out.add(new StepStatus(SetupStep.FINISH, Level.WARNING,
                    "drop is only " + map.getDropHeight() + " blocks",
                    "Very short for a descent map. Fine for testing."));
        } else {
            out.add(new StepStatus(SetupStep.FINISH, Level.DONE,
                    finishes + " placed \u00B7 drop " + map.getDropHeight(), null));
        }

        // Spades
        int capacity = Math.max(starts, 1);
        int spadesWanted = config.spadesFor(capacity);
        int spades = map.count(MarkerRole.SPADE);
        if (spades == 0) {
            out.add(new StepStatus(SetupStep.SPADES, Level.WARNING, "none placed",
                    "The card table can never be reached on this map."));
        } else if (spades < spadesWanted) {
            out.add(new StepStatus(SetupStep.SPADES, Level.WARNING,
                    spades + " placed, " + spadesWanted + " wanted at " + capacity + " players",
                    "Rounds will place fewer spades than configured."));
        } else if (spades < spadesWanted * 2) {
            out.add(new StepStatus(SetupStep.SPADES, Level.WARNING,
                    spades + " placed \u00B7 " + (spadesWanted * 2) + "+ avoids repeats", null));
        } else {
            out.add(new StepStatus(SetupStep.SPADES, Level.DONE, spades + " candidates", null));
        }

        // Chips
        int chipsWanted = config.chipsFor(capacity);
        int chips = map.count(MarkerRole.CHIP);
        int badChips = 0;
        for (var m : map.get(MarkerRole.CHIP)) {
            if (m.getValue() <= 0) badChips++;
        }
        if (chips == 0) {
            out.add(new StepStatus(SetupStep.CHIPS, Level.WARNING, "none placed",
                    "Nothing to collect on the way down."));
        } else if (badChips > 0) {
            out.add(new StepStatus(SetupStep.CHIPS, Level.ERROR,
                    badChips + " with no denomination",
                    "Set a value with the tool before placing: /sf tool value 25"));
        } else if (chips < chipsWanted) {
            out.add(new StepStatus(SetupStep.CHIPS, Level.WARNING,
                    chips + " placed, " + chipsWanted + " wanted at " + capacity + " players", null));
        } else {
            out.add(new StepStatus(SetupStep.CHIPS, Level.DONE,
                    chips + " candidates \u00B7 pool " + map.getChipPoolValue(), null));
        }

        // Deathmatch spawns
        int dm = map.count(MarkerRole.DM_SPAWN);
        if (!isFinalMap) {
            out.add(new StepStatus(SetupStep.DM_SPAWNS, Level.OPTIONAL, "not the final map", null));
        } else if (dm < 2) {
            out.add(new StepStatus(SetupStep.DM_SPAWNS, Level.ERROR, dm + " placed, need at least 2",
                    "Survivors start the deathmatch here."));
        } else {
            out.add(new StepStatus(SetupStep.DM_SPAWNS, Level.DONE, dm + " placed", null));
        }

        // Chests
        int chests = map.count(MarkerRole.CHEST);
        out.add(new StepStatus(SetupStep.CHESTS, chests == 0 ? Level.OPTIONAL : Level.DONE,
                chests == 0 ? "none (optional)" : chests + " placed", null));

        // Register
        int errors = 0, warnings = 0;
        for (StepStatus s : out) {
            if (s.getLevel() == Level.ERROR) errors++;
            if (s.getLevel() == Level.WARNING) warnings++;
        }
        if (map.isRegistered() && errors == 0) {
            out.add(new StepStatus(SetupStep.REGISTER, Level.DONE, "registered", null));
        } else if (errors > 0) {
            out.add(new StepStatus(SetupStep.REGISTER, Level.ERROR,
                    "blocked by " + errors + " error" + (errors == 1 ? "" : "s"), null));
        } else if (warnings > 0) {
            out.add(new StepStatus(SetupStep.REGISTER, Level.WARNING,
                    "ready, " + warnings + " warning" + (warnings == 1 ? "" : "s"),
                    "You'll be asked to confirm."));
        } else {
            out.add(new StepStatus(SetupStep.REGISTER, Level.WARNING, "ready", "Click to register."));
        }

        return out;
    }

    public StepStatus statusOf(List<StepStatus> statuses, SetupStep step) {
        for (StepStatus s : statuses) {
            if (s.getStep() == step) {
                return s;
            }
        }
        return null;
    }

    /** First incomplete step in walk order, excluding optional ones. Null when nothing is left. */
    public StepStatus next(List<StepStatus> statuses) {
        for (StepStatus s : statuses) {
            if (s.getStep() == SetupStep.CHESTS) continue;
            if (s.getLevel() == Level.ERROR) return s;
        }
        for (StepStatus s : statuses) {
            if (s.getStep() == SetupStep.CHESTS) continue;
            if (s.getLevel() == Level.WARNING && s.getStep() != SetupStep.REGISTER) return s;
        }
        StepStatus register = statusOf(statuses, SetupStep.REGISTER);
        return register != null && register.getLevel() != Level.DONE ? register : null;
    }

    public boolean canRegister(List<StepStatus> statuses) {
        for (StepStatus s : statuses) {
            if (s.getLevel() == Level.ERROR) return false;
        }
        return true;
    }
}
