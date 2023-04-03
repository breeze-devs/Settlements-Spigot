package dev.breeze.settlements.entities.wolves.behaviors;

import dev.breeze.settlements.utils.MessageUtil;
import dev.breeze.settlements.utils.TimeUtil;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.Wolf;

import javax.annotation.Nonnull;
import java.util.Map;

public abstract class BaseWolfBehavior extends Behavior<Wolf> {

    private static final int DEFAULT_START_CONDITION_CHECK_COOLDOWN = TimeUtil.seconds(5);

    @Getter
    private final int maxStartConditionCheckCooldown;

    private int startConditionCheckCooldown;

    public BaseWolfBehavior(Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState, int runTime) {
        this(requiredMemoryState, runTime, DEFAULT_START_CONDITION_CHECK_COOLDOWN);
    }

    public BaseWolfBehavior(Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState, int runTime, int maxStartConditionCheckCooldown) {
        super(requiredMemoryState, runTime);

        this.maxStartConditionCheckCooldown = maxStartConditionCheckCooldown;
        this.startConditionCheckCooldown = 0;
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Wolf wolf, long gameTime) {
        MessageUtil.broadcast("&a[Debug] Wolf behavior " + this.getClass().getSimpleName() + " has started");
    }

    @Override
    protected final boolean checkExtraStartConditions(@Nonnull ServerLevel world, @Nonnull Wolf entity) {
        if (--this.startConditionCheckCooldown > 0)
            return false;

        this.startConditionCheckCooldown = this.maxStartConditionCheckCooldown;
        return this.checkExtraStartConditionsRateLimited(world, entity);
    }

    protected abstract boolean checkExtraStartConditionsRateLimited(ServerLevel world, Wolf entity);

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Wolf wolf, long gameTime) {
        MessageUtil.broadcast("&c[Debug] Wolf behavior " + this.getClass().getSimpleName() + " has stopped");
    }

}
