package dev.breeze.settlements.entities.wolves.sensors;

import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.entities.wolves.memories.WolfMemoryType;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class WolfNearbyItemsSensor extends Sensor<Wolf> {

    /**
     * How far away to scan for items horizontally
     */
    private static final double RANGE_HORIZONTAL = 15.0D;

    /**
     * How far away to scan for items vertically
     */
    private static final double RANGE_VERTICAL = 3.5D;

    /**
     * How often will the wolf scans for nearby items
     */
    private static final int SENSE_COOLDOWN = TimeUtil.seconds(7);

    public WolfNearbyItemsSensor() {
        super(SENSE_COOLDOWN);
    }

    @Override
    protected void doTick(@Nonnull ServerLevel world, @Nonnull Wolf wolf) {
        // Type cast checking
        if (!(wolf instanceof VillagerWolf villagerWolf))
            return;

        // Check activity == WORK
        Brain<Wolf> brain = villagerWolf.getBrain();
        if (brain.getSchedule().getActivityAt((int) world.getWorld().getTime()) != Activity.WORK)
            return;

        // Scan for nearby dropped items
        List<ItemEntity> list = world.getEntitiesOfClass(ItemEntity.class, villagerWolf.getBoundingBox().inflate(RANGE_HORIZONTAL, RANGE_VERTICAL,
                RANGE_HORIZONTAL), (itemEntity -> itemEntity != null && !itemEntity.isPassenger()));

        // Set or erase memory
        if (list.isEmpty())
            brain.eraseMemory(WolfMemoryType.NEARBY_ITEMS);
        else
            brain.setMemory(WolfMemoryType.NEARBY_ITEMS, Optional.of(list));
    }

    @Override
    @Nonnull
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(WolfMemoryType.NEARBY_ITEMS);
    }

}
