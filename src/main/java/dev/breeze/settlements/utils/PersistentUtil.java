package dev.breeze.settlements.utils;

import dev.breeze.settlements.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;

public final class PersistentUtil {

    /*
     * ItemStack NBT methods
     */
    public static <T, Z> Z getItemEntry(ItemStack item, PersistentDataType<T, Z> dataType, String key, Z defaultValue) {
        ItemMeta meta = item.getItemMeta();
        assert meta != null : "Item has no item meta!";

        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.getOrDefault(generateKey(key), dataType, defaultValue);
    }

    public static <T, Z> void setItemEntry(ItemStack item, PersistentDataType<T, Z> dataType, String key, Z value) {
        ItemMeta meta = item.getItemMeta();
        assert meta != null : "Item has no item meta!";

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(generateKey(key), dataType, value);

        item.setItemMeta(meta);
    }

    public static void removeItemEntry(ItemStack item, String key) {
        ItemMeta meta = item.getItemMeta();
        assert meta != null : "Item has no item meta!";

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.remove(generateKey(key));

        item.setItemMeta(meta);
    }

    /*
     * Entity NBT methods
     */
    @Nullable
    public static <T, Z> Z getEntityEntry(Entity entity, PersistentDataType<T, Z> dataType, String key) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        return container.get(generateKey(key), dataType);
    }

    public static <T, Z> void setEntityEntry(Entity entity, PersistentDataType<T, Z> dataType, String key, Z value) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        container.set(generateKey(key), dataType, value);
    }

    public static void removeEntityEntry(Entity entity, String key) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        container.remove(generateKey(key));
    }

    /*
     * Block (tile entity) NBT methods
     */
    public static <T, Z> Z getBlockEntry(Block block, PersistentDataType<T, Z> dataType, String key, Z defaultValue) {
        assert block.getState() instanceof TileState : "Block is not a tile-entity!";
        TileState state = ((TileState) block.getState());

        PersistentDataContainer container = state.getPersistentDataContainer();
        return container.getOrDefault(generateKey(key), dataType, defaultValue);
    }

    public static <T, Z> void setBlockEntry(Block block, PersistentDataType<T, Z> dataType, String key, Z value) {
        assert block.getState() instanceof TileState : "Block is not a tile-entity!";
        TileState state = ((TileState) block.getState());

        PersistentDataContainer container = state.getPersistentDataContainer();
        container.set(generateKey(key), dataType, value);

        state.update();
    }

    public static void removeBlockEntry(Block block, String key) {
        assert block.getState() instanceof TileState : "Block is not a tile-entity!";
        TileState state = ((TileState) block.getState());

        PersistentDataContainer container = state.getPersistentDataContainer();
        container.remove(generateKey(key));

        state.update();
    }

    /*
     * Utility methods
     */

    /**
     * Generates a namespaced key using the string key
     *
     * @param key key string to generate from
     * @return Bukkit namespaced key
     */
    private static NamespacedKey generateKey(String key) {
        return new NamespacedKey(Main.getPlugin(), key);
    }

}
