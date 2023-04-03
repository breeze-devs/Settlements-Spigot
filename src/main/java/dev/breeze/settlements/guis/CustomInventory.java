package dev.breeze.settlements.guis;

import dev.breeze.settlements.utils.MessageUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

@Getter
public final class CustomInventory {

    private final Inventory bukkitInventory;
    private final CustomInventoryHolder inventoryHolder;

    public CustomInventory(int rows, String name) {
        this(rows, name, new CustomInventoryHolder());
    }

    public CustomInventory(int rows, String name, CustomInventoryHolder holder) {
        this.inventoryHolder = holder;
        this.bukkitInventory = Bukkit.createInventory(this.inventoryHolder, rows * 9, MessageUtil.translateColorCode(name));
        this.inventoryHolder.setInventory(this.bukkitInventory);
    }

    public void showToPlayer(Player player) {
        player.openInventory(this.bukkitInventory);
    }

}

