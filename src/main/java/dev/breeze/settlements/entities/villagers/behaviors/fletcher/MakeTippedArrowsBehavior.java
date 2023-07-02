package dev.breeze.settlements.entities.villagers.behaviors.fletcher;

import dev.breeze.settlements.entities.EntityModuleController;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.behaviors.InteractAtTargetBehavior;
import dev.breeze.settlements.utils.LocationUtil;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.SoundUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.itemstack.PotionItemStackBuilder;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

public final class MakeTippedArrowsBehavior extends InteractAtTargetBehavior {

    private static final double INTERACT_RANGE_SQUARED = Math.pow(2.5, 2);

    private static final ItemStack ARROW = new ItemStackBuilder(Material.ARROW).buildNms();

    /*
     * Transforms for the arrow ingredient
     */
    private static final Matrix4f FUSE_ARROW_TRANSFORM = new Matrix4f(
            -0.4243f, -0.0000f, 0.4243f, 0.0000f,
            0.4243f, 0.0000f, 0.4243f, 0.0000f,
            -0.0000f, 0.6000f, -0.0000f, 0.0000f,
            0.4823f, 0.0150f, 0.5000f, 1.0000f
    );
    private static final Matrix4f[] FUSE_ARROW_WIGGLE_TRANSFORMS = new Matrix4f[]{
            new Matrix4f(
                    -0.3948F, 0.0000F, 0.4518F, 0.0000F,
                    0.4518F, 0.0000F, 0.3948F, 0.0000F,
                    -0.0000F, 0.6000F, -0.0000F, 0.0000F,
                    0.4823F, 0.0150F, 0.5000F, 1.0000F
            ),
            new Matrix4f(
                    -0.4574F, 0.0004F, 0.3884F, 0.0000F,
                    0.3884F, -0.0004F, 0.4574F, 0.0000F,
                    0.0006F, 0.6000F, 0.0000F, 0.0000F,
                    0.4823F, 0.0150F, 0.4699F, 1.0000F
            )
    };

    /**
     * Transform for the lingering potion ingredient
     */
    // Original transform
    private static final Matrix4f FUSE_POTION_TRANSFORM = new Matrix4f(
            0.0000f, -0.0000f, -0.4000f, 0.0000f,
            -0.4000f, 0.0000f, 0.0000f, 0.0000f,
            0.0000f, 0.8000f, -0.0000f, 0.0000f,
            0.6556f, 0.0150f, 0.4673f, 1.0000f
    );
    private static final Matrix4f[] FUSE_POTION_WIGGLE_TRANSFORMS = new Matrix4f[]{
            new Matrix4f(
                    0.0655F, -0.0000F, -0.3946F, 0.0000F,
                    -0.3946F, 0.0000F, -0.0655F, 0.0000F,
                    0.0000F, 0.8000F, -0.0000F, 0.0000F,
                    0.6556F, 0.0150F, 0.4673F, 1.0000F
            ),
            new Matrix4f(
                    -0.0437F, -0.0000F, -0.3976F, 0.0000F,
                    -0.3976F, 0.0000F, 0.0437F, 0.0000F,
                    0.0000F, 0.8000F, -0.0000F, 0.0000F,
                    0.6556F, 0.0150F, 0.4673F, 1.0000F
            )
    };

    /**
     * Transform for the crafted tipped arrow
     */
    private static final Matrix4f TIPPED_ARROW_TRANSFORM = new Matrix4f(
            -0.4591f, -0.0229f, 0.4595f, 0.0000f,
            0.4595f, 0.0096f, 0.4596f, 0.0000f,
            -0.0230f, 0.6495f, 0.0094f, 0.0000f,
            0.5000f, 0.0150f, 0.5000f, 1.0000f
    );

    private static final Vector FLETCHING_TABLE_OFFSET = new Vector(0, 1, 0);

    @Nullable
    private BlockPos fletchingTable;

    /*
     * Animation variables
     */
    @Nullable
    private PotionType potionType;
    @Nullable
    private Color potionColor;
    @Nullable
    private ItemStack lingeringPotion;
    @Nullable
    private ItemStack tippedArrow;

