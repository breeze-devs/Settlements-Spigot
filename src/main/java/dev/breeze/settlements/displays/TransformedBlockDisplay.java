package dev.breeze.settlements.displays;

import lombok.Builder;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;

@Getter
public class TransformedBlockDisplay extends TransformedDisplay {

    @Nonnull
    private final BlockData blockData;

    @Builder
    public TransformedBlockDisplay(@Nonnull BlockData blockData, @Nonnull Matrix4f transform, boolean temporary) {
        super(transform, DisplayType.BLOCK, temporary);
        this.blockData = blockData;
    }

    @Override
    @Nonnull
    public BlockDisplay createEntity(@Nonnull Location location) {
        BlockDisplay blockDisplay = (BlockDisplay) location.getWorld().spawnEntity(location, EntityType.BLOCK_DISPLAY);
        blockDisplay.setBlock(this.blockData);
        return blockDisplay;
    }

    @Nonnull
    @Override
    public TransformedDisplay cloneWithoutEntity(boolean temporary) {
        return new TransformedBlockDisplay(this.blockData, this.transform, temporary);
    }

}
