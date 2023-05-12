package dev.breeze.settlements.entities.villagers.sensors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

@Getter
public abstract class VillagerNearbyBlockSensor extends BaseVillagerSensor {

    /**
     * How far away to scan horizontally
     */
    private final int rangeHorizontal;
    /**
     * How far away to scan vertically
     */
    private final int rangeVertical;

    /**
     * How often will the villager scan for nearby water areas
     * - should be infrequent as terrain doesn't change that much
     */
    private final int senseCooldown;

    /**
     * The activities that this sensor is allowed to run in
     * - if empty, then runs in all activities
     */
    @Nonnull
    private final List<Activity> allowedActivities;

    public VillagerNearbyBlockSensor(int rangeHorizontal, int rangeVertical, int senseCooldown, @Nonnull List<Activity> allowedActivities) {
        super(senseCooldown);

        this.rangeHorizontal = rangeHorizontal;
        this.rangeVertical = rangeVertical;
        this.senseCooldown = senseCooldown;
        this.allowedActivities = allowedActivities;
    }

    @Override
    protected void tickSensor(@Nonnull ServerLevel world, @Nonnull BaseVillager villager, @Nonnull Brain<Villager> brain) {
        // Check if current activity is allowed
        // - if allowed list is empty, we default to allowed
        if (!this.allowedActivities.isEmpty() && !this.allowedActivities.contains(brain.getSchedule().getActivityAt((int) world.getWorld().getTime()))) {
            return;
        }

        this.tickBlockSensor(world, villager, brain);
    }

    protected abstract void tickBlockSensor(@Nonnull ServerLevel world, @Nonnull BaseVillager villager, @Nonnull Brain<Villager> brain);

    @Override
    @Nonnull
    public abstract Set<MemoryModuleType<?>> requires();

    @Nonnull
    public final ItemStackBuilder getGuiItemBuilderAbstract() {
        ItemStackBuilder builder = this.getBaseGuiItemBuilder();
        builder.appendLore("&fHorizontal range: %d blocks".formatted(this.rangeHorizontal));
        builder.appendLore("&fVertical range: %d blocks".formatted(this.rangeVertical));
        return builder;
    }

    @Nonnull
    public abstract ItemStackBuilder getBaseGuiItemBuilder();

}
