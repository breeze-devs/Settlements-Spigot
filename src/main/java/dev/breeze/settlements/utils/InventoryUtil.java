package dev.breeze.settlements.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryUtil {

    public static Inventory createInventory(int rows, String name) {
        // Invalid inventory size
        if (rows < 0 || rows > 6)
            throw new ArithmeticException("Inventory can only be between 3-6 rows!");
        return Bukkit.createInventory(null, rows * 9, MessageUtil.translateColorCode(name));
    }

    public static void boarderInventory(Inventory inv, ItemStack border) {
        int size = inv.getSize();
        for (int a = 0; a < 9; a++)
            inv.setItem(a, border);
        for (int a = size - 9; a < size; a++)
            inv.setItem(a, border);
        for (int a = 0; a < size; a += 9)
            inv.setItem(a, border);
        for (int a = 8; a < size; a += 9)
            inv.setItem(a, border);
    }

    public static void setItems(Inventory inv, ItemStack item, int... slots) {
        for (int slot : slots)
            inv.setItem(slot, item);
    }

    public static void setItems(Inventory inv, ItemStack item, int from, int to) {
        for (int a = from; a < to; a++)
            inv.setItem(a, item);
    }

    public static void addOrDropItem(Inventory inv, ItemStack item, Location toDrop) {
        if (inv.addItem(item).values().isEmpty())
            return;
        // Failed to add to inventory, drop on the ground
        toDrop.getWorld().dropItem(toDrop, item);
    }

}
