package dev.breeze.settlements.utils.itemstack;

import lombok.Getter;
import org.bukkit.Material;

@Getter
public enum ItemPreset {

    INVENTORY_BLACK_GLASS_PANE(new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" ")),
    INVENTORY_BACK_BUTTON(new ItemStackBuilder(Material.BARRIER)),
    INVENTORY_CONFIRM_BUTTON(new ItemStackBuilder(Material.EMERALD)),
    ;


    private final ItemStackBuilder builder;

    ItemPreset(ItemStackBuilder builder) {
        this.builder = builder;
    }

}
