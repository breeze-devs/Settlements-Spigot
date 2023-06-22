package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.bukkit.Material;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CollectArrowsBehavior extends InteractAtTargetBehavior {

    @Nullable
    private List<AbstractArrow> targetArrows;

    public CollectArrowsBehavior() {
        // Preconditions to this behavior
        super(Map.of(
                        VillagerMemoryType.NEARBY_ARROWS.getMemoryModuleType(), MemoryStatus.VALUE_PRESENT
                ), TimeUtil.seconds(30), Math.pow(30, 2),
                TimeUtil.minutes(1), Math.pow(1, 2),
                5, 2,
                TimeUtil.seconds(20), TimeUtil.seconds(4));

        this.targetArrows = null;
    }

    @Override
    protected boolean scan(ServerLevel level, Villager villager) {
        Brain<Villager> brain = villager.getBrain();
        List<AbstractArrow> arrows = VillagerMemoryType.NEARBY_ARROWS.get(brain);
        if (arrows == null || arrows.isEmpty()) {
            return false;
        }

        this.targetArrows = new ArrayList<>(arrows);
        return true;
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.start(level, villager, gameTime);
        this.removeAllInvalidArrows();
    }

    @Override
    protected boolean checkExtraCanStillUseConditions(ServerLevel level, Villager villager, long gameTime) {
        return this.targetArrows != null && !this.targetArrows.isEmpty();
    }

    @Override
    protected void tickExtra(ServerLevel level, Villager villager, long gameTime) {
        if (this.targetArrows == null || this.targetArrows.isEmpty()) {
            return;
        }
        villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.targetArrows.get(0), false));
    }

    @Override
    protected void navigateToTarget(ServerLevel level, Villager villager, long gameTime) {
        if (this.targetArrows == null || this.targetArrows.isEmpty()) {
            return;
        }
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetArrows.get(0), 0.5F, 0));
    }

    @Override
    protected void interactWithTarget(ServerLevel level, Villager villager, long gameTime) {
        if (this.targetArrows == null || this.targetArrows.isEmpty()) {
            return;
        } else if (!this.targetArrows.get(0).isAlive()) {
            this.removeAllInvalidArrows();
            return;
        }

        // Pick up the arrow
        AbstractArrow arrow = this.targetArrows.get(0);
        villager.take(arrow, 1);
        arrow.discard();

        // Update state
        this.targetArrows.remove(0);
        this.removeAllInvalidArrows();
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.stop(level, villager, gameTime);

        // Reset memory (to prevent picking up the same arrows again)
        // - for this behavior to run again, the sensor will need to scan for arrows again
        VillagerMemoryType.NEARBY_ARROWS.set(villager.getBrain(), null);

        // Reset variables
        this.targetArrows = null;
    }

    @Override
    protected boolean hasTarget() {
        return this.targetArrows != null && !this.targetArrows.isEmpty();
    }

    @Override
    protected boolean isTargetReachable(Villager villager) {
        return this.targetArrows != null && !this.targetArrows.isEmpty() && villager.distanceToSqr(this.targetArrows.get(0)) < this.getInteractRangeSquared();
    }

    @Nonnull
    @Override
    public ItemStackBuilder getGuiItemBuilderAbstract() {
        return new ItemStackBuilder(Material.ARROW)
                .setDisplayName("&eCollect nearby arrows behavior")
                .setLore("&7Occasionally picks up nearby arrows on the ground");
    }

    private void removeAllInvalidArrows() {
        if (this.targetArrows == null) {
            return;
        }

        while (this.targetArrows.size() > 0) {
            if (this.targetArrows.get(0) != null && this.targetArrows.get(0).isAlive()) {
                break;
            }
            this.targetArrows.remove(0);
        }
    }

}
