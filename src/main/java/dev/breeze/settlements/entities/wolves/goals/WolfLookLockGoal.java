package dev.breeze.settlements.entities.wolves.goals;

import dev.breeze.settlements.entities.wolves.VillagerWolf;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class WolfLookLockGoal extends Goal {

    private final VillagerWolf wolf;

    public WolfLookLockGoal(VillagerWolf wolf) {
        this.wolf = wolf;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.wolf.isLookLocked();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

}
