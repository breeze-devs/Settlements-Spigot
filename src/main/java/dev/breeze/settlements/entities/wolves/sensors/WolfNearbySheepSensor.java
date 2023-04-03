package dev.breeze.settlements.entities.wolves.sensors;

import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.entities.wolves.memories.WolfMemoryType;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.animal.Sheep;
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
 * Sensor for detecting nearby sheep
 * - used in chasing sheep behavior
 */
public class WolfNearbySheepSensor extends Sensor<Wolf> {

    private static final double RANGE_HORIZONTAL = 25.0;
    private static final double RANGE_VERTICAL = 4.0;
    private static final double MIN_DISTANCE_SQUARED = Math.pow(3, 2);

    /**
     * How often does the sensor get triggered
     */
    private static final int SENSE_COOLDOWN = TimeUtil.seconds(30);

    public WolfNearbySheepSensor() {
        super(SENSE_COOLDOWN);
    }

    @Override
    @Nonnull
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(WolfMemoryType.NEARBY_SHEEP);
    }

    @Override
    protected void doTick(@Nonnull ServerLevel world, @Nonnull Wolf wolf) {
        // Type cast checking
        if (!(wolf instanceof VillagerWolf villagerWolf))
            return;

        // Check activity == WORK
        Brain<Wolf> brain = villagerWolf.getBrain();
        if (!brain.isActive(Activity.WORK))
            return;

        // Criteria for selecting a sniffable entity
        Predicate<LivingEntity> criteria = (nearby) -> {
            // Check basic requirements
            if (nearby == null || !nearby.isAlive())
                return false;
            // Check minimum distance
            return !(nearby.distanceToSqr(villagerWolf) < MIN_DISTANCE_SQUARED);
        };

        // Create result list
        List<Sheep> sniffable = new ArrayList<>();

        // Try to get nearby entities fitting the criteria
        AABB nearbyBoundingBox = villagerWolf.getBoundingBox().inflate(RANGE_HORIZONTAL, RANGE_VERTICAL, RANGE_HORIZONTAL);
        world.getEntities(EntityTypeTest.forClass(Sheep.class), nearbyBoundingBox, criteria, sniffable);

        // Set or erase memory
        if (sniffable.isEmpty())
            brain.eraseMemory(WolfMemoryType.NEARBY_SHEEP);
        else
            brain.setMemory(WolfMemoryType.NEARBY_SHEEP, Optional.of(sniffable));
    }

}
