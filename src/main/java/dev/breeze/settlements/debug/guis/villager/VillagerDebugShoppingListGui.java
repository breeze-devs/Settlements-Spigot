package dev.breeze.settlements.debug.guis.villager;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.guis.CustomInventory;
import dev.breeze.settlements.utils.MessageUtil;
import dev.breeze.settlements.utils.SoundPresets;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class VillagerDebugShoppingListGui implements Listener {

    private static final String INVENTORY_IDENTIFIER = "debug_villager_shopping_list";

    @Nonnull
    public static CustomInventory getViewableInventory(Player player, BaseVillager villager) {
        CustomInventory inventory = new CustomInventory(3, "&9Debug - Villager Shopping List",
                new VillagerDebugInventoryHolder(INVENTORY_IDENTIFIER, villager));
        Inventory bukkitInventory = inventory.getBukkitInventory();

        // Add all items from the villager's shopping list
        HashMap<Material, Integer> shoppingList = VillagerMemoryType.SHOPPING_LIST.get(villager.getBrain());
        if (shoppingList != null) {
            for (Map.Entry<Material, Integer> entry : shoppingList.entrySet()) {
                bukkitInventory.addItem(new ItemStackBuilder(entry.getKey())
                        .setAmount(entry.getValue())
                        .build());
            }
        }

        return inventory;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        // Check event validity
        if (!(inventory.getHolder() instanceof VillagerDebugInventoryHolder holder) || !holder.idMatch(INVENTORY_IDENTIFIER)) {
            return;
        }

        // Update the villager's shopping list with the items in the inventory
        HashMap<Material, Integer> shoppingList = new HashMap<>();
        for (ItemStack item : event.getInventory().getContents()) {
            if (item == null)
                continue;
            // Add or merge the item
            if (shoppingList.containsKey(item.getType())) {
                shoppingList.put(item.getType(), shoppingList.get(item.getType()) + item.getAmount());
            } else {
                shoppingList.put(item.getType(), item.getAmount());
            }
        }
        VillagerMemoryType.SHOPPING_LIST.set(holder.getVillager().getBrain(), shoppingList);

        // Notify player
        MessageUtil.sendMessageWithPrefix(event.getPlayer(), "&aUpdated shopping list for villager!");
        SoundPresets.inventorySave((Player) event.getPlayer());
    }

}
