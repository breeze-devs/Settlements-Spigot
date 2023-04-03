package dev.breeze.settlements.entities.cats.behaviors;

import dev.breeze.settlements.entities.cats.VillagerCat;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;
import java.util.Map;

public final class CatSleepBehavior extends BaseCatBehavior {

    private static final int MAX_LIE_CHECK_COOLDOWN = TimeUtil.seconds(30);

    private int cooldown;

    public CatSleepBehavior() {
        // Runtime is infinite until we canStillUse returns false
        super(Map.of(), 1);
        this.cooldown = 0;
    }

    @Override
    protected boolean checkExtraStartConditionsRateLimited(@Nonnull ServerLevel level, @Nonnull Cat cat) {
        // Check if we are sitting or lying down
        return !cat.isOrderedToSit() && !cat.isLying();
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Cat cat, long gameTime) {
        super.start(level, cat, gameTime);

        cat.setLying(true);
        if (cat instanceof VillagerCat villagerCat) {
            villagerCat.setLookLocked(true);
            villagerCat.setMovementLocked(true);
        }
    }

    @Override
    protected boolean canStillUse(@Nonnull ServerLevel level, @Nonnull Cat cat, long gameTime) {
        return cat.getBrain().isActive(Activity.REST);
    }

    @Override
    protected void tick(@Nonnull ServerLevel level, @Nonnull Cat cat, long time) {
        if (--this.cooldown > 0)
            return;
        cat.setLying(true);
        this.cooldown = MAX_LIE_CHECK_COOLDOWN;
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Cat cat, long gameTime) {
        super.stop(level, cat, gameTime);

        cat.setLying(false);
        if (cat instanceof VillagerCat villagerCat) {
            villagerCat.setLookLocked(false);
            villagerCat.setMovementLocked(false);
        }
    }

    /**
     * Override timed out method so that this behavior will not time out
     * - will only stop when canStillUse() returns false
     */
    @Override
    protected boolean timedOut(long time) {
        return false;
    }

}

