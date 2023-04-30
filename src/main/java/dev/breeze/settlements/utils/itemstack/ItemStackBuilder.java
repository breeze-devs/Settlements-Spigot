package dev.breeze.settlements.utils.itemstack;

import dev.breeze.settlements.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ItemStackBuilder {

    @Nonnull
    protected final ItemStack itemStack;
    @Nonnull
    protected final ItemMeta itemMeta;

    /**
     * Constructor from material
     */
    public ItemStackBuilder(@Nonnull Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = Objects.requireNonNull(this.itemStack.getItemMeta());
    }

    /**
     * Copy from another Bukkit item stack
     */
    public ItemStackBuilder(@Nonnull ItemStack itemStack) {
        this.itemStack = new ItemStack(itemStack);
        this.itemMeta = Objects.requireNonNull(this.itemStack.getItemMeta());
    }

    /**
     * Copy from another NMS item stack
     */
    public ItemStackBuilder(@Nonnull net.minecraft.world.item.ItemStack itemStack) {
        this(ItemUtil.toBukkit(itemStack));
    }

    @Nonnull
    public ItemStack build() {
        this.itemStack.setItemMeta(this.itemMeta);
        return this.itemStack;
    }

    @Nonnull
    public net.minecraft.world.item.ItemStack buildNms() {
        this.itemStack.setItemMeta(this.itemMeta);
        return CraftItemStack.asNMSCopy(this.itemStack);
    }

    /*
     * Builder methods
     */
    public ItemStackBuilder setMaterial(Material material) {
        this.itemStack.setType(material);
        return this;
    }

    public ItemStackBuilder setAmount(int amount) {
        this.itemStack.setAmount(amount);
        return this;
    }

    public ItemStackBuilder setDisplayName(String name) {
        this.itemMeta.setDisplayName(MessageUtil.translateColorCode(name));
        return this;
    }

    public ItemStackBuilder setLore(String... lore) {
        ArrayList<String> colored = new ArrayList<>();
        for (String line : lore)
            colored.add(MessageUtil.translateColorCode(line));
        this.itemMeta.setLore(colored);
        return this;
    }

    public ItemStackBuilder setLore(List<String> lore) {
        return setLore(lore.toArray(new String[0]));
    }

    public ItemStackBuilder appendLore(String... lore) {
        List<String> colored = this.itemMeta.hasLore() ? this.itemMeta.getLore() : new ArrayList<>();
        for (String line : lore)
            Objects.requireNonNull(colored).add(MessageUtil.translateColorCode(line));
        this.itemMeta.setLore(colored);
        return this;
    }

    public ItemStackBuilder insertLore(int index, String lore) {
        List<String> colored = this.itemMeta.hasLore() ? this.itemMeta.getLore() : new ArrayList<>();
        colored.add(index, MessageUtil.translateColorCode(lore));
        this.itemMeta.setLore(colored);
        return this;
    }

    public ItemStackBuilder addEnchantment(Enchantment enchantment, int level) {
        this.itemMeta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemStackBuilder makeUnbreakable() {
        this.itemMeta.setUnbreakable(true);
        return this;
    }

    public ItemStackBuilder hideFlags(ItemFlag... flags) {
        for (ItemFlag flag : flags)
            this.itemMeta.addItemFlags(flag);
        return this;
    }

}
