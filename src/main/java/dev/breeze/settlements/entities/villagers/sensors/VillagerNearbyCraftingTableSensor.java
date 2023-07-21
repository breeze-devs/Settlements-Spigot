package dev.breeze.settlements.entities.villagers.sensors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import org.bukkit.Material;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class VillagerNearbyCraftingTableSensor extends VillagerNearbyBlockSensor {

    public VillagerNearbyCraftingTableSensor() {
        // TODO: 2min cooldown
        super(20, 5, TimeUtil.seconds(15), List.of(Activity.WORK));
    }

    @Override
    protected void tickBlockSensor(@Nonnull ServerLevel world, @Nonnull BaseVillager villager, @Nonnull Brain<Villager> brain) {
        Optional<BlockPos> enchantingTable = BlockPos.findClosestMatch(villager.blockPosition(), this.getRangeHorizontal(), this.getRangeVertical(), (pos) -> {
            // Check block state
            BlockState state = world.getBlockState(pos);
            if (!state.is(Blocks.CRAFTING_TABLE)) {
                return false;
            }

            // Check reachability (if the target is blocked off)
            Path path = villager.getNavigation().createPath(pos, 2);
            return path != null;
        });
        VillagerMemoryType.NEAREST_CRAFTING_TABLE.set(brain, enchantingTable.orElse(null));
    }

    @Override
    @Nonnull
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(VillagerMemoryType.NEAREST_CRAFTING_TABLE.getMemoryModuleType());
    }

    @Nonnull
    @Override
    public ItemStackBuilder getBaseGuiItemBuilder() {
        return new ItemStackBuilder(Material.CRAFTING_TABLE)
                .setDisplayName("&eNearest crafting table sensor")
                .setLore("&fInfrequently scans for the closest crafting table");
    }

}
