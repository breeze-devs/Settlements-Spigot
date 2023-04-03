package dev.breeze.settlements.entities.cats.goals;

import dev.breeze.settlements.entities.cats.VillagerCat;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class CatMovementLockGoal extends Goal {

    private final VillagerCat cat;

    public CatMovementLockGoal(VillagerCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.cat.isMovementLocked();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

}
