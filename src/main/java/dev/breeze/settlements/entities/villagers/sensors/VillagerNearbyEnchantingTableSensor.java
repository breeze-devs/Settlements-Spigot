package dev.breeze.settlements.entities.villagers.sensors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class VillagerNearbyEnchantingTableSensor extends VillagerNearbyBlockSensor {

    public VillagerNearbyEnchantingTableSensor() {
        super(20, 5, TimeUtil.minutes(5), List.of(Activity.WORK));
    }

    @Override
    protected void tickSensor(@Nonnull ServerLevel world, @Nonnull BaseVillager villager) {
        // Detect nearby enchanting table
        Optional<BlockPos> enchantingTable = BlockPos.findClosestMatch(villager.blockPosition(), this.getRangeHorizontal(), this.getRangeVertical(), (pos) -> {
            // Check block state
            BlockState state = world.getBlockState(pos);
            if (!isEnchantingTable(state)) {
                return false;
            }

            // Check reachability (if the target is blocked off)
            Path path = villager.getNavigation().createPath(pos, 2);
            return path != null;
        });
        VillagerMemoryType.NEAREST_ENCHANTING_TABLE.set(villager.getBrain(), enchantingTable.orElse(null));
    }

    @Override
    @Nonnull
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(VillagerMemoryType.NEAREST_ENCHANTING_TABLE.getMemoryModuleType());
    }

    public static boolean isEnchantingTable(BlockState state) {
        return state.is(Blocks.ENCHANTING_TABLE);
    }

}
