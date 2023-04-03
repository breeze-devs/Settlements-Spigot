package dev.breeze.settlements.entities.cats.behaviors;

import dev.breeze.settlements.entities.cats.VillagerCat;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.animal.Cat;

import javax.annotation.Nonnull;

public final class CatFollowOwnerBehaviorController {

    private static final int DETECT_COOLDOWN = TimeUtil.seconds(10);

    public static OneShot<Cat> startFollowOwner() {
        return new OneShot<>() {
            private int cooldown;

            @Override
            public boolean trigger(@Nonnull ServerLevel world, @Nonnull Cat cat, long time) {
                if (--this.cooldown > 0 || cat.isOrderedToSit())
                    return false;

                this.cooldown = DETECT_COOLDOWN;
                if (cat instanceof VillagerCat villagerCat)
                    villagerCat.setStopFollowOwner(false);
                return true;
            }
        };
    }

    public static OneShot<Cat> stopFollowOwner() {
        return new OneShot<>() {
            private int cooldown;

            @Override
            public boolean trigger(@Nonnull ServerLevel world, @Nonnull Cat cat, long time) {
                if (--this.cooldown > 0 || !cat.isOrderedToSit())
                    return false;

                this.cooldown = DETECT_COOLDOWN;
                if (cat instanceof VillagerCat villagerCat)
                    villagerCat.setStopFollowOwner(true);
                return true;
            }
        };
    }


}
