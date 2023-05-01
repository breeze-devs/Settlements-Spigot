package dev.breeze.settlements.entities.villagers.sensors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.World;
import org.bukkit.block.data.Levelled;
import org.bukkit.craftbukkit.v1_19_R2.block.data.CraftBlockData;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class VillagerNearbyWaterAreaSensor extends VillagerNearbyBlockSensor {

    public VillagerNearbyWaterAreaSensor() {
        super(20, 4, TimeUtil.minutes(5), List.of(Activity.WORK));
    }

    @Override
    protected void tickSensor(@Nonnull ServerLevel world, @Nonnull BaseVillager villager) {
        // Detect nearby water areas
        Optional<BlockPos> nearestWaterArea = this.findNearestWaterArea(world, villager);
        VillagerMemoryType.NEAREST_WATER_AREA.set(villager.getBrain(), nearestWaterArea.orElse(null));
    }

    @Override
    @Nonnull
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(VillagerMemoryType.NEAREST_WATER_AREA.getMemoryModuleType());
    }

    private Optional<BlockPos> findNearestWaterArea(@Nonnull ServerLevel world, @Nonnull Villager villager) {
        return BlockPos.findClosestMatch(villager.blockPosition(), this.getRangeHorizontal(), this.getRangeVertical(), (pos) -> {
            // Check if block is water source
            BlockState state = world.getBlockState(pos);
            if (!this.isWaterSource(state))
                return false;

            // Check if block is the highest block
            World bukkitWorld = world.getWorld();
            final int x = pos.getX();
            final int y = pos.getY();
            final int z = pos.getZ();
            if (bukkitWorld.getHighestBlockYAt(x, z) != y)
                return false;

            // Check if block of water is at least a 3x3 water source zone
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    // Prevent from checking again
                    if (dx == 0 && dz == 0)
                        continue;

                    // Check if nearby block is the highest block
                    if (bukkitWorld.getHighestBlockYAt(x + dx, z + dz) != y)
                        continue;

                    // Check if nearby block is water source
                    if (!this.isWaterSource(world.getBlockState(mutableBlockPos.set(x + dx, y, z + dz)))) {
                        return false;
                    }
                }
            }

            return true;
        });
    }

    private boolean isWaterSource(BlockState state) {
        if (!state.is(Blocks.WATER))
            return false;
        CraftBlockData blockData = CraftBlockData.fromData(state);
        if (!(blockData instanceof Levelled levelled))
            return false;
        return levelled.getLevel() == 0;
    }

}
