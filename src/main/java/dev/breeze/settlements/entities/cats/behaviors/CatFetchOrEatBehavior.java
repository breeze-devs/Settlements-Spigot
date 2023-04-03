package dev.breeze.settlements.entities.cats.behaviors;

import dev.breeze.settlements.entities.cats.VillagerCat;
import dev.breeze.settlements.entities.cats.memories.CatMemoryType;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.SoundUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Similar to the WolfFetchBehavior, but that the cat might want to eat the fish instead of handing it over
 */
public final class CatFetchOrEatBehavior extends BaseCatBehavior {

    private static final float SPEED_MODIFIER = 1.25F;
    private static final double PICK_UP_RANGE_SQUARED = Math.pow(1.8, 2);
    private static final double DROP_OFF_RANGE_SQUARED = Math.pow(1, 2);
    private static final int MAX_FETCH_COOLDOWN = TimeUtil.seconds(10);
    private static final int MAX_FETCH_DURATION = TimeUtil.seconds(20);
    private static final int MAX_EAT_DURATION = TimeUtil.seconds(2);

    private static final Predicate<ItemEntity> CRITERIA = (itemEntity) -> {
        if (itemEntity == null || !itemEntity.isAlive())
            return false;
        return itemEntity.getItem().is(ItemTags.FISHES);
    };

    /**
     * The chance of the cat eating the fish instead of fetching it
     * - TODO: perhaps change this by cat variant? e.g. orange cats eats more often
     */
    private static final double INITIAL_EAT_PROBABILITY = 0.3;
    /**
     * If the cat's HP falls below this threshold, it will always eat the fish
     */
    private static final double EAT_HP_THRESHOLD = 0.25;

    private int cooldown;
    private double eatProbability = INITIAL_EAT_PROBABILITY;
    private int eatDuration;

    private FetchStatus status;
    @Nullable
    private BaseVillager cachedOwner;
    @Nullable
    private ItemEntity target;
    @Nullable
    private ItemStack eatItem;

    public CatFetchOrEatBehavior() {
        super(Map.of(
                // There should be an item of interest to fetch
                CatMemoryType.NEARBY_ITEMS, MemoryStatus.VALUE_PRESENT
        ), MAX_FETCH_DURATION);

        // No initial cooldown
        this.cooldown = 0;
        this.eatDuration = 0;
        this.status = FetchStatus.STAND_BY;
    }

    @Override
    protected boolean checkExtraStartConditionsRateLimited(@Nonnull ServerLevel level, @Nonnull Cat cat) {
        // Not -1 because this method is rate limited
        this.cooldown -= this.getMaxStartConditionCheckCooldown();
        if (this.cooldown > 0)
            return false;

        // Check if the cat has owner
        BaseVillager owner = this.getOwner(cat);
        if (owner == null)
            return false;

        if (cat.isOrderedToSit())
            return false;

        return this.scan(level, cat);
    }

    private boolean scan(@Nonnull ServerLevel level, @Nonnull Cat cat) {
        // Scan for nearby items
        Optional<List<ItemEntity>> itemMemory = cat.getBrain().getMemory(CatMemoryType.NEARBY_ITEMS);
        if (itemMemory.isEmpty() || itemMemory.get().isEmpty())
            return false;

        if (this.target != null)
            return true;

        // Try to determine a target
        for (ItemEntity nearby : itemMemory.get()) {
            if (nearby != null && CRITERIA.test(nearby)) {
                this.target = nearby;
                return true;
            }
        }

        // No valid target found
        return false;
    }

    @Override
    protected boolean canStillUse(@Nonnull ServerLevel level, @Nonnull Cat cat, long gameTime) {
        // Check time limit
        if (this.cooldown < -MAX_FETCH_DURATION)
            return false;

        // Ignore item status if eating
        if (this.status == FetchStatus.EATING)
            return true;

        // Check item status
        if (this.cachedOwner == null || this.target == null || !this.target.isAlive())
            return false;

        // Check if another animal has picked this item up
        if (this.status == FetchStatus.SEEKING && this.target.isPassenger())
            return false;

        // Check if we've somehow dropped the item on the way back
        return this.status != FetchStatus.RETURNING || this.target.isPassenger();
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Cat cat, long gameTime) {
        super.start(level, cat, gameTime);

        this.status = FetchStatus.SEEKING;
        if (cat instanceof VillagerCat villagerCat) {
            villagerCat.setStopFollowOwner(true);
            villagerCat.setLookLocked(true);
            villagerCat.setMovementLocked(true);

            // If owner is not working or HP is too low, always eat the fish
            if ((villagerCat.getOwner() != null && !villagerCat.getOwner().getBrain().isActive(Activity.WORK))
                    || villagerCat.getHealth() / villagerCat.getMaxHealth() < EAT_HP_THRESHOLD) {
                this.eatProbability = 1;
            }
        }
    }

