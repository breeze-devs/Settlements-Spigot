package dev.breeze.settlements.entities.villagers.sensors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.utils.DebugUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nonnull;
import java.util.Set;

public abstract class BaseVillagerSensor extends Sensor<Villager> {

    private final int senseCooldown;

    public BaseVillagerSensor(int senseCooldown) {
        super(senseCooldown);

        this.senseCooldown = senseCooldown;
    }

    @Override
    protected final void doTick(@Nonnull ServerLevel world, @Nonnull Villager villager) {
        // Villager type check
        if (!(villager instanceof BaseVillager baseVillager)) {
            return;
        }

        DebugUtil.broadcastEntity("&6Ticking sensor %s!".formatted(this.getClass().getSimpleName()), villager.getStringUUID(),
                baseVillager.getHoverDescription());
        this.tickSensor(world, baseVillager, baseVillager.getBrain());
    }

    protected abstract void tickSensor(@Nonnull ServerLevel world, @Nonnull BaseVillager villager, @Nonnull Brain<Villager> brain);

    @Override
    @Nonnull
    public abstract Set<MemoryModuleType<?>> requires();

    @Nonnull
    public final ItemStackBuilder getGuiItemBuilder() {
        ItemStackBuilder builder = this.getGuiItemBuilderAbstract();

        builder.appendLore("&7---");
        builder.appendLore("&7Related memory: %s".formatted(this.requires().toString()));
        builder.appendLore("&7Interval: %s".formatted(TimeUtil.ticksToReadableTime(this.senseCooldown)));

        return builder;
    }

    @Nonnull
    public abstract ItemStackBuilder getGuiItemBuilderAbstract();

}
