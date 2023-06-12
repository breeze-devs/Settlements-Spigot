package dev.breeze.settlements.entities.villagers.memories;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.utils.particle.ParticlePreset;
import lombok.Builder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class VillagerBlockPosMemory extends VillagerMemory<BlockPos> {

    @Builder(builderMethodName = "blockPosMemoryBuilder")
    public VillagerBlockPosMemory(@Nonnull String identifier, @Nonnull String displayName, @Nonnull List<String> description,
                                  @Nullable Material itemMaterial) {
        super(identifier, new BlockPosParser(), new BlockPosSerializer(), new BlockPosClickEventHandler(),
                displayName, description, itemMaterial);
    }

    private static class BlockPosParser implements MemoryParser<BlockPos> {

        @Nonnull
        @Override
        public List<String> getDisplayString(@Nonnull BlockPos memory) {
            return Collections.singletonList("&7Location: x: %d, y: %d, z: %d".formatted(memory.getX(), memory.getY(), memory.getZ()));
        }

    }

    private static class BlockPosSerializer implements MemorySerializer<BlockPos> {

        @Nonnull
        @Override
        public LongTag toTag(@Nonnull BlockPos memory) {
            return LongTag.valueOf(memory.asLong());
        }

        @Nonnull
        @Override
        public BlockPos fromTag(@Nonnull CompoundTag memoriesTag, @Nonnull String key) {
            return BlockPos.of(memoriesTag.getLong(key));
        }

    }

    private static class BlockPosClickEventHandler implements MemoryClickHandler<BlockPos> {

        @Override
        public void onClick(@Nonnull Player player, @Nonnull BaseVillager baseVillager, @Nullable Object memory) {
            if (memory == null) {
                return;
            }

            player.closeInventory();
            BlockPos pos = (BlockPos) memory;
            Location target = new Location(player.getWorld(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

            // Highlight location
            ParticlePreset.displayLinePrivate(player, player.getEyeLocation(), target, 15, Particle.END_ROD, 2, 0, 0, 0, 0);
            ParticlePreset.displayCirclePrivate(player, target, 1, 20, Particle.VILLAGER_HAPPY, 1, 0, 0, 0, 0);
        }

    }

}
