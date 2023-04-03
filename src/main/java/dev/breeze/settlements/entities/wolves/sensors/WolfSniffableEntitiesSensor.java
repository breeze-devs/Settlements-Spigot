package dev.breeze.settlements.entities.wolves.sensors;

import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.entities.wolves.memories.WolfMemoryType;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Sensor for detecting nearby living entities
 * - used in dog-walking related behaviors
 */
public class WolfSniffableEntitiesSensor extends Sensor<Wolf> {

    private static final double RANGE_HORIZONTAL = 15.0;
    private static final double RANGE_VERTICAL = 3.0;
    private static final double MIN_DISTANCE_SQUARED = Math.pow(4, 2);

    /**
     * How often does the sensor get triggered
     */
    private static final int SENSE_COOLDOWN = TimeUtil.seconds(7);

    public WolfSniffableEntitiesSensor() {
        super(SENSE_COOLDOWN);
    }

    @Override
    protected void doTick(@Nonnull ServerLevel world, @Nonnull Wolf wolf) {
        // Type cast checking
        if (!(wolf instanceof VillagerWolf villagerWolf))
            return;

        // Check activity == PLAY
        Brain<Wolf> brain = villagerWolf.getBrain();
        if (brain.getSchedule().getActivityAt((int) world.getWorld().getTime()) != Activity.PLAY)
            return;

        // Criteria for selecting a sniffable entity
        Predicate<LivingEntity> criteria = (nearby) -> {
            // Check basic requirements
            if (nearby == null || nearby == villagerWolf || nearby == villagerWolf.getOwner() || !nearby.isAlive())
                return false;
            // Check minimum distance
            if (nearby.distanceToSqr(villagerWolf) < MIN_DISTANCE_SQUARED)
                return false;

            // Check if we've sniffed it before
            if (!brain.hasMemoryValue(WolfMemoryType.RECENTLY_SNIFFED_ENTITIES))
                return true;
            Set<LivingEntity> recentlySniffed = brain.getMemory(WolfMemoryType.RECENTLY_SNIFFED_ENTITIES).get();
            return !recentlySniffed.contains(nearby);
        };

        // Create result list
        List<LivingEntity> sniffable = new ArrayList<>(1);

        // Try to get nearby entities fitting the criteria
        // - limit to 1 for efficiency
        AABB nearbyBoundingBox = villagerWolf.getBoundingBox().inflate(RANGE_HORIZONTAL, RANGE_VERTICAL, RANGE_HORIZONTAL);
        world.getEntities(EntityTypeTest.forClass(LivingEntity.class), nearbyBoundingBox, criteria, sniffable, 1);

        // Set or erase memory
        if (sniffable.isEmpty())
            brain.eraseMemory(WolfMemoryType.NEARBY_SNIFFABLE_ENTITIES);
        else
            brain.setMemory(WolfMemoryType.NEARBY_SNIFFABLE_ENTITIES, Optional.of(sniffable));
    }

    @Override
    @Nonnull
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(WolfMemoryType.NEARBY_SNIFFABLE_ENTITIES);
    }

}
