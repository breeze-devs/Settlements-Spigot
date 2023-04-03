package dev.breeze.settlements.entities.cats.goals;

import dev.breeze.settlements.entities.cats.VillagerCat;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;

public class CatFollowOwnerGoal extends FollowOwnerGoal {

    private final VillagerCat cat;

    public CatFollowOwnerGoal(VillagerCat cat, double speed, float minDistance, float maxDistance, boolean leavesAllowed) {
        super(cat, speed, minDistance, maxDistance, leavesAllowed);
        this.cat = cat;
    }

    @Override
    public boolean canUse() {
        // Don't follow owner if fetching
        if (this.cat.isStopFollowOwner())
            return false;
        // Otherwise, use super's decision
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Don't follow owner if fetching
        if (this.cat.isStopFollowOwner())
            return false;
        // Otherwise, use super's decision
        return super.canContinueToUse();
    }
}
