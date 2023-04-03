package dev.breeze.settlements.entities.wolves.goals;

import dev.breeze.settlements.entities.wolves.VillagerWolf;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class WolfMovementLockGoal extends Goal {

    private final VillagerWolf wolf;

    public WolfMovementLockGoal(VillagerWolf wolf) {
        this.wolf = wolf;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.wolf.isMovementLocked();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

}
