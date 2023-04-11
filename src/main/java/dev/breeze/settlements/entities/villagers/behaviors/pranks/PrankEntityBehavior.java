package dev.breeze.settlements.entities.villagers.behaviors.pranks;

import dev.breeze.settlements.config.files.NitwitPranksConfig;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.behaviors.InteractAtTargetBehavior;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.TimeUtil;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public abstract class PrankEntityBehavior extends InteractAtTargetBehavior {

    private static final float MIN_MOVE_SPEED = NitwitPranksConfig.getInstance().getPrankMoveSpeedMin().getValue();
    private static final float MAX_MOVE_SPEED = NitwitPranksConfig.getInstance().getPrankMoveSpeedMax().getValue();

    @Getter
    @Nullable
    private LivingEntity annoyTarget;

    private float speed;

    public PrankEntityBehavior(int behaviorCooldown, double interactRangeSquared, int maxInteractDuration) {
        // Preconditions to this behavior
        super(Map.of(
                        // There should be living entities nearby
                        MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
                ), TimeUtil.seconds(20), Math.pow(20, 2),
                behaviorCooldown, interactRangeSquared,
                5, 1,
                TimeUtil.seconds(20), maxInteractDuration);

        this.annoyTarget = null;
        this.speed = RandomUtil.RANDOM.nextFloat(MIN_MOVE_SPEED, MAX_MOVE_SPEED);
    }

    @Override
    protected final boolean scan(ServerLevel level, Villager villager) {
        Brain<Villager> brain = villager.getBrain();

        // Don't start behavior if there are no nearby living entities
        if (brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).isEmpty())
            return false;

        // Attempt to find a target to annoy
        if (this.annoyTarget == null) {
            List<LivingEntity> nearbyEntities = villager.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).get();

            for (LivingEntity nearby : nearbyEntities) {
                if (nearby instanceof Villager || nearby instanceof WanderingTrader) {
                    this.annoyTarget = nearby;
                    break;
                }
            }

            if (this.annoyTarget != null)
                villager.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, this.annoyTarget);
        }

        return this.annoyTarget != null;
    }

    @Override
    protected final void start(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.start(level, villager, gameTime);

        // Set move speed to a random speed
        this.speed = RandomUtil.RANDOM.nextFloat(MIN_MOVE_SPEED, MAX_MOVE_SPEED);
    }

    @Override
    protected final boolean checkExtraCanStillUseConditions(ServerLevel level, Villager villager, long gameTime) {
        return this.annoyTarget != null && this.annoyTarget.isAlive();
    }

    @Override
    protected final void tickExtra(ServerLevel level, Villager villager, long gameTime) {
        // Hold the firework item
        if (villager instanceof BaseVillager baseVillager) {
            baseVillager.setHeldItem(this.getItemToHold());
        }

        // Look at target
        if (this.annoyTarget != null) {
            villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.annoyTarget, true));
        }
    }

    @Override
    protected final void navigateToTarget(ServerLevel level, Villager villager, long gameTime) {
        // Safety check
        if (this.annoyTarget == null)
            return;

        // Pathfind to the sheep
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.annoyTarget, this.speed, 3));
    }

    @Override
    protected final void interactWithTarget(ServerLevel level, Villager villager, long gameTime) {
        // Safety check
        if (this.annoyTarget == null)
            return;

        World world = level.getWorld();
        Location selfLoc = new Location(world, villager.getX(), villager.getEyeY(), villager.getZ());
        Location targetLoc = new Location(world, this.annoyTarget.getX(), this.annoyTarget.getEyeY(), this.annoyTarget.getZ());
        this.performAnnoy(level, villager, gameTime, world, selfLoc, targetLoc);

        // Stop behavior
        this.doStop(level, villager, gameTime);
    }

    protected abstract void performAnnoy(ServerLevel level, Villager villager, long gameTime, World world, Location selfLoc, Location targetLoc);

    @Override
    protected final void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.stop(level, villager, gameTime);

        // Reset held item
        if (villager instanceof BaseVillager baseVillager)
            baseVillager.clearHeldItem();

        // Remove interaction memory
        villager.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);

        // Reset variables
        this.annoyTarget = null;
    }

    @Override
    protected final boolean hasTarget() {
        return this.annoyTarget != null;
    }

    @Override
    protected final boolean isTargetReachable(Villager villager) {
        return this.annoyTarget != null && villager.distanceToSqr(this.annoyTarget) < this.getInteractRangeSquared();
    }

    protected abstract ItemStack getItemToHold();

}
