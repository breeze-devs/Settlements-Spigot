package dev.breeze.settlements.entities.cats.sensors;

import dev.breeze.settlements.entities.cats.VillagerCat;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.animal.Cat;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

public class CatOwnerSensor extends Sensor<Cat> {

    /**
     * How often will the cat check its owner status
     */
    private static final int SENSE_COOLDOWN = TimeUtil.minutes(1);

    public CatOwnerSensor() {
        super(SENSE_COOLDOWN);
    }

    @Override
    protected void doTick(@Nonnull ServerLevel world, @Nonnull Cat cat) {
        // Type cast checking
        if (!(cat instanceof VillagerCat villagerCat)) {
            return;
        }

        BaseVillager owner = villagerCat.getOwner();
        if (owner != null && owner.isAlive()) {
            // Notify cat to refresh brain
            villagerCat.onOwnerSensorTick();
        }
    }

    @Override
    @Nonnull
    public Set<MemoryModuleType<?>> requires() {
        // This sensor modifies no memories
        return Collections.emptySet();
    }

}