    @Nullable
    private ItemDisplay fuseArrowDisplay;
    @Nullable
    private ItemDisplay fusePotionDisplay;
    @Nullable
    private ItemDisplay tippedArrowDisplay;

    @Nonnull
    private AnimationState animationState;
    private int animationTicksRemaining;

    private int animationWiggleIndex;

    public MakeTippedArrowsBehavior() {
        // Preconditions to this behavior
        super(Map.of(
                        // The villager should have a job site (fletching table)
                        MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT
                ), TimeUtil.seconds(30), 0,
                TimeUtil.minutes(2), INTERACT_RANGE_SQUARED,
                5, 1,
                TimeUtil.seconds(20), AnimationState.getTotalDuration() + TimeUtil.seconds(1));

        this.fletchingTable = null;
        this.potionType = null;
        this.potionColor = null;
        this.lingeringPotion = null;
        this.tippedArrow = null;

        this.fuseArrowDisplay = null;
        this.fusePotionDisplay = null;
        this.tippedArrowDisplay = null;

        this.animationState = AnimationState.NOT_STARTED;
        this.animationTicksRemaining = 0;

        this.animationWiggleIndex = 0;
    }

    @Override
    protected boolean scan(@Nonnull ServerLevel level, @Nonnull Villager villager) {
        // Confirm that the job site is a fletching table
        Brain<Villager> brain = villager.getBrain();
        Optional<GlobalPos> memory = brain.getMemory(MemoryModuleType.JOB_SITE);
        if (memory.isEmpty()) {
            return false;
        }

        GlobalPos fletchingTable = memory.get();
        BlockState state = level.getBlockState(fletchingTable.pos());
        if (!isFletchingTable(state)) {
            return false;
        }

        // Block is a fletching table, we are good to go
        this.fletchingTable = fletchingTable.pos();

        this.potionType = RandomUtil.choice(PotionType.values());
        this.potionColor = Optional.ofNullable(this.potionType.getEffectType()).map(PotionEffectType::getColor).orElse(null);
        this.lingeringPotion = getLingeringPotion(this.potionType);
        this.tippedArrow = getTippedArrow(this.potionType);

        this.fuseArrowDisplay = null;
        this.fusePotionDisplay = null;
        this.tippedArrowDisplay = null;

        this.animationState = AnimationState.NOT_STARTED;
        this.animationTicksRemaining = 0;

        this.animationWiggleIndex = 0;

        return true;
    }

    @Override
    protected boolean checkExtraCanStillUseConditions(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        return this.fletchingTable != null && isFletchingTable(level.getBlockState(this.fletchingTable));
    }

