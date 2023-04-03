package dev.breeze.settlements.guis;

import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import javax.annotation.Nonnull;

@NoArgsConstructor
public class CustomInventoryHolder implements InventoryHolder {

    @Setter
    private Inventory inventory;

    @Nonnull
    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

}
