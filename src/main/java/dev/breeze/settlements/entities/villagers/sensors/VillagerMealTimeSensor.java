package dev.breeze.settlements.entities.villagers.sensors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import org.bukkit.Material;

import javax.annotation.Nonnull;
import java.util.Set;

public class VillagerMealTimeSensor extends BaseVillagerSensor {

    private static final int SENSE_COOLDOWN = TimeUtil.seconds(30);

    // Meal times
    public static final int BREAKFAST_START = 1800;
    public static final int BREAKFAST_END = 2200;
    public static final int LUNCH_START = 5800;
    public static final int LUNCH_END = 6200;
    public static final int DINNER_START = 10800;
    public static final int DINNER_END = 11200;

    public VillagerMealTimeSensor() {
        super(SENSE_COOLDOWN);
    }

    @Override
    protected void tickSensor(@Nonnull ServerLevel world, @Nonnull BaseVillager villager, @Nonnull Brain<Villager> brain) {
        long timeOfDay = world.getLevel().getWorld().getTime();
        boolean isMealTime = false;

        if (timeOfDay > BREAKFAST_START && timeOfDay < BREAKFAST_END) {
            // Breakfast
            isMealTime = true;
        } else if (timeOfDay > LUNCH_START && timeOfDay < LUNCH_END) {
            // Lunch
            isMealTime = true;
        } else if (timeOfDay > DINNER_START && timeOfDay < DINNER_END) {
            // Dinner
            isMealTime = true;
        }

        // Set or erase memory
        VillagerMemoryType.IS_MEAL_TIME.set(brain, isMealTime ? true : null);
    }

    @Override
    @Nonnull
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(VillagerMemoryType.IS_MEAL_TIME.getMemoryModuleType());
    }

    @Nonnull
    @Override
    public ItemStackBuilder getGuiItemBuilderAbstract() {
        return new ItemStackBuilder(Material.BREAD)
                .setDisplayName("&eMeal time sensor")
                .setLore(
                        "&fChecks if it's time to eat yet",
                        "&fMeal times:",
                        "&7- Breakfast: %d - %d ticks".formatted(BREAKFAST_START, BREAKFAST_END),
                        "&7- Lunch: %d - %d ticks".formatted(LUNCH_START, LUNCH_END),
                        "&7- Dinner: %d - %d ticks".formatted(DINNER_START, DINNER_END)
                );
    }

}
