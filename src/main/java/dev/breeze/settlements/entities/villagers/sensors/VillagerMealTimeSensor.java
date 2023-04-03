package dev.breeze.settlements.entities.villagers.sensors;

import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nonnull;
import java.util.Set;

public class VillagerMealTimeSensor extends Sensor<Villager> {

    private static final int SENSE_COOLDOWN = TimeUtil.seconds(10);

    public VillagerMealTimeSensor() {
        super(SENSE_COOLDOWN);
    }

    @Override
    protected void doTick(@Nonnull ServerLevel world, @Nonnull Villager villager) {
        Brain<Villager> brain = villager.getBrain();

        long timeOfDay = world.getLevel().getWorld().getTime();
        boolean isMealTime = false;

        if (timeOfDay > 1800 && timeOfDay < 2200) {
            // Breakfast
            isMealTime = true;
        } else if (timeOfDay > 5800 && timeOfDay < 6200) {
            // Lunch
            isMealTime = true;
        } else if (timeOfDay > 10800 && timeOfDay < 11200) {
            // Dinner
            isMealTime = true;
        }

        // Set or erase memory
        if (isMealTime) {
            brain.setMemory(VillagerMemoryType.IS_MEAL_TIME, true);
        } else {
            brain.eraseMemory(VillagerMemoryType.IS_MEAL_TIME);
        }
    }

    @Override
    @Nonnull
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(VillagerMemoryType.IS_MEAL_TIME);
    }

}
