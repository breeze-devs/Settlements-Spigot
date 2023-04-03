package dev.breeze.settlements.utils.itemstack;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

public class ItemUtil {

    /**
     * Gets the lore line of an itemstack
     *
     * @param item item to be queried
     * @param line line of the lore, negative means counting from behind
     * @return line of the lore
     */
    @Nonnull
    public static String getLoreLine(@Nonnull ItemStack item, int line) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore())
            throw new ArrayIndexOutOfBoundsException("Item has no lore");
        List<String> lore = item.getItemMeta().getLore();

        // Python negative index conversion
        while (line < 0)
            line += lore.size();
        if (line >= lore.size())
            throw new ArrayIndexOutOfBoundsException(String.format("Item's lore have %d lines, accessed line #%d", lore.size(), line));
        return lore.get(line);
    }

    /**
     * Gets the remaining durability of an itemstack
     */
    public static int getDurability(ItemStack item) {
        assert item.getItemMeta() instanceof Damageable : "Item is not damageable!";
        int durability = ((Damageable) item.getItemMeta()).getDamage();
        int max = item.getType().getMaxDurability();
        return max - durability - 1;
    }

    /**
     * Gets the maximum durability of the itemstack
     */
    public static int getMaxDurability(ItemStack item) {
        return item.getType().getMaxDurability();
    }

    /**
     * Serialize the itemstack into a string
     *
     * @return (String) Base 64 representation of the itemstack
     */
    public static String serialize(ItemStack item) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream bukkitStream = new BukkitObjectOutputStream(byteStream);
            bukkitStream.writeObject(item);
            bukkitStream.flush();
            return new String(Base64.getEncoder().encode(byteStream.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * De-serialize the Base64 string into an itemstack
     *
     * @return (ItemStack) itemstack representation of the b64 string
     */
    public static ItemStack deserialize(String base64) {
        try {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
            BukkitObjectInputStream bukkitStream = new BukkitObjectInputStream(byteStream);
            return (ItemStack) bukkitStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

}
