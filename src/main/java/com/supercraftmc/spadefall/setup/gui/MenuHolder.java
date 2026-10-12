package com.supercraftmc.spadefall.setup.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marks an inventory as one of ours so the click listener can identify it
 * without string-matching titles. Standard Bukkit pattern.
 */
public final class MenuHolder implements InventoryHolder {

    public enum Kind { HUB, ARENA }

    private final Kind kind;
    private Inventory inventory;

    public MenuHolder(Kind kind) {
        this.kind = kind;
    }

    public Kind getKind() { return kind; }

    void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}
