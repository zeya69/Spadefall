package com.supercraftmc.spadefall.game;

import com.supercraftmc.spadefall.SpadefallPlugin;
import com.supercraftmc.spadefall.arena.Arena;
import com.supercraftmc.spadefall.map.MapDefinition;
import com.supercraftmc.spadefall.map.Marker;
import com.supercraftmc.spadefall.map.MarkerRole;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Chip pickups on the current map.
 *
 * Each round draws {@code chipsFor(playerCount)} candidates from the map's
 * pool at random and spawns a small glowing block for each, coloured by
 * denomination with the value floating above. Walking within a block of one
 * collects it: score and unbanked chips both go up by its face value.
 *
 * Built so spades can reuse it in the next slice - a spade is a pickup with a
 * different look and a different reward.
 */
public final class PickupManager {

    public static final String TAG = "spadefall_pickup";

    private final SpadefallPlugin plugin;
    private final Random random = new Random();

    public PickupManager(SpadefallPlugin plugin) {
        this.plugin = plugin;
    }

    public static final class Pickup {
        final Location location;
        final int value;
        final ItemDisplay block;
        final TextDisplay label;
        int spin;

        Pickup(Location location, int value, ItemDisplay block, TextDisplay label) {
            this.location = location;
            this.value = value;
            this.block = block;
            this.label = label;
        }

        public int getValue() { return value; }
    }

    public void spawnChips(Arena arena, MapDefinition map, int playerCount) {
        World world = Bukkit.getWorld(map.getWorldName());
        if (world == null) return;

        List<Marker> pool = new ArrayList<>(map.get(MarkerRole.CHIP));
        pool.removeIf(m -> m.getValue() <= 0);
        Collections.shuffle(pool, random);
        int wanted = Math.min(pool.size(), plugin.getConfigManager().chipsFor(playerCount));

        List<Pickup> pickups = arena.getPickups();
        for (int i = 0; i < wanted; i++) {
            Marker m = pool.get(i);
            Location loc = new Location(world, m.getX() + 0.5D, m.getY() + 0.1D, m.getZ() + 0.5D);
            pickups.add(spawn(world, loc, m.getValue()));
        }
    }

    private Pickup spawn(World world, Location loc, int value) {
        Material disc = materialFor(value);
        Color colour = colourFor(value);

        // A music disc is a round sprite with a coloured centre - a poker chip.
        // Stood upright and spun it reads as a coin from every angle.
        Location discLoc = loc.clone().add(0.0D, 0.55D, 0.0D);
        ItemDisplay block = world.spawn(discLoc, ItemDisplay.class, d -> {
            d.setItemStack(new org.bukkit.inventory.ItemStack(disc));
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            d.setGlowing(true);
            d.setGlowColorOverride(colour);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setPersistent(false);
            d.addScoreboardTag(TAG);
            d.setInterpolationDuration(5);
            d.setTransformation(new Transformation(new Vector3f(0, 0, 0), new AxisAngle4f(),
                    new Vector3f(0.7f, 0.7f, 0.7f), new AxisAngle4f()));
        });

        TextDisplay label = world.spawn(loc.clone().add(0, 1.15D, 0), TextDisplay.class, d -> {
            d.setText(chatColourFor(value) + "\u00A7l" + value);
            d.setBillboard(Display.Billboard.CENTER);
            d.setBackgroundColor(Color.fromARGB(0x55000000));
            d.setShadowed(true);
            d.setSeeThrough(true);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setPersistent(false);
            d.addScoreboardTag(TAG);
        });

        return new Pickup(loc, value, block, label);
    }

    /** Called every few ticks: bobs the chips and collects any a player is standing in. */
    public void tick(Arena arena) {
        List<Pickup> pickups = arena.getPickups();
        if (pickups.isEmpty()) return;

        List<Player> active = arena.getActivePlayers();
        double radiusSq = plugin.getConfig().getDouble("game.pickup-radius", 1.2D);
        radiusSq *= radiusSq;

        for (int i = pickups.size() - 1; i >= 0; i--) {
            Pickup p = pickups.get(i);
            spin(p);
            for (Player player : active) {
                if (!player.getWorld().equals(p.location.getWorld())) continue;
                if (player.getLocation().distanceSquared(p.location) <= radiusSq) {
                    collect(arena, player, p);
                    pickups.remove(i);
                    break;
                }
            }
        }
    }

    private void spin(Pickup p) {
        p.spin = (p.spin + 1) % 8;
        float angle = (float) (p.spin * Math.PI / 4);
        p.block.setInterpolationDelay(0);
        p.block.setTransformation(new Transformation(new Vector3f(0, 0, 0),
                new AxisAngle4f(angle, 0, 1, 0), new Vector3f(0.7f, 0.7f, 0.7f), new AxisAngle4f()));
    }

    private void collect(Arena arena, Player player, Pickup p) {
        RoundState round = arena.getRound();
        if (round != null) {
            // Chips are the wallet. Score gets a flat amount per chip so a slow
            // collector gets richer but can't out-score the player who got down first.
            round.addScore(player.getUniqueId(), plugin.getConfig().getInt("scoring.points-per-chip", 10));
            round.addChips(player.getUniqueId(), p.value);
        }
        remove(p);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.4f);
        try {
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                            chatColourFor(p.value) + "+" + p.value + " \u00A77chips"));
        } catch (Throwable ignored) { }
    }

    private void remove(Pickup p) {
        if (p.block.isValid()) p.block.remove();
        if (p.label.isValid()) p.label.remove();
    }

    public void clear(Arena arena) {
        for (Pickup p : arena.getPickups()) {
            remove(p);
        }
        arena.getPickups().clear();
    }

    public int sweepStrays() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntities()) {
                if (e.getScoreboardTags().contains(TAG)) {
                    e.remove();
                    removed++;
                }
            }
        }
        return removed;
    }

    // ---- poker colours: his real chipset ---------------------------------

    static Material materialFor(int value) {
        if (value >= 500) return Material.MUSIC_DISC_STAL;   // black
        if (value >= 100) return Material.MUSIC_DISC_WAIT;   // blue
        if (value >= 25) return Material.MUSIC_DISC_CAT;     // green
        if (value >= 5) return Material.MUSIC_DISC_CHIRP;    // red
        return Material.MUSIC_DISC_STRAD;                    // white
    }

    static Color colourFor(int value) {
        if (value >= 500) return Color.fromRGB(0x1A1A1A);
        if (value >= 100) return Color.fromRGB(0x3A6DFF);
        if (value >= 25) return Color.fromRGB(0x55FF55);
        if (value >= 5) return Color.fromRGB(0xFF5555);
        return Color.fromRGB(0xFFFFFF);
    }

    static String chatColourFor(int value) {
        if (value >= 500) return "\u00A78";
        if (value >= 100) return "\u00A79";
        if (value >= 25) return "\u00A7a";
        if (value >= 5) return "\u00A7c";
        return "\u00A7f";
    }
}
