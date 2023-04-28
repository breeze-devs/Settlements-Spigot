package dev.breeze.settlements.debug.guis.villager;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.behaviors.BaseVillagerBehavior;
import dev.breeze.settlements.guis.CustomInventory;
import dev.breeze.settlements.utils.InventoryUtil;
import dev.breeze.settlements.utils.SoundPresets;
import dev.breeze.settlements.utils.itemstack.ItemPreset;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import net.minecraft.world.entity.schedule.Activity;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class VillagerDebugBehaviorGui implements Listener {

    private static final String INVENTORY_IDENTIFIER = "debug_villager_behavior";

    // Register slots for all clickable slots
    private static final int SLOT_PREVIOUS_MENU = InventoryUtil.rowMiddleIndex(6);

    @Nonnull
    public static CustomInventory getViewableInventory(Player player, BaseVillager villager) {
        CustomInventory inventory = new CustomInventory(6, "&9Debug - Villager Behaviors", new VillagerDebugInventoryHolder(INVENTORY_IDENTIFIER, villager));
        Inventory bukkitInventory = inventory.getBukkitInventory();

        // Create inventory border (no listener needed)
        InventoryUtil.boarderInventory(bukkitInventory, ItemPreset.INVENTORY_BLACK_GLASS_PANE.getBuilder().build());

        // Entity type item (no listener needed)
        bukkitInventory.setItem(InventoryUtil.rowMiddleIndex(1), new ItemStackBuilder(Material.COMPASS).setDisplayName("&a&lVillager Behaviors").build());

        // Add memory items
        Optional<Activity> optionalActivity = villager.getBrain().getActiveNonCoreActivity();
        for (Map.Entry<Activity, List<BaseVillagerBehavior>> entry : villager.getActivityBehaviorListMap().entrySet()) {
            boolean isActivityActive = optionalActivity.isPresent() && optionalActivity.get() == entry.getKey();
            for (BaseVillagerBehavior behavior : entry.getValue()) {
                int index = bukkitInventory.firstEmpty();
                bukkitInventory.setItem(index, behavior.getGuiItemBuilder()
                        .insertLore(0, "%sSchedule: %s".formatted(isActivityActive ? "&a" : "&7", entry.getKey().getName().toUpperCase(Locale.ROOT)))
                        .build());
            }
        }

        // Exit button
        bukkitInventory.setItem(SLOT_PREVIOUS_MENU, ItemPreset.INVENTORY_BACK_BUTTON.getBuilder().setDisplayName("&cBack").build());

        return inventory;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Check event validity
        if (!(event.getWhoClicked() instanceof Player player) || !(event.getInventory().getHolder() instanceof VillagerDebugInventoryHolder holder) || !holder.idMatch(INVENTORY_IDENTIFIER)) {
            return;
        }

        // Cancel event
        event.setCancelled(true);

        int slot = event.getSlot();
        if (slot == SLOT_PREVIOUS_MENU) {
            player.openInventory(VillagerDebugMainGui.getViewableInventory(player, holder.getVillager()).getBukkitInventory());
            SoundPresets.inventoryClickExit(player);
        }
    }

}