    @Override
    protected void tick(@Nonnull ServerLevel level, @Nonnull Cat cat, long gameTime) {
        BaseVillager owner = this.getOwner(cat);

        if (this.status == FetchStatus.SEEKING) {
            // Safety null check
            if (owner == null || this.target == null)
                return;

            if (cat.distanceToSqr(this.target) > PICK_UP_RANGE_SQUARED) {
                // We are too far from the item of interest, walk to it
                if (cat.getNavigation().isDone())
                    cat.getNavigation().moveTo(cat.getNavigation().createPath(this.target, 0), SPEED_MODIFIER);
                cat.getLookControl().setLookAt(this.target);
            } else {
                // We are close enough to the item to pick it up
                // Determine if the cat is going to eat it or fetch it
                if (RandomUtil.RANDOM.nextDouble() < this.eatProbability) {
                    // Set eat item
                    this.eatItem = CraftItemStack.asBukkitCopy(this.target.getItem());

                    // Remove item
                    this.target.discard();
                    this.target = null;

                    // Change status to eating
                    this.status = FetchStatus.EATING;
                    this.eatDuration = RandomUtil.RANDOM.nextInt(MAX_EAT_DURATION);
                } else {
                    this.target.startRiding(cat, true);
                    cat.playSound(SoundEvents.ITEM_PICKUP, 0.15F, 1.0F);

                    // Change status to returning
                    this.status = FetchStatus.RETURNING;
                }
            }
        } else if (this.status == FetchStatus.RETURNING) {
            // Safety null check
            if (owner == null || this.target == null)
                return;

            if (cat.distanceToSqr(owner) > DROP_OFF_RANGE_SQUARED) {
                // We are too far from the owner, walk to it
                cat.getNavigation().moveTo(cat.getNavigation().createPath(owner, 0), SPEED_MODIFIER);
                cat.getLookControl().setLookAt(owner);
            } else {
                // Drop off the carried item
                this.target.stopRiding();
                Vec3 motion = this.target.position().vectorTo(owner.position()).normalize().scale(0.2).with(Direction.Axis.Y, 0.2);
                this.target.getBukkitEntity().setVelocity(new Vector(motion.x, motion.y, motion.z)); // use Bukkit for smooth item animation

                // Give owner the item
                owner.receiveItem(this.target);
                cat.playSound(SoundEvents.WOLF_AMBIENT, 0.15F, 1.3F);

                // Stop behavior
                this.target = null;
                this.doStop(level, cat, gameTime);
            }
        } else if (this.status == FetchStatus.EATING) {
            if (--this.eatDuration < 0) {
                if (this.eatItem != null && this.eatItem.getType() == Material.PUFFERFISH) {
                    // If ate puffer fish, add poison
                    cat.addEffect(new MobEffectInstance(MobEffects.POISON, TimeUtil.seconds(2), 1));
                } else {
                    // Otherwise, add regeneration
                    cat.addEffect(new MobEffectInstance(MobEffects.REGENERATION, TimeUtil.seconds(5), 1));
                }
                this.doStop(level, cat, gameTime);
                return;
            }

            // Look at ground
            cat.getLookControl().setLookAt(cat.getX(), cat.getY() - 1, cat.getZ());

            // Display eat effect
            if (this.eatDuration % 5 == 0) {
                Location location = new Location(cat.level.getWorld(), cat.getX(), cat.getY(), cat.getZ());
                if (this.eatItem != null)
                    ParticleUtil.itemBreak(location, this.eatItem, 5, 0.2, 0.2, 0.2, 0.05);
                SoundUtil.playSoundPublic(location, RandomUtil.RANDOM.nextBoolean() ? Sound.ENTITY_CAT_EAT : Sound.ENTITY_GENERIC_EAT, 0.05F,
                        (float) (1 + RandomUtil.RANDOM.nextDouble() / 2 - 0.25));
            }
        }
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Cat cat, long gameTime) {
        super.stop(level, cat, gameTime);

        this.cooldown = MAX_FETCH_COOLDOWN;
        this.eatProbability = INITIAL_EAT_PROBABILITY;
        this.eatDuration = 0;
        this.status = FetchStatus.STAND_BY;

        if (cat instanceof VillagerCat villagerCat) {
            villagerCat.setStopFollowOwner(false);
            villagerCat.setLookLocked(false);
            villagerCat.setMovementLocked(false);
        }

        // Drop off the carried item at current position
        // - delivery not successful
        if (this.target != null) {
            this.target.stopRiding();
            this.target = null;
        }

        this.eatItem = null;
    }

    /**
     * Gets the owner
     */
    @Nullable
    private BaseVillager getOwner(Cat cat) {
        // Try using the cache
        if (this.cachedOwner != null)
            return this.cachedOwner;

        // Try to get the owner
        if (!(cat instanceof VillagerCat villagerCat))
            return null;
        this.cachedOwner = villagerCat.getOwner();
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
        RETURNING,

        /**
         * Eating the item instead of fetching it
         */
        EATING
    }

}