    @Override
    protected void tickExtra(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        if (this.fletchingTable != null) {
            villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.fletchingTable));
        }
    }

    @Override
    protected void navigateToTarget(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        if (this.fletchingTable != null) {
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.fletchingTable, 0.5F, 1));
        }
    }

    @Override
    protected void interactWithTarget(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        // We are close enough to the enchanting table
        if (!(villager instanceof BaseVillager baseVillager) || this.fletchingTable == null) {
            return;
        }

        switch (this.animationState) {
            case HOLD_ARROW -> baseVillager.setHeldItem(ARROW);
            case PLACE_ARROW_INSTANT -> {
                baseVillager.clearHeldItem();
                baseVillager.playSound(SoundEvents.ITEM_FRAME_ADD_ITEM, 1F, 1.3F);

                // Place the arrow on the fletching table
                this.fuseArrowDisplay = (ItemDisplay) level.getWorld().spawnEntity(getFletchingTableLocation(level), EntityType.ITEM_DISPLAY);
                this.fuseArrowDisplay.setItemStack(CraftItemStack.asBukkitCopy(ARROW));
                this.fuseArrowDisplay.setTransformationMatrix(FUSE_ARROW_TRANSFORM);
                EntityModuleController.temporaryBukkitEntities.add(this.fuseArrowDisplay);
            }
            case HOLD_POTION -> baseVillager.setHeldItem(this.lingeringPotion);
            case PLACE_POTION_INSTANT -> {
                baseVillager.clearHeldItem();
                baseVillager.playSound(SoundEvents.ITEM_FRAME_ADD_ITEM, 1F, 1.3F);

                // Place the potion on the fletching table
                this.fusePotionDisplay = (ItemDisplay) level.getWorld().spawnEntity(getFletchingTableLocation(level), EntityType.ITEM_DISPLAY);
                this.fusePotionDisplay.setItemStack(CraftItemStack.asBukkitCopy(this.lingeringPotion));
                this.fusePotionDisplay.setTransformationMatrix(FUSE_POTION_TRANSFORM);
                EntityModuleController.temporaryBukkitEntities.add(this.fusePotionDisplay);
            }
            case CRAFTING_TIPPED_ARROW -> {
                // Display crafting animations
                this.animationWiggleIndex = (this.animationWiggleIndex + 1) % FUSE_ARROW_WIGGLE_TRANSFORMS.length;
                if (this.fuseArrowDisplay != null) {
                    this.fuseArrowDisplay.setTransformationMatrix(FUSE_ARROW_WIGGLE_TRANSFORMS[this.animationWiggleIndex]);
                }
                if (this.fusePotionDisplay != null) {
                    this.fusePotionDisplay.setTransformationMatrix(FUSE_POTION_WIGGLE_TRANSFORMS[this.animationWiggleIndex]);
                }

                // Display particles
                if (this.animationTicksRemaining % 5 == 0) {
                    Location fletchingTableLocation = getFletchingTableLocation(level).add(0.5, 0, 0.5);
                    ParticleUtil.globalParticle(fletchingTableLocation, Particle.CRIT_MAGIC, 1, 0.3, 0.1, 0.3, 0.1);
                }
            }
            case TIPPED_ARROW_APPEAR_INSTANT -> {
                // Remove the arrow & potion displays
                if (this.fuseArrowDisplay != null) {
                    EntityModuleController.temporaryBukkitEntities.remove(this.fuseArrowDisplay);
                    this.fuseArrowDisplay.remove();
                    this.fuseArrowDisplay = null;
                }
                if (this.fusePotionDisplay != null) {
                    EntityModuleController.temporaryBukkitEntities.remove(this.fusePotionDisplay);
                    this.fusePotionDisplay.remove();
                    this.fusePotionDisplay = null;
                }

                // Place the tipped arrow on the fletching table
                Location fletchingTableLocation = getFletchingTableLocation(level);
                this.tippedArrowDisplay = (ItemDisplay) level.getWorld().spawnEntity(fletchingTableLocation, EntityType.ITEM_DISPLAY);
                this.tippedArrowDisplay.setItemStack(CraftItemStack.asBukkitCopy(this.tippedArrow));
                this.tippedArrowDisplay.setTransformationMatrix(TIPPED_ARROW_TRANSFORM);
                EntityModuleController.temporaryBukkitEntities.add(this.tippedArrowDisplay);

                // Play effects
                SoundUtil.playSoundPublic(fletchingTableLocation, Sound.ENTITY_VILLAGER_WORK_FLETCHER, 1.0F);
                ParticleUtil.globalParticle(fletchingTableLocation.add(0.5, 0, 0.5), Particle.FIREWORKS_SPARK, 3, 0.3, 0.1, 0.3, 0.1);
            }
            case WAIT -> {
                // Display potion particles
                if (this.animationTicksRemaining % 10 == 0 && this.potionColor != null) {
                    Location fletchingTableLocation = getFletchingTableLocation(level).add(0.5, 0, 0.5);
                    ParticleUtil.coloredPotion(fletchingTableLocation, this.potionColor.getRed(), this.potionColor.getGreen(), this.potionColor.getBlue());
                }
            }
            case COLLECT_TIPPED_ARROW_INSTANT -> {
                baseVillager.playSound(SoundEvents.ITEM_PICKUP);

                // Remove the tipped arrow display
                if (this.tippedArrowDisplay != null) {
                    EntityModuleController.temporaryBukkitEntities.remove(this.tippedArrowDisplay);
                    this.tippedArrowDisplay.remove();
                    this.tippedArrowDisplay = null;
                }
            }
        }

        // Tick animation timer
        if (--this.animationTicksRemaining < 0) {
            AnimationState nextState = this.animationState.getNextState();
            if (nextState == null) {
                // Animation is complete, stop behavior
                this.doStop(level, villager, gameTime);
                return;
            }

            // Update animation state
            this.animationState = this.animationState.getNextState();
            this.animationTicksRemaining = this.animationState.getDuration();
        }
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.stop(level, villager, gameTime);
        if (!(villager instanceof BaseVillager baseVillager)) {
            return;
        }

        // Clear held item
        baseVillager.clearHeldItem();

        // Enable default walk target setting
        baseVillager.setDefaultWalkTargetDisabled(false);

        // Reset variables
        this.fletchingTable = null;

        this.potionType = null;
        this.lingeringPotion = null;
        this.tippedArrow = null;

        this.fuseArrowDisplay = null;
        this.fusePotionDisplay = null;
        this.tippedArrowDisplay = null;

        this.animationState = AnimationState.NOT_STARTED;
        this.animationTicksRemaining = 0;

        this.animationWiggleIndex = 0;
    }

    @Override
    protected boolean hasTarget() {
        return true;
    }

    @Override
    protected boolean isTargetReachable(Villager villager) {
        if (this.fletchingTable == null) {
            return false;
        }
        return villager.distanceToSqr(this.fletchingTable.getX(), this.fletchingTable.getY(), this.fletchingTable.getZ()) < INTERACT_RANGE_SQUARED;
    }

    @Nonnull
    @Override
    public ItemStackBuilder getGuiItemBuilderAbstract() {
        return new PotionItemStackBuilder(PotionItemStackBuilder.PotionType.TIPPED_ARROW)
                .setBasePotionEffect(new PotionData(PotionType.WATER))
                .setDisplayName("&eCraft Tipped Arrows Behavior")
                .setLore("&7Fletchers will occasionally craft tipped arrows");
    }

    @Nonnull
    private Location getFletchingTableLocation(@Nonnull ServerLevel level) {
        if (this.fletchingTable == null) {
            throw new IllegalStateException("Fletching table location should not be null");
        }
        return LocationUtil.fromBlockPos(level.getWorld(), this.fletchingTable).add(FLETCHING_TABLE_OFFSET);
    }

    private static boolean isFletchingTable(@Nonnull BlockState state) {
        return state.is(Blocks.FLETCHING_TABLE);
    }


    private static ItemStack getLingeringPotion(PotionType potionType) {
        return new PotionItemStackBuilder(PotionItemStackBuilder.PotionType.LINGERING)
                .setBasePotionEffect(new PotionData(potionType))
                .buildNms();
    }

    private static ItemStack getTippedArrow(PotionType potionType) {
        return new PotionItemStackBuilder(PotionItemStackBuilder.PotionType.TIPPED_ARROW)
                .setBasePotionEffect(new PotionData(potionType))
                .buildNms();
    }

    @AllArgsConstructor
    private enum AnimationState {

        COLLECT_TIPPED_ARROW_INSTANT(null, 0),
        WAIT(COLLECT_TIPPED_ARROW_INSTANT, TimeUtil.seconds(1)),
        TIPPED_ARROW_APPEAR_INSTANT(WAIT, 0),
        CRAFTING_TIPPED_ARROW(TIPPED_ARROW_APPEAR_INSTANT, TimeUtil.seconds(1)),
        PLACE_POTION_INSTANT(CRAFTING_TIPPED_ARROW, 0),
        HOLD_POTION(PLACE_POTION_INSTANT, TimeUtil.seconds(1)),
        PLACE_ARROW_INSTANT(HOLD_POTION, 0),
        HOLD_ARROW(PLACE_ARROW_INSTANT, TimeUtil.seconds(1)),
        NOT_STARTED(HOLD_ARROW, 0);

        @Getter
        @Nullable
        private final AnimationState nextState;

        /**
         * Duration of this animation state in ticks
         * - set to 0 for a one-off animation
         */
        @Getter
        private final int duration;

        public static int getTotalDuration() {
            int total = 0;
            for (AnimationState controller : values()) {
                total += controller.getDuration();
            }
            return total;
        }

    }

}

