package dev.breeze.settlements.utils.itemstack;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class PotionItemStackBuilder extends ItemStackBuilder {

    public PotionItemStackBuilder(PotionType potionType) {
        super(potionType.getPotionMaterial());
    }

    private PotionMeta getPotionMeta() {
        return (PotionMeta) super.itemMeta;
    }

    public PotionItemStackBuilder setBasePotionEffect(PotionData data) {
        this.getPotionMeta().setBasePotionData(data);
        return this;
    }

    /**
     * Adds a custom potion effect to this potion
     *
     * @param potionEffect the potion effect to add
     * @param overwrite    true if any existing effect of the same type should be overwritten
     */
    public PotionItemStackBuilder addPotionEffect(PotionEffect potionEffect, boolean overwrite) {
        this.getPotionMeta().addCustomEffect(potionEffect, overwrite);
        return this;
    }

    public PotionItemStackBuilder removePotionEffect(PotionEffectType potionEffectType) {
        this.getPotionMeta().removeCustomEffect(potionEffectType);
        return this;
    }

    /**
     * Limits the potions created using this builder to be potion materials
     */
    @Getter
    public enum PotionType {
        NORMAL(Material.POTION),
        SPLASH(Material.SPLASH_POTION),
        LINGERING(Material.LINGERING_POTION);

        private final Material potionMaterial;

        PotionType(Material potionMaterial) {
            this.potionMaterial = potionMaterial;
        }
    }

}
