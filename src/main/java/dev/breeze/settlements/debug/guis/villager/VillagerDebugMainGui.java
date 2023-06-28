package dev.breeze.settlements.debug.guis.villager;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.ReputationLevels;
import dev.breeze.settlements.guis.CustomInventory;
import dev.breeze.settlements.utils.InventoryUtil;
import dev.breeze.settlements.utils.SoundPresets;
import dev.breeze.settlements.utils.itemstack.ItemPreset;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.itemstack.SkullItemStackBuilder;
import net.minecraft.world.entity.schedule.Activity;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class VillagerDebugMainGui implements Listener {

    private static final String INVENTORY_IDENTIFIER = "debug_villager";

    // Register slots for all clickable slots
    private static final int SLOT_BEHAVIORS = InventoryUtil.toIndex(2, 5);
    private static final int SLOT_MEMORIES = InventoryUtil.toIndex(2, 6);
    private static final int SLOT_SENSORS = InventoryUtil.toIndex(2, 7);
    private static final int SLOT_INVENTORY = InventoryUtil.toIndex(2, 8);

    private static final int SLOT_EMERALD_BALANCE = InventoryUtil.toIndex(3, 2);
    private static final int SLOT_KILL = InventoryUtil.toIndex(3, 8);

    private static final int SLOT_CLOSE_MENU = InventoryUtil.rowMiddleIndex(4);

    @Nonnull
    public static CustomInventory getViewableInventory(Player player, BaseVillager villager) {
        CustomInventory inventory = new CustomInventory(4, "&9Debug - Villager", new VillagerDebugInventoryHolder(INVENTORY_IDENTIFIER, villager));
        Inventory bukkitInventory = inventory.getBukkitInventory();

        // Create inventory border (no listener needed)
        InventoryUtil.boarderInventory(bukkitInventory, ItemPreset.INVENTORY_BLACK_GLASS_PANE.getBuilder().build());

        // Entity type item (no listener needed)
        bukkitInventory.setItem(InventoryUtil.rowMiddleIndex(1), new ItemStackBuilder(Material.VILLAGER_SPAWN_EGG).setDisplayName("&a&lVillager").build());

        // Profession (no listener needed)
        bukkitInventory.setItem(InventoryUtil.toIndex(2, 2), villager.getProfessionGuiItem());

        // Reputation (no listener needed)
        int reputation = villager.getPlayerReputation(((CraftPlayer) player).getHandle());
        bukkitInventory.setItem(InventoryUtil.toIndex(2, 3), new SkullItemStackBuilder()
                .setSkin(player.getName())
                .setDisplayName("&e&lYour Reputation")
                .setLore("%s &7(%d)".formatted(ReputationLevels.getTitle(reputation), reputation))
                .build());

        // Scheduled activity (no listener needed)
        bukkitInventory.setItem(InventoryUtil.toIndex(2, 4), new ItemStackBuilder(Material.CLOCK)
                .setDisplayName("&e&lScheduled Activity")
                .setLore(getVillagerActivityStrings(villager))
                .build());

        // Behavior
        bukkitInventory.setItem(SLOT_BEHAVIORS, new ItemStackBuilder(Material.COMPASS)
                .setDisplayName("&e&lBehaviors")
                .setLore("&7Click to view active behaviors")
                .build());

        // Memories
        bukkitInventory.setItem(SLOT_MEMORIES, new ItemStackBuilder(Material.WRITABLE_BOOK)
                .setDisplayName("&e&lMemories")
                .setLore("&7Click to view villager's memories")
                .build());

        // Sensors
        bukkitInventory.setItem(SLOT_SENSORS, new ItemStackBuilder(Material.SCULK_SENSOR)
                .setDisplayName("&e&lSensors")
                .setLore("&7Click to view villager's sensors")
                .build());

        // Inventory
        bukkitInventory.setItem(SLOT_INVENTORY, new ItemStackBuilder(Material.CHEST)
                .setDisplayName("&e&lInventory")
                .setLore("&7Click to view custom inventory")
                .build());

        // Emerald balance
        bukkitInventory.setItem(SLOT_EMERALD_BALANCE, new ItemStackBuilder(Material.EMERALD)
                .setDisplayName("&e&lEmerald Balance")
                .setLore("&7The villager has &e%d &7emeralds".formatted(villager.getEmeraldBalance()))
                .build());

        // Kill button
        bukkitInventory.setItem(SLOT_KILL, new ItemStackBuilder(Material.BARRIER)
                .setDisplayName("&c&lKill Villager")
                .setLore("&cRight click &7to kill villager", "&7This action will not affect your reputation")
                .build());

        // Exit button
        bukkitInventory.setItem(SLOT_CLOSE_MENU, ItemPreset.INVENTORY_BACK_BUTTON.getBuilder().setDisplayName("&cClose Menu").build());

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
        if (slot == SLOT_BEHAVIORS) {
            // Show villager's registered behaviors
            VillagerDebugBehaviorGui.getViewableInventory(player, holder.getVillager()).showToPlayer(player);
            SoundPresets.inventoryClickEnter(player);
        } else if (slot == SLOT_MEMORIES) {
            // Show villager's memories
            VillagerDebugMemoryGui.getViewableInventory(player, holder.getVillager()).showToPlayer(player);
            SoundPresets.inventoryClickEnter(player);
        } else if (slot == SLOT_SENSORS) {
            // Show villager's sensors
            VillagerDebugSensorGui.getViewableInventory(player, holder.getVillager()).showToPlayer(player);
            SoundPresets.inventoryClickEnter(player);
        } else if (slot == SLOT_INVENTORY) {
            // Show villager's inventory to the player, allowing edits
            holder.getVillager().getCustomInventory().getViewableInventory().showToPlayer(player);
            SoundPresets.inventoryOpen(player);
        } else if (slot == SLOT_KILL) {
            // Kill villager
            if (event.getClick() == ClickType.RIGHT) {
                holder.getVillager().kill();
                player.closeInventory();
                SoundPresets.inventoryClose(player);
            }
        } else if (slot == SLOT_CLOSE_MENU) {
            player.closeInventory();
            SoundPresets.inventoryClose(player);
        }
    }

    @Nonnull
    private static List<String> getVillagerActivityStrings(BaseVillager villager) {
        List<String> lore = new ArrayList<>(Arrays.asList(
                "&7IDLE (10 - 2000 ticks)",
                "&7WORK (2000 - 9000 ticks)",
                "&7MEET (9000 - 11000 ticks)",
                "&7IDLE (11000 - 12000 ticks)",
                "&7REST (12000 - 10 ticks)"
        ));

        // Try to get villager's current activity
        Optional<Activity> optionalActivity = villager.getBrain().getActiveNonCoreActivity();
        if (optionalActivity.isEmpty())
            return lore;
        Activity activity = optionalActivity.get();

        // Determine which activity (index) to highlight
        int highlightIndex = -1;
        if (activity == Activity.REST) {
            highlightIndex = 4;
        } else if (activity == Activity.IDLE) {
            long timeOfDay = villager.level().getWorld().getTime();
            if (timeOfDay < 2000) {
                highlightIndex = 0;
            } else {
                highlightIndex = 3;
            }
        } else if (activity == Activity.WORK) {
            highlightIndex = 1;
        } else if (activity == Activity.MEET) {
            highlightIndex = 2;
        }

        // Highlight the corresponding activity
        if (highlightIndex != -1) {
            lore.set(highlightIndex, lore.get(highlightIndex).replace("&7", "&a"));
        }

        return lore;
    }

}
