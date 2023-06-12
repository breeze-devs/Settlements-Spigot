package dev.breeze.settlements.entities.villagers.inventory;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.guis.CustomInventory;
import dev.breeze.settlements.utils.LogUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.itemstack.ItemUtil;
import lombok.Getter;
import net.minecraft.nbt.*;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VillagerInventory {

    public static final int DEFAULT_INVENTORY_ROWS = 2;

    /**
     * The maximum number of items that a villager can hold
     * - if the same item is added afterward, it will be "deleted"
     */
    public static final int MAX_OVER_STACKED_AMOUNT = 64 * 10;

    @Nonnull
    private final BaseVillager villager;

    private final int rows;

    @Getter
    @Nonnull
    private final ItemStack[] items;

    /**
     * Constructs a new VillagerInventory instance with the specified number of rows.
     *
     * @param villager the custom villager associated with this inventory
     * @param rows     the number of rows in the inventory, between 1 and 6 (inclusive)
     * @throws IllegalArgumentException if the number of rows is outside the valid range
     */
    public VillagerInventory(@Nonnull BaseVillager villager, int rows) throws IllegalArgumentException {
        if (rows < 1 || rows > 6)
            throw new IllegalArgumentException("Inventory rows cannot be more than 6, got %d".formatted(rows));
        this.villager = villager;

        this.rows = rows;
        this.items = new ItemStack[this.rows * 9];
    }

    /**
     * Constructs a new VillagerInventory instance from the specified NBT tag
     *
     * @param nbt the NBT tag to read the inventory data from
     * @return a new VillagerInventory instance with the data from the NBT tag
     * @throws NullPointerException if the NBT tag is null
     * @throws ClassCastException   if the NBT tag is not of the expected type
     */
    @Nonnull
    public static VillagerInventory fromNbt(@Nonnull BaseVillager villager, @Nonnull CompoundTag nbt) {
        int rows = nbt.getInt("rows");
        VillagerInventory inventory = new VillagerInventory(villager, rows);

        ListTag itemTags = nbt.getList("items", Tag.TAG_STRING);
        for (Tag itemTag : itemTags) {
            String base64 = itemTag.getAsString();
            ItemStack item = ItemUtil.deserialize(base64);
            if (item == null) {
                LogUtil.severe("Deserialization of itemstack in villager inventory failed! Ignoring itemstack!");
                LogUtil.severe("B64: %s", base64);
                continue;
            }
            inventory.addItem(ItemUtil.deserialize(base64));
        }

        return inventory;
    }

    /**
     * Serializes this VillagerInventory as a CompoundTag in NBT format
     * <p>
     * The CompoundTag contains two fields:
     * - "rows", an integer representing the number of rows in the inventory
     * - "items", a ListTag containing serialized ItemStacks representing the items in the inventory
     * <p>
     * The ItemStacks are serialized as Base64 strings using the ItemUtil class
     * <p>
     * Empty slots in the inventory are skipped, and if an ItemStack cannot be serialized, it is ignored and a warning is logged
     * <p>
     *
     * @return the serialized inventory as a CompoundTag in NBT format
     */
    @Nonnull
    public CompoundTag toNbtTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("rows", IntTag.valueOf(this.rows));

        ListTag itemTag = new ListTag();
        for (ItemStack item : this.items) {
            // Ignore empty slots
            if (item == null)
                continue;

            // Serialize item into B64 string
            String base64 = ItemUtil.serialize(item);
            if (base64 == null) {
                LogUtil.severe("Serialization of itemstack in villager inventory failed! Ignoring itemstack!");
                LogUtil.severe("Item: %s", item.toString());
                continue;
            }
            itemTag.add(StringTag.valueOf(base64));
        }

        // Add the item list to the tag
        tag.put("items", itemTag);

        return tag;
    }

    /**
     * Creates a Bukkit inventory replica of the villager inventory
     * <p>
     * Note that over-stacked items set to max stack and appended with the lore "Over-stacked: amount"
     * <p>
     * This method is primarily used for debugging & editing items
     *
     * @return the Bukkit viewable inventory
     */
    @Nonnull
    public CustomInventory getViewableInventory() {
        CustomInventory inventory = new CustomInventory(this.rows, "&9Edit Villager Inventory", new VillagerInventoryHolder(this.villager));
        Inventory bukkitInventory = inventory.getBukkitInventory();
        for (int i = 0; i < this.items.length; i++) {
            if (this.items[i] == null)
                continue;

            // Clone item
            ItemStack item = this.items[i].clone();

            // Check if item is over-stacked
            if (item.getAmount() > item.getMaxStackSize()) {
                // Append lore
                item = new ItemStackBuilder(item)
                        .setAmount(item.getMaxStackSize())
                        .appendLore("&eOver-stacked: &7%d".formatted(item.getAmount()))
                        .build();
            }

            bukkitInventory.setItem(i, item);
        }
        return inventory;
    }

    /**
     * Returns how many slot this inventory has
     *
     * @return the number of slots in this inventory
     */
    public int getSize() {
        return this.rows * 9;
    }

    /**
     * Returns the item in the target slot
     *
     * @param index the index of the slot to retrieve the item from
     * @return the ItemStack in the specified slot, or null if the slot is empty
     * @throws ArrayIndexOutOfBoundsException if the specified index is outside the range of valid slot indices
     */
    @Nullable
    public ItemStack getItem(int index) throws ArrayIndexOutOfBoundsException {
        if (index >= this.rows * 9)
            throw new ArrayIndexOutOfBoundsException("Trying to get item at slot %d in an inventory with size %d!".formatted(index, this.rows * 9));
        return this.items[index];
    }

    /**
     * Replaces the item in the target slot with the given one, or clears it if the given item is null
     *
     * @param index the slot index to set the item in
     * @param item  the item to set in the slot, or null to clear the slot
     * @throws ArrayIndexOutOfBoundsException if the index is outside the range of valid slot indices
     */
    public void setItem(int index, @Nullable ItemStack item) throws ArrayIndexOutOfBoundsException {
        if (index >= this.rows * 9)
            throw new ArrayIndexOutOfBoundsException("Trying to set item at slot %d in an inventory with size %d!".formatted(index, this.rows * 9));
        this.items[index] = item;
    }

    /**
     * Adds one or more items to the inventory, ignoring the stack limit.
     * - although this isn't Minecraft-y, it's for ease-of-coding
     * - tl;dr I'm lazy
     *
     * @param newItems The items to add to the inventory
     * @return A list of items that were not added due to a lack of space in the inventory
     */
    @Nonnull
    public List<ItemStack> addItem(@Nonnull ItemStack... newItems) {
        List<ItemStack> notAdded = new ArrayList<>();

        for (ItemStack toAdd : newItems) {
            if (toAdd == null)
                continue;

            boolean added = false;

            // Check if inventory already contains a similar item
            for (ItemStack loop : this.items) {
                if (loop == null || !loop.isSimilar(toAdd))
                    continue;
                // Item already exists, increment amount
                loop.setAmount(Math.min(MAX_OVER_STACKED_AMOUNT, loop.getAmount() + toAdd.getAmount()));
                added = true;
                break;
            }

            // If the inventory does not contain a similar item, we add to a new slot
            if (!added) {
                int slot = this.firstEmpty();
                if (slot == -1) {
                    // Inventory is filled, add to the notAdded list
                    notAdded.add(toAdd);
                } else {
                    this.items[slot] = toAdd;
                }
            }
        }

        return notAdded;
    }

    /**
     * Attempts to remove the specified count of items from the inventory
     * If the inventory contains less than the specified count,
     * the method will remove as many items as possible and return the remaining count as a negative number.
     * <p>
     * Therefore, it is recommended to check the inventory using {@link #contains(ItemStack)} or {@link #count(ItemStack)}
     * before calling this method.
     *
     * @param toRemove the item to remove
     * @param count    the number of items to remove
     * @return the number of items remaining in the inventory after removal, which can be negative
     */
    public int remove(@Nonnull ItemStack toRemove, int count) {
        for (int i = 0; i < this.items.length; i++) {
            ItemStack loop = this.items[i];
            if (loop == null || !loop.isSimilar(toRemove)) {
                continue;
            }

            // Attempt to remove item
            if (loop.getAmount() > count) {
                // If enough, return remaining item count
                int remaining = loop.getAmount() - count;
                loop.setAmount(remaining);
                return remaining;
            } else {
                // Not enough to remove, subtract amount and clear slot
                count -= loop.getAmount();
                this.setItem(i, null);
            }
        }
        return -count;
    }

    /**
     * Checks whether the inventory contains any number of the specified item
     *
     * @param item the item to check for
     * @return true if the inventory contains any number of the specified item, false otherwise
     */
    public boolean contains(@Nonnull ItemStack item) {
        for (ItemStack loop : this.items) {
            if (loop != null && loop.isSimilar(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the inventory contains at least the specified amount of the specified item
     *
     * @param item   the item to check for
     * @param amount the minimum amount of the item to check for
     * @return true if the inventory contains at least the specified amount of the item, false otherwise
     */
    public boolean contains(@Nonnull ItemStack item, int amount) {
        return this.count(item) > amount;
    }

    public boolean contains(@Nonnull Material material, int amount) {
        return this.count(material) > amount;
    }

    /**
     * Checks whether the inventory can fit at least one 'stack' of the specified item
     *
     * @param item the item to check for
     * @return true if the inventory already contains a similar item (ignoring stack limits) or if there is an empty slot, false otherwise
     */
    public boolean canFit(@Nonnull ItemStack item) {
        return this.contains(item) || !this.isCompletelyFilled();
    }

    /**
     * Returns the amount of the specified item that are in the inventory
     *
     * @param item the item to count
     * @return the amount of the specified item that are in the inventory
     */
    public int count(@Nonnull ItemStack item) {
        int hasAmount = 0;
        for (ItemStack loop : this.items) {
            if (loop != null && loop.isSimilar(item)) {
                hasAmount += loop.getAmount();
            }
        }
        return hasAmount;
    }

    public int count(@Nonnull Material material) {
        int hasAmount = 0;
        for (ItemStack loop : this.items) {
            if (loop != null && loop.getType() == material) {
                hasAmount += loop.getAmount();
            }
        }
        return hasAmount;
    }

    /**
     * Returns the index of the first slot that contains an instance of the specified item in the inventory
     *
     * @param item the item to search for
     * @return the index of the first slot containing the item, or -1 if the item is not found
     */
    public int first(@Nonnull ItemStack item) {
        for (int i = 0; i < this.items.length; i++) {
            ItemStack loop = this.items[i];
            if (loop != null && loop.isSimilar(item))
                return i;
        }
        return -1;
    }

    /**
     * Returns the index of the first empty slot in the inventory
     *
     * @return the index of the first empty slot, or -1 if the inventory is completely filled
     */
    public int firstEmpty() {
        for (int i = 0; i < this.items.length; i++) {
            if (this.items[i] == null) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks whether the inventory is completely empty (i.e., contains no items)
     *
     * @return true if the inventory is completely empty, false otherwise
     */
    public boolean isCompletelyEmpty() {
        for (ItemStack item : this.items) {
            if (item != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether the inventory is completely filled (i.e., has no empty slots)
     *
     * @return true if the inventory is completely filled, false otherwise
     */
    public boolean isCompletelyFilled() {
        for (ItemStack item : this.items) {
            if (item == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Removes all items from the inventory
     */
    public void clear() {
        Arrays.fill(this.items, null);
    }

}
