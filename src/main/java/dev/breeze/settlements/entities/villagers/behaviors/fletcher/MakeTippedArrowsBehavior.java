package dev.breeze.settlements.entities.villagers.behaviors.fletcher;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.behaviors.InteractAtTargetBehavior;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.itemstack.PotionItemStackBuilder;
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
import org.bukkit.Material;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

public final class MakeTippedArrowsBehavior extends InteractAtTargetBehavior {

    private static final int INTERACT_RANGE = 2;

    private static final ItemStack ARROW = new ItemStackBuilder(Material.ARROW).buildNms();

    @Nullable
    private BlockPos fletchingTable;

    /*
     * Animation variables
     */
    @Nullable
    private PotionType potionType;
    @Nullable
    private ItemStack lingeringPotion;
    @Nullable
    private ItemStack tippedArrow;

    @Nonnull
    private AnimationState animationState;
    private int animationTicksRemaining;

    public MakeTippedArrowsBehavior() {
        // Preconditions to this behavior
        // TODO: scan 30s / behavior 2m
        super(Map.of(
                        // The villager should have a job site (fletching table)
                        MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT
                ), TimeUtil.seconds(5), 0,
                TimeUtil.seconds(30), Math.pow(INTERACT_RANGE, 2),
                5, 1,
                TimeUtil.seconds(20), AnimationState.getTotalDuration());

        this.fletchingTable = null;
        this.potionType = null;
        this.lingeringPotion = null;
        this.tippedArrow = null;

        this.animationState = AnimationState.NOT_STARTED;
        this.animationTicksRemaining = 0;
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
        BlockState state = level.getBlockStateIfLoaded(fletchingTable.pos());
        if (state == null || !isFletchingTable(state)) {
            return false;
        }

        // Block is a fletching table, we are good to go
        this.fletchingTable = fletchingTable.pos();

        this.potionType = RandomUtil.choice(PotionType.values());
        this.lingeringPotion = getLingeringPotion(this.potionType);
        this.tippedArrow = getTippedArrow(this.potionType);

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

        // TODO: use item display in 1.20
        switch (this.animationState) {
            case HOLD_ARROW -> {
                baseVillager.setHeldItem(ARROW);
            }
            case PLACE_ARROW -> {
                baseVillager.clearHeldItem();
                baseVillager.playSound(SoundEvents.ITEM_FRAME_ADD_ITEM, 0.3F, 1.3F);
                // TODO: place the arrow on the fletching table
            }
            case HOLD_POTION -> {
                baseVillager.setHeldItem(this.lingeringPotion);
            }
            case PLACE_POTION -> {
                baseVillager.clearHeldItem();
                baseVillager.playSound(SoundEvents.ITEM_FRAME_ADD_ITEM, 0.3F, 1.3F);
                // TODO: place the potion on the fletching table
            }
            case CRAFT_TIPPED_ARROW -> {
                // TODO: fuse the arrow & potion into a tipped arrow
            }
            case WAIT -> {
                // Do nothing
            }
            case COLLECT_TIPPED_ARROW -> {
                baseVillager.playSound(SoundEvents.ITEM_PICKUP);
                // TODO: remove arrow & potion
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

        this.animationState = AnimationState.NOT_STARTED;
        this.animationTicksRemaining = 0;
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
        return villager.distanceToSqr(this.fletchingTable.getX(), this.fletchingTable.getY(), this.fletchingTable.getZ()) < INTERACT_RANGE;
    }

    @Nonnull
    @Override
    public ItemStackBuilder getGuiItemBuilderAbstract() {
        return new ItemStackBuilder(Material.ENCHANTING_TABLE)
                .setDisplayName("&eCraft Tipped Arrows Behavior")
                .setLore("&7Fletchers will occasionally craft tipped arrows");
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

        COLLECT_TIPPED_ARROW(null, 0),
        WAIT(COLLECT_TIPPED_ARROW, TimeUtil.seconds(2)),
        CRAFT_TIPPED_ARROW(WAIT, TimeUtil.seconds(2)),
        PLACE_POTION(CRAFT_TIPPED_ARROW, 0),
        HOLD_POTION(PLACE_POTION, TimeUtil.seconds(1)),
        PLACE_ARROW(HOLD_POTION, 0),
        HOLD_ARROW(PLACE_ARROW, TimeUtil.seconds(1)),
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

