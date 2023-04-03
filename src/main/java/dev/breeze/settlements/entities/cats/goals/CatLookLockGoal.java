package dev.breeze.settlements.entities.cats.goals;

import dev.breeze.settlements.entities.cats.VillagerCat;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class CatLookLockGoal extends Goal {

    private final VillagerCat cat;

    public CatLookLockGoal(VillagerCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.cat.isLookLocked();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

}
