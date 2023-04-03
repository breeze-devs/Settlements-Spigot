package dev.breeze.settlements.entities.wolves.behaviors;

import dev.breeze.settlements.Main;
import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.entities.wolves.memories.WolfMemoryType;
import dev.breeze.settlements.entities.wolves.sensors.WolfFenceAreaSensor;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.SoundUtil;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public final class WolfChaseSheepBehavior extends BaseWolfBehavior {

    private static final float CHASE_SPEED = 1.6F;
    private static final float SCARE_SPEED = 1.35F;

    private static final float SHEEP_SPEED = 1.5F;
    private static final float SHEEP_SPEED_DELTA = 0.7F;

    private static final int MAX_BEHAVIOR_DURATION = TimeUtil.minutes(3);
    private static final int MAX_BEHAVIOR_COOLDOWN = TimeUtil.seconds(20);
    private static final int MAX_SCAN_COOLDOWN = TimeUtil.seconds(5);
    private static final int MAX_SHEEP_NAVIGATE_COOLDOWN = TimeUtil.seconds(1);

    // TODO: we can improve this by determining if the sheep is actually outside of the fence area
    private static final double MIN_DISTANCE_FROM_FENCE_CENTER = Math.pow(6, 2);
    private static final double MAX_DISTANCE_FROM_SHEEP_WHEN_CHASING = Math.pow(4, 2);
    private static final double MAX_INTERACTION_RANGE = Math.pow(4, 2);

    // Growling scares an AOE area of sheep back to the pen
    // - randomizes once after wolf's navigation is completed
    private static final double GROWL_CHANCE = 0.3;
    private static final double GROWL_RADIUS = 8;

    private int cooldown;
    private int sheepNavigateCooldown;
    private ChaseState state;

    @Nullable
    private WolfFenceAreaSensor.FenceArea fenceArea;
    @Nonnull
    private final BlockPos[] randomPosInsideFenceArea;

    @Nonnull
    private List<Sheep> nearbySheep;
    @Nonnull
    private final Set<Sheep> restoreCollision;
    @Nullable
    private Sheep target;

    public WolfChaseSheepBehavior() {
        super(Map.of(
                // There should be a nearby fence area
                WolfMemoryType.NEAREST_FENCE_AREA, MemoryStatus.VALUE_PRESENT,
                // There should be nearby sheep
                WolfMemoryType.NEARBY_SHEEP, MemoryStatus.VALUE_PRESENT
        ), MAX_BEHAVIOR_DURATION, MAX_SCAN_COOLDOWN);

        // No initial cooldown
        this.cooldown = 0;
        this.sheepNavigateCooldown = 0;
        this.state = ChaseState.STANDBY;

        this.fenceArea = null;
        this.randomPosInsideFenceArea = new BlockPos[50];

        this.nearbySheep = new ArrayList<>();
        this.restoreCollision = new HashSet<>();
        this.target = null;
    }

    @Override
    protected boolean checkExtraStartConditionsRateLimited(@Nonnull ServerLevel level, @Nonnull Wolf wolf) {
        // Not -1 because this method is rate limited
        this.cooldown -= this.getMaxStartConditionCheckCooldown();
        if (this.cooldown > 0)
            return false;

        // Check if fence gate is valid
        if (!this.isFenceGate(level, wolf.getBrain().getMemory(WolfMemoryType.NEAREST_FENCE_AREA).get())) {
            // Fence area not valid, erase memory and do not start behavior
            wolf.getBrain().eraseMemory(WolfMemoryType.NEAREST_FENCE_AREA);
            return false;
        }

        // Check if the wolf is sitting
        return !wolf.isOrderedToSit();
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Wolf wolf, long gameTime) {
        super.start(level, wolf, gameTime);

        // Precondition of this behavior is that memory exists, so no need to check
        this.fenceArea = wolf.getBrain().getMemory(WolfMemoryType.NEAREST_FENCE_AREA).get();
        for (int i = 0; i < this.randomPosInsideFenceArea.length; i++) {
            BlockPos inside = this.fenceArea.getAnyPositionInside();

            // If randomization of inside area fails, stop behavior
            if (inside == null) {
                this.doStop(level, wolf, gameTime);
                return;
            }
            this.randomPosInsideFenceArea[i] = inside;
        }

        this.nearbySheep = wolf.getBrain().getMemory(WolfMemoryType.NEARBY_SHEEP).get();

        // Attempt to find sheep
        this.target = this.findSheep();
        if (this.target == null) {
            this.doStop(level, wolf, gameTime);
            return;
        }

        // Set movement params
        if (wolf instanceof VillagerWolf villagerWolf) {
            villagerWolf.setStopFollowOwner(true);
            villagerWolf.setLookLocked(true);
            villagerWolf.setMovementLocked(true);
        }

        // Set state
        if (this.isFenceGateOpen(wolf.level)) {
            this.state = ChaseState.CHASING_SHEEP;
        } else {
            this.state = ChaseState.OPENING_GATE;
        }

        // Howl once
        wolf.playSound(SoundEvents.WOLF_HOWL, 1F, 1.0F);
    }

    @Override
    protected boolean canStillUse(@Nonnull ServerLevel level, @Nonnull Wolf wolf, long gameTime) {
        // Check time limit
        if (this.cooldown < -MAX_BEHAVIOR_DURATION)
            return false;
        return wolf.getBrain().isActive(Activity.WORK);
    }

    @Override
    protected void tick(@Nonnull ServerLevel level, @Nonnull Wolf wolf, long gameTime) {
        // Safety checks
        if (this.fenceArea == null || !this.isFenceGate(level, this.fenceArea)) {
            this.doStop(level, wolf, gameTime);
            return;
        }

        // Wolf gate logic
        final BlockPos gatePos = this.fenceArea.getGatePosition();
        final double distanceToGateSqr = wolf.distanceToSqr(gatePos.getX(), gatePos.getY(), gatePos.getZ());

        /*
         * Close the gate
         * - state: CLOSING_GATE
         */
        if (this.state == ChaseState.CLOSING_GATE) {
            // If we are not close enough to the gate, navigate to it
            if (distanceToGateSqr > MAX_INTERACTION_RANGE) {
                if (wolf.getNavigation().isDone()) {
                    wolf.getNavigation().moveTo(wolf.getNavigation().createPath(gatePos, 0), CHASE_SPEED);
                }
                return;
            }

            // We are close enough to the gate, now close it
            this.setFenceGateOpen(wolf.level, false, false);

            if (this.target == null) {
                // If no target, stop behavior
                this.doStop(level, wolf, gameTime);
            } else {
                // Otherwise, set to chasing sheep
                this.state = ChaseState.CHASING_SHEEP;
            }
            return;
        }

        /*
         * State can be OPENING_GATE or CHASING_SHEEP now
         */
        // If no target, try to find another
        if (this.target == null || !this.target.isAlive()) {
            this.target = this.findSheep();

            // If still cannot find another target, stop behavior
            if (this.target == null) {
                // Check if fence gate is still open
                if (this.isFenceGateOpen(level)) {
                    // Fence is open, set state to CLOSING_GATE
                    this.state = ChaseState.CLOSING_GATE;
                } else {
                    // Fence is closed, stop behavior
                    this.doStop(level, wolf, gameTime);
                }
                return;
            }

            // Otherwise (found new target), continue loop
        }

        // Check if the gate is closed while we are nearby chasing sheep
        // - if so, stop chasing and open the gate
        if (this.state != ChaseState.OPENING_GATE && distanceToGateSqr < Math.pow(4, 2) && !this.isFenceGateOpen(level)) {
            this.state = ChaseState.OPENING_GATE;
            return;
        }

        /*
         * The gate is closed, we should open it
         * - state: OPENING_GATE
         */
        if (this.state == ChaseState.OPENING_GATE) {
            // If we are not close enough to the gate, navigate to it
            if (distanceToGateSqr > MAX_INTERACTION_RANGE) {
                if (wolf.getNavigation().isDone()) {
                    wolf.getNavigation().moveTo(wolf.getNavigation().createPath(gatePos, 0), CHASE_SPEED);
                }
                return;
            }

            // We are close enough to the gate, now open it
            this.state = ChaseState.CHASING_SHEEP;
            this.setFenceGateOpen(wolf.level, true, false);
            return;
        }

        /*
         * The gate is open, we can focus on chasing sheep
         * - state: CHASING_SHEEP
         */
        BlockPos center = this.fenceArea.getCenter();
        wolf.getLookControl().setLookAt(this.target);

        // If not close to the sheep, navigate to it
        if (wolf.distanceToSqr(this.target) > MAX_DISTANCE_FROM_SHEEP_WHEN_CHASING) {
            if (wolf.getNavigation().isDone()) {
                wolf.getNavigation().moveTo(wolf.getNavigation().createPath(this.target, 0), CHASE_SPEED);
            }
            return;
        }

        // We are in range, scare the sheep
        if (--this.sheepNavigateCooldown < 0) {
            this.sheepNavigateCooldown = MAX_SHEEP_NAVIGATE_COOLDOWN;
            Path path = this.target.getNavigation().createPath(this.getRandomCachedInsideBlockPos(), 0);
            this.target.getNavigation().moveTo(path, this.getRandomSheepSpeed());

            // If close enough to fence center, set target to null (next tick target will be found)
            if (this.target.distanceToSqr(center.getX(), center.getY(), center.getZ()) < MIN_DISTANCE_FROM_FENCE_CENTER) {
                this.target = null;
                return;
            }
        }

        if (wolf.getNavigation().isDone()) {
            // Chase the speed
            wolf.getNavigation().moveTo(wolf.getNavigation().createPath(this.target, ((int) MAX_DISTANCE_FROM_SHEEP_WHEN_CHASING)), SCARE_SPEED);

            // Growl randomly to scare nearby sheep to the pen
            if (RandomUtil.RANDOM.nextDouble() < GROWL_CHANCE) {
                // Play growl sound
                wolf.playSound(SoundEvents.WOLF_GROWL, 0.15F, 1.0F);

                // Scare nearby AOE of sheep
                List<Sheep> nearbySheep = wolf.level.getEntitiesOfClass(Sheep.class, wolf.getBoundingBox().inflate(GROWL_RADIUS, GROWL_RADIUS, GROWL_RADIUS),
                        (sheep -> sheep != null && sheep.isAlive()));
                for (Sheep sheep : nearbySheep) {
                    // Randomly turns off collision temporarily to avoid crowding
                    if (RandomUtil.RANDOM.nextDouble() < 0.05 && !this.restoreCollision.contains(sheep)) {
                        sheep.collides = false;
                        this.restoreCollision.add(sheep);

                        Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), () -> {
                            sheep.collides = true;
                            this.restoreCollision.remove(sheep);
                        }, TimeUtil.seconds(1));
                    }

                    Path path = sheep.getNavigation().createPath(this.getRandomCachedInsideBlockPos(), 0);
                    sheep.getNavigation().moveTo(path, this.getRandomSheepSpeed());
                }
            }
        }
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Wolf wolf, long gameTime) {
        super.stop(level, wolf, gameTime);

        // Close gate silently
        if (this.fenceArea != null && this.isFenceGate(level, this.fenceArea)) {
            this.setFenceGateOpen(wolf.level, false, true);
        }

        // Restore collision to sheep
        for (Sheep sheep : this.restoreCollision) {
            sheep.collides = true;
        }

        this.cooldown = MAX_BEHAVIOR_COOLDOWN;
        this.fenceArea = null;
        this.nearbySheep.clear();
        this.target = null;

        if (wolf instanceof VillagerWolf villagerWolf) {
            villagerWolf.setStopFollowOwner(false);
            villagerWolf.setLookLocked(false);
            villagerWolf.setMovementLocked(false);
        }
    }

    /**
     * Attempts to find a valid sheep to chase
     */
    private Sheep findSheep() {
        if (this.fenceArea == null)
            return null;

        // Try to determine a target
        BlockPos center = this.fenceArea.getCenter();
        for (Sheep sheep : this.nearbySheep) {
            if (sheep == null || !sheep.isAlive())
                continue;
            // Ignore baby sheep, since they follow their parent
            if (sheep.isBaby())
                continue;
            // If close to the fence center, ignore
            if (sheep.distanceToSqr(center.getX(), center.getY(), center.getZ()) < MIN_DISTANCE_FROM_FENCE_CENTER)
                continue;
            // Set target
            return sheep;
        }

        // No valid sheep found
        return null;
    }

    private boolean isFenceGate(Level level, WolfFenceAreaSensor.FenceArea fenceArea) {
        if (fenceArea == null)
            throw new NullPointerException("Fence area cannot be null!");
        return level.getBlockState(fenceArea.getGatePosition()).is(BlockTags.FENCE_GATES, stateBase -> stateBase.getBlock() instanceof FenceGateBlock);
    }

    private boolean isFenceGateOpen(Level level) {
        if (this.fenceArea == null)
            throw new NullPointerException("Fence area cannot be null!");
        return level.getBlockState(this.fenceArea.getGatePosition()).getValue(FenceGateBlock.OPEN);
    }

    private void setFenceGateOpen(Level level, boolean toOpen, boolean silent) {
        // Ignore if fence gate is already opened/closed
        if (this.fenceArea == null || this.isFenceGateOpen(level) == toOpen)
            return;

        // Change state
        BlockPos gatePos = this.fenceArea.getGatePosition();
        BlockState fenceState = level.getBlockState(gatePos);
        level.setBlock(gatePos, fenceState.setValue(FenceGateBlock.OPEN, toOpen), 10);

        // Play sound if not silent
        if (!silent) {
            Location location = new Location(level.getWorld(), gatePos.getX(), gatePos.getY(), gatePos.getZ());
            Sound sound = toOpen ? Sound.BLOCK_FENCE_GATE_OPEN : Sound.BLOCK_FENCE_GATE_CLOSE;
            SoundUtil.playSoundPublic(location, sound, 1F, RandomUtil.RANDOM.nextFloat() * 0.1F + 0.9F);
        }
    }

    private float getRandomSheepSpeed() {
        return SHEEP_SPEED + RandomUtil.RANDOM.nextFloat(SHEEP_SPEED_DELTA) - (SHEEP_SPEED_DELTA / 2);
    }

    @Nonnull
    private BlockPos getRandomCachedInsideBlockPos() {
        return RandomUtil.choice(this.randomPosInsideFenceArea);
    }

    private enum ChaseState {
        STANDBY,
        OPENING_GATE,
        CHASING_SHEEP,
        CLOSING_GATE
    }

}

