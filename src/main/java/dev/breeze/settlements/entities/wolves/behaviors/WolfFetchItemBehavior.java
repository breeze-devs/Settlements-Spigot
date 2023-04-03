package dev.breeze.settlements.entities.wolves.behaviors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.entities.wolves.memories.WolfMemoryType;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public final class WolfFetchItemBehavior extends BaseWolfBehavior {

    /**
     * What speed will the wolf move at when fetching
     */
    private static final float SPEED_MODIFIER = 1.15F;

    /**
     * The pick-up range of the wolf
     * - in blocks (squared)
     */
    private static final double PICK_UP_RANGE_SQUARED = Math.pow(1.8, 2);

    /**
     * The drop-off range of the wolf
     * - in blocks (squared)
     */
    private static final double DROP_OFF_RANGE_SQUARED = Math.pow(1, 2);

    /**
     * How long will the wolf "rest" after fetching something
     */
    private static final int MAX_FETCH_COOLDOWN = TimeUtil.seconds(10);

    /**
     * How long will the wolf wait before scanning again
     */
    private static final int MAX_SCAN_COOLDOWN = TimeUtil.seconds(10);

    /**
     * The maximum duration that the wolf is allowed to fetch something
     */
    private static final int MAX_FETCH_DURATION = TimeUtil.seconds(20);

    @Nonnull
    private final Predicate<ItemEntity> criteria;
    private int cooldown;

    private FetchStatus status;
    @Nullable
    private BaseVillager cachedOwner;
    @Nullable
    private ItemEntity target;

    public WolfFetchItemBehavior(@Nonnull Predicate<ItemEntity> criteria) {
        super(Map.of(
                // There should be an item of interest to fetch
                WolfMemoryType.NEARBY_ITEMS, MemoryStatus.VALUE_PRESENT
        ), MAX_FETCH_DURATION);

        this.criteria = criteria;

        // No initial cooldown
        this.cooldown = 0;
        this.status = FetchStatus.STAND_BY;
    }

    @Override
    protected boolean checkExtraStartConditionsRateLimited(@Nonnull ServerLevel level, @Nonnull Wolf wolf) {
        // Not -1 because this method is rate limited
        this.cooldown -= this.getMaxStartConditionCheckCooldown();
        if (this.cooldown > 0)
            return false;

        // Check if the wolf has owner
        BaseVillager owner = this.getOwner(wolf);
        if (owner == null)
            return false;

        // Check if the wolf is sitting
        if (wolf.isOrderedToSit())
            return false;

        return this.scan(level, wolf);
    }

    private boolean scan(@Nonnull ServerLevel level, @Nonnull Wolf wolf) {
        // Scan for nearby items
        Optional<List<ItemEntity>> itemMemory = wolf.getBrain().getMemory(WolfMemoryType.NEARBY_ITEMS);
        if (itemMemory.isEmpty() || itemMemory.get().isEmpty())
            return false;

        if (this.target != null)
            return true;

        // Try to determine a target
        for (ItemEntity nearby : itemMemory.get()) {
            if (nearby != null && this.criteria.test(nearby)) {
                this.target = nearby;
                return true;
            }
        }

        // No valid target found
        return false;
    }

    @Override
    protected boolean canStillUse(@Nonnull ServerLevel level, @Nonnull Wolf wolf, long gameTime) {
        // Check time limit
        if (this.cooldown < -MAX_FETCH_DURATION)
            return false;

        if (this.cachedOwner == null || this.target == null || !this.target.isAlive())
            return false;

        // Check if another wolf has picked this item up
        if (this.status == FetchStatus.SEEKING && this.target.isPassenger())
            return false;

        // Check if we've somehow dropped the item on the way back
        return this.status != FetchStatus.RETURNING || this.target.isPassenger();
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Wolf wolf, long gameTime) {
        super.start(level, wolf, gameTime);

        this.status = FetchStatus.SEEKING;
        if (wolf instanceof VillagerWolf villagerWolf) {
            villagerWolf.setStopFollowOwner(true);
            villagerWolf.setLookLocked(true);
            villagerWolf.setMovementLocked(true);
        }
    }

    @Override
    protected void tick(@Nonnull ServerLevel level, @Nonnull Wolf wolf, long gameTime) {
        BaseVillager owner = this.getOwner(wolf);

        // Safety null check
        if (owner == null || this.target == null)
            return;

        if (this.status == FetchStatus.SEEKING) {
            if (wolf.distanceToSqr(this.target) > PICK_UP_RANGE_SQUARED) {
                // We are too far from the item of interest, walk to it
                wolf.getNavigation().moveTo(wolf.getNavigation().createPath(this.target, 0), SPEED_MODIFIER);
                wolf.getLookControl().setLookAt(this.target);
            } else {
                // We are close enough to the item to pick it up
                this.target.startRiding(wolf, true);
                wolf.playSound(SoundEvents.ITEM_PICKUP, 0.15F, 1.0F);

                // Change status to returning
                this.status = FetchStatus.RETURNING;
            }
        } else if (this.status == FetchStatus.RETURNING) {
            if (wolf.distanceToSqr(owner) > DROP_OFF_RANGE_SQUARED) {
                // We are too far from the owner, walk to it
                wolf.getNavigation().moveTo(wolf.getNavigation().createPath(owner, 0), SPEED_MODIFIER);
                wolf.getLookControl().setLookAt(owner);
            } else {
                // Drop off the carried item
                this.target.stopRiding();
                Vec3 motion = this.target.position().vectorTo(owner.position()).normalize().scale(0.2).with(Direction.Axis.Y, 0.2);
                this.target.getBukkitEntity().setVelocity(new Vector(motion.x, motion.y, motion.z)); // use Bukkit for smooth item animation

                // Give owner the item
                owner.receiveItem(this.target);
                wolf.playSound(SoundEvents.WOLF_AMBIENT, 0.15F, 1.3F);

                // Stop behavior
                this.target = null;
                this.doStop(level, wolf, gameTime);
            }
        }
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Wolf wolf, long gameTime) {
        super.stop(level, wolf, gameTime);

        this.cooldown = MAX_FETCH_COOLDOWN;
        this.status = FetchStatus.STAND_BY;

        if (wolf instanceof VillagerWolf villagerWolf) {
            villagerWolf.setStopFollowOwner(false);
            villagerWolf.setLookLocked(false);
            villagerWolf.setMovementLocked(false);
        }

        // Drop off the carried item at current position
        // - delivery not successful
        if (this.target != null) {
            this.target.stopRiding();
            this.target = null;
        }
    }

    /**
     * Gets the owner
     */
    @Nullable
    private BaseVillager getOwner(Wolf wolf) {
        // Try using the cache
        if (this.cachedOwner != null)
            return this.cachedOwner;

        // Try to get the owner
        if (!(wolf instanceof VillagerWolf villagerWolf))
            return null;
        this.cachedOwner = villagerWolf.getOwner();
        return this.cachedOwner;
    }

    private enum FetchStatus {
        /**
         * Not actively fetching
         */
        STAND_BY,

        /**
         * Actively pursuing an item
         */
        SEEKING,

        /**
         * Returning to owner after picking up the item
         */
        RETURNING
    }

}

