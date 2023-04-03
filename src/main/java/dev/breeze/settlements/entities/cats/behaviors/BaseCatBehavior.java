package dev.breeze.settlements.entities.cats.behaviors;

import dev.breeze.settlements.utils.MessageUtil;
import dev.breeze.settlements.utils.TimeUtil;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.Cat;

import javax.annotation.Nonnull;
import java.util.Map;

public abstract class BaseCatBehavior extends Behavior<Cat> {

    private static final int DEFAULT_START_CONDITION_CHECK_COOLDOWN = TimeUtil.seconds(5);

    @Getter
    private final int maxStartConditionCheckCooldown;

    private int startConditionCheckCooldown;

    public BaseCatBehavior(Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState, int runTime) {
        this(requiredMemoryState, runTime, DEFAULT_START_CONDITION_CHECK_COOLDOWN);
    }

    public BaseCatBehavior(Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState, int runTime, int maxStartConditionCheckCooldown) {
        super(requiredMemoryState, runTime);

        this.maxStartConditionCheckCooldown = maxStartConditionCheckCooldown;
        this.startConditionCheckCooldown = 0;
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Cat cat, long gameTime) {
        MessageUtil.debug("&a[Debug] Cat behavior " + this.getClass().getSimpleName() + " has started");
    }

    @Override
    protected final boolean checkExtraStartConditions(@Nonnull ServerLevel world, @Nonnull Cat cat) {
        if (--this.startConditionCheckCooldown > 0)
            return false;

        this.startConditionCheckCooldown = this.maxStartConditionCheckCooldown;
        return this.checkExtraStartConditionsRateLimited(world, cat);
    }

    protected abstract boolean checkExtraStartConditionsRateLimited(@Nonnull ServerLevel world, @Nonnull Cat cat);

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Cat cat, long gameTime) {
        MessageUtil.debug("&c[Debug] Cat behavior " + this.getClass().getSimpleName() + " has stopped");
    }

}
