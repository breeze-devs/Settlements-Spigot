package dev.breeze.settlements.entities.villagers.inventory;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.utils.LogUtil;
import dev.breeze.settlements.utils.MessageUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class VillagerInventoryEditEvents implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof VillagerInventoryHolder villagerHolder) ||
                !event.getView().getTitle().equals(MessageUtil.translateColorCode("&9Edit Villager Inventory"))) {
            return;
        }

        // Apply items to the villager inventory
        BaseVillager villager = villagerHolder.getVillager();
        VillagerInventory customInventory = villager.getCustomInventory();
        customInventory.clear();

        for (ItemStack item : event.getInventory().getContents()) {
            if (item == null)
                continue;
            customInventory.addItem(parseAmount(item));
        }
    }

    private static ItemStack parseAmount(ItemStack item) {
        if (item == null)
            return null;

        // Check if item has lore
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore())
            return item;

        List<String> loreList = item.getItemMeta().getLore();
        int amount = item.getAmount();

        int i = 0;
        while (i < loreList.size()) {
            String lore = loreList.get(i);
            if (!lore.startsWith(MessageUtil.translateColorCode("&eOver-stacked: &7"))) {
                i++;
                continue;
            }

            // Try to parse the amount
            String amountStr = MessageUtil.stripColor(lore).replace("Over-stacked: ", "");
            try {
                amount = Integer.parseInt(amountStr);
            } catch (NumberFormatException ex) {
                LogUtil.warning("Failed to parse item amount (%s) while editing villager inventory! Using default amount (%s) instead!", amountStr, amount);
            }

            // Remove lore containing the over-stacked amount
            loreList.remove(i);

            // Return a clone of the item
            return new ItemStackBuilder(item)
                    .setAmount(amount)
                    .setLore(loreList)
                    .build();
        }

        return item;
    }

}
