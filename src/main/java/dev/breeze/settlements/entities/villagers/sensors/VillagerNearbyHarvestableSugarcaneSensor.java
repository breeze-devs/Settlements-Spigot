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

public class VillagerNearbyHarvestableSugarcaneSensor extends VillagerNearbyBlockSensor {

    public VillagerNearbyHarvestableSugarcaneSensor() {
        super(20, 5, TimeUtil.minutes(5), List.of(Activity.WORK));
    }

    @Override
    protected void tickSensor(@Nonnull ServerLevel world, @Nonnull BaseVillager villager) {
        // Detect nearby enchanting table
        Optional<BlockPos> enchantingTable = BlockPos.findClosestMatch(villager.blockPosition(), this.getRangeHorizontal(), this.getRangeVertical(), (pos) -> {
            // Check block state
            if (!isHarvestableSugarcane(world, pos)) {
                return false;
            }

            // Check reachability (if the target is blocked off)
            Path path = villager.getNavigation().createPath(pos, 2);
            return path != null;
        });
        VillagerMemoryType.NEAREST_HARVESTABLE_SUGARCANE.set(villager.getBrain(), enchantingTable.orElse(null));
    }

    @Override
    @Nonnull
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(VillagerMemoryType.NEAREST_HARVESTABLE_SUGARCANE.getMemoryModuleType());
    }

    /**
     * Checks if the block state is a harvestable sugarcane block
     * - note that it will only return true when the sugarcane is the second block from the ground up
     * - e.g. if the block is [sand, sugarcane, *sugarcane*, sugarcane] only the "starred" sugarcane will return true
     */
    public static boolean isHarvestableSugarcane(@Nonnull ServerLevel level, @Nonnull BlockPos pos) {
        // Check current block is a sugarcane block
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.SUGAR_CANE)) {
            return false;
        }

        // Check that the block below is also a sugarcane block
        BlockState below = level.getBlockState(pos.below());
        if (!below.is(Blocks.SUGAR_CANE)) {
            return false;
        }


        // Check that 2 blocks below is no longer a sugarcane block
        BlockState below2 = level.getBlockState(pos.below(2));
        return !below2.is(Blocks.SUGAR_CANE);
    }

}
