package dev.breeze.settlements.entities.wolves.sensors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.animal.Wolf;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

public class WolfOwnerSensor extends Sensor<Wolf> {

    /**
     * How often will the wolf check its owner status
     */
    private static final int SENSE_COOLDOWN = TimeUtil.minutes(1);

    public WolfOwnerSensor() {
        super(SENSE_COOLDOWN);
    }

    @Override
    protected void doTick(@Nonnull ServerLevel world, @Nonnull Wolf wolf) {
        // Type cast checking
        if (!(wolf instanceof VillagerWolf villagerWolf)) {
            return;
        }

        BaseVillager owner = villagerWolf.getOwner();
        if (owner != null && owner.isAlive()) {
            // Notify cat to refresh brain
            villagerWolf.onOwnerSensorTick();
        }
    }

    @Override
    @Nonnull
    public Set<MemoryModuleType<?>> requires() {
        // This sensor modifies no memories
        return Collections.emptySet();
    }

}
