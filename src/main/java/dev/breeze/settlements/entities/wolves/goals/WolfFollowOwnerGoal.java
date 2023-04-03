package dev.breeze.settlements.entities.wolves.goals;

import dev.breeze.settlements.entities.wolves.VillagerWolf;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;

public class WolfFollowOwnerGoal extends FollowOwnerGoal {

    private final VillagerWolf wolf;

    public WolfFollowOwnerGoal(VillagerWolf wolf, double speed, float minDistance, float maxDistance, boolean leavesAllowed) {
        super(wolf, speed, minDistance, maxDistance, leavesAllowed);
        this.wolf = wolf;
    }

    @Override
    public boolean canUse() {
        // Don't follow owner if fetching
        if (this.wolf.isStopFollowOwner())
            return false;
        // Otherwise, use super's decision
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Don't follow owner if fetching
        if (this.wolf.isStopFollowOwner())
            return false;
        // Otherwise, use super's decision
        return super.canContinueToUse();
    }
}
