package dev.breeze.settlements.entities.wolves.goals;

import dev.breeze.settlements.entities.wolves.VillagerWolf;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;

public class WolfSitWhenOrderedToGoal extends SitWhenOrderedToGoal {

    private final VillagerWolf wolf;

    public WolfSitWhenOrderedToGoal(VillagerWolf wolf) {
        super(wolf);
        this.wolf = wolf;
    }

    /**
     * Simplify condition as we don't really care much about owners
     * - mostly copied from super
     */
    @Override
    public boolean canUse() {
        if (this.wolf.isInWaterOrBubble() || !this.wolf.onGround)
            return false;
        return this.wolf.isOrderedToSit();
    }

}
