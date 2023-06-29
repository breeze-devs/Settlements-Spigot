package dev.breeze.settlements.entities.cats.goals;

import dev.breeze.settlements.entities.cats.VillagerCat;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;

public class CatSitWhenOrderedToGoal extends SitWhenOrderedToGoal {

    private final VillagerCat cat;

    public CatSitWhenOrderedToGoal(VillagerCat cat) {
        super(cat);
        this.cat = cat;
    }

    /**
     * Simplify condition as we don't really care much about owners
     * - mostly copied from super
     */
    @Override
    public boolean canUse() {
        if (this.cat.isInWaterOrBubble() || !this.cat.onGround())
            return false;
        return this.cat.isOrderedToSit();
    }

}
