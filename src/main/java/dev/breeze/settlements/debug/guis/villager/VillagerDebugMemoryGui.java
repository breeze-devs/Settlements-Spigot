package dev.breeze.settlements.debug.guis.villager;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.memories.MemoryClickHandler;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemory;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.guis.CustomInventory;
import dev.breeze.settlements.utils.InventoryUtil;
import dev.breeze.settlements.utils.SoundPresets;
import dev.breeze.settlements.utils.itemstack.ItemPreset;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.npc.Villager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class VillagerDebugMemoryGui implements Listener {

    private static final String INVENTORY_IDENTIFIER = "debug_villager_memory";

    // Register slots for all clickable slots
    private static final int SLOT_PREVIOUS_MENU = InventoryUtil.rowMiddleIndex(6);
    private static final Map<Integer, VillagerMemory<?>> ITEM_SLOT_MEMORY_MAP = new HashMap<>();

    @Nonnull
    public static CustomInventory getViewableInventory(Player player, BaseVillager villager) {
        CustomInventory inventory = new CustomInventory(6, "&9Debug - Villager Memories", new VillagerDebugInventoryHolder(INVENTORY_IDENTIFIER, villager));
        Inventory bukkitInventory = inventory.getBukkitInventory();

        // Create inventory border (no listener needed)
        InventoryUtil.boarderInventory(bukkitInventory, ItemPreset.INVENTORY_BLACK_GLASS_PANE.getBuilder().build());

        // Entity type item (no listener needed)
        bukkitInventory.setItem(InventoryUtil.rowMiddleIndex(1), new ItemStackBuilder(Material.WRITABLE_BOOK).setDisplayName("&a&lVillager Memories").build());

        // Add memory items
        Brain<Villager> brain = villager.getBrain();
        for (VillagerMemory<?> memory : VillagerMemoryType.ALL_MEMORIES) {
            int index = bukkitInventory.firstEmpty();
            bukkitInventory.setItem(index, memory.getGuiItem(brain));
            ITEM_SLOT_MEMORY_MAP.put(index, memory);
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
        } else if (ITEM_SLOT_MEMORY_MAP.containsKey(slot)) {
            VillagerMemory<?> memory = ITEM_SLOT_MEMORY_MAP.get(slot);
            MemoryClickHandler<?> clickHandler = memory.getClickEventHandler();
            if (clickHandler != null) {
                Object memoryValue = memory.get(holder.getVillager().getBrain());
                if (memoryValue != null) {
                    clickHandler.onClick(player, memoryValue);
                    SoundPresets.inventoryClickEnter(player);
                }
            }
        }
    }

}
