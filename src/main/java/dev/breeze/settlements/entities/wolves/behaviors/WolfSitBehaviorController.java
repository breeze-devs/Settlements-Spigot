package dev.breeze.settlements.entities.wolves.behaviors;

import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.animal.Wolf;

import javax.annotation.Nonnull;

public final class WolfSitBehaviorController {

    private static final int DETECT_COOLDOWN = TimeUtil.seconds(10);

    public static OneShot<Wolf> sit() {
        return new OneShot<>() {
            private int cooldown;

            @Override
            public boolean trigger(@Nonnull ServerLevel world, @Nonnull Wolf wolf, long time) {
                if (--this.cooldown > 0 || wolf.isOrderedToSit())
                    return false;

                this.cooldown = DETECT_COOLDOWN;
                wolf.setOrderedToSit(true);
                return true;
            }
        };
    }

    public static OneShot<Wolf> stand() {
        return new OneShot<>() {
            private int cooldown;

            @Override
            public boolean trigger(@Nonnull ServerLevel world, @Nonnull Wolf wolf, long time) {
                if (--this.cooldown > 0 || !wolf.isOrderedToSit())
                    return false;

                this.cooldown = DETECT_COOLDOWN;
                wolf.setOrderedToSit(false);
                return true;
            }
        };
    }


}
