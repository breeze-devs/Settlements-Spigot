package dev.breeze.settlements.displays;

import lombok.Builder;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;

@Getter
public class TransformedItemDisplay extends TransformedDisplay {

    @Nonnull
    private final ItemStack itemStack;

    @Builder
    public TransformedItemDisplay(@Nonnull ItemStack itemStack, @Nonnull Matrix4f transform, boolean temporary) {
        super(transform, DisplayType.ITEM, temporary);
        this.itemStack = itemStack;
    }

    @Override
    @Nonnull
    public ItemDisplay createEntity(@Nonnull Location location) {
        ItemDisplay blockDisplay = (ItemDisplay) location.getWorld().spawnEntity(location, EntityType.ITEM_DISPLAY);
        blockDisplay.setItemStack(this.itemStack);
        return blockDisplay;
    }

    @Nonnull
    @Override
    public TransformedDisplay cloneWithoutEntity(boolean temporary) {
        return new TransformedItemDisplay(this.itemStack, this.transform, temporary);
    }

}
