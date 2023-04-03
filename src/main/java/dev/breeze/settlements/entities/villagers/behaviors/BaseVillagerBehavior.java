package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.utils.MessageUtil;
import dev.breeze.settlements.utils.TimeUtil;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nonnull;
import java.util.Map;

public abstract class BaseVillagerBehavior extends Behavior<Villager> {

    private static final int DEFAULT_START_CONDITION_CHECK_COOLDOWN = TimeUtil.seconds(5);

    @Getter
    private final int maxStartConditionCheckCooldown;

    private int startConditionCheckCooldown;

    public BaseVillagerBehavior(Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState, int runTime) {
        this(requiredMemoryState, runTime, DEFAULT_START_CONDITION_CHECK_COOLDOWN);
    }

    public BaseVillagerBehavior(Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState, int runTime, int maxStartConditionCheckCooldown) {
        super(requiredMemoryState, runTime);

        this.maxStartConditionCheckCooldown = maxStartConditionCheckCooldown;
        this.startConditionCheckCooldown = this.maxStartConditionCheckCooldown;
    }

    @Override
    protected final boolean checkExtraStartConditions(@Nonnull ServerLevel world, @Nonnull Villager villager) {
        if (--this.startConditionCheckCooldown > 0)
            return false;

        this.startConditionCheckCooldown = this.maxStartConditionCheckCooldown;
        return this.checkExtraStartConditionsRateLimited(world, villager);
    }

    protected abstract boolean checkExtraStartConditionsRateLimited(@Nonnull ServerLevel world, @Nonnull Villager villager);

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        MessageUtil.debug("&a[Debug] Villager behavior " + this.getClass().getSimpleName() + " has started");
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        MessageUtil.debug("&c[Debug] Villager behavior " + this.getClass().getSimpleName() + " has stopped");
    }

}
