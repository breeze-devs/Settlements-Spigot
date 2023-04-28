package dev.breeze.settlements.entities.villagers.behaviors.pranks;

import dev.breeze.settlements.config.files.NitwitPranksConfig;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.behaviors.InteractAtTargetBehavior;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Material;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public final class RingBellBehavior extends InteractAtTargetBehavior {

    /**
     * How close should the villager be to the water before casting
     */
    private static final int MAX_DISTANCE_FROM_BELL = NitwitPranksConfig.getInstance().getRingBellDistance().getValue();
    private static final double MAX_DISTANCE_FROM_BELL_SQUARED = Math.pow(MAX_DISTANCE_FROM_BELL, 2);

    @Nullable
    private BlockPos bell;

    public RingBellBehavior() {
        // Preconditions to this behavior
        super(Map.of(
                        // The villager should have seen water nearby
                        MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT
                ), TimeUtil.minutes(1), 0,
                NitwitPranksConfig.getInstance().getRingBellCooldown().getValue(), MAX_DISTANCE_FROM_BELL_SQUARED,
                5, 10,
                TimeUtil.seconds(20), TimeUtil.seconds(1));
    }

    @Override
    protected boolean scan(@Nonnull ServerLevel level, @Nonnull Villager villager) {
        if (!villager.getBrain().hasMemoryValue(MemoryModuleType.MEETING_POINT))
            return false;

        BlockPos bellPos = villager.getBrain().getMemory(MemoryModuleType.MEETING_POINT).get().pos();
        if (!isBell(level, bellPos))
            return false;

        // Save bell position
        this.bell = bellPos;
        return true;
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.start(level, villager, gameTime);
        if (!(villager instanceof BaseVillager baseVillager))
            return;

        // Disable default walking
        baseVillager.setDefaultWalkTargetDisabled(true);
    }

    @Override
    protected boolean checkExtraCanStillUseConditions(@Nonnull ServerLevel level, @Nonnull Villager villager, @Nonnull long gameTime) {
        if (this.bell == null)
            return false;

        return isBell(level, this.bell);
    }

    @Override
    protected void tickExtra(@Nonnull ServerLevel level, @Nonnull Villager villager, @Nonnull long gameTime) {
        if (!(villager instanceof BaseVillager baseVillager))
            return;

        // Look at the bell
        if (this.bell != null) {
            baseVillager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.bell));
        }
    }

    @Override
    protected void navigateToTarget(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        if (this.bell != null) {
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.bell, 0.55F, MAX_DISTANCE_FROM_BELL));
        }
    }

    @Override
    protected void interactWithTarget(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        // Safety check
        if (this.bell == null)
            return;

        // We are close enough to the bell, ring it
        BellBlock bellBlock = (BellBlock) level.getBlockState(this.bell).getBlock();
        bellBlock.attemptToRing(villager, level, this.bell, null);
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.stop(level, villager, gameTime);

        // Reset variables
        this.bell = null;

        // Enable default walk target setting
        if (villager instanceof BaseVillager baseVillager) {
            baseVillager.setDefaultWalkTargetDisabled(false);
        }
    }

    @Override
    protected boolean hasTarget() {
        return true;
    }

    @Override
    protected boolean isTargetReachable(@Nonnull Villager villager) {
        if (this.bell == null)
            return false;
        return villager.distanceToSqr(this.bell.getX(), this.bell.getY(), this.bell.getZ()) < MAX_DISTANCE_FROM_BELL_SQUARED;
    }

    @Nonnull
    @Override
    public ItemStackBuilder getGuiItemBuilderAbstract() {
        return new ItemStackBuilder(Material.BELL)
                .setDisplayName("&eRing bell prank")
                .setLore("&7Infrequently ring the bell to annoy other villagers");
    }

    private static boolean isBell(@Nonnull ServerLevel level, @Nonnull BlockPos bellPos) {
        BlockState state = level.getBlockState(bellPos);
        if (!state.is(Blocks.BELL))
            return false;
        return state.getBlock() instanceof BellBlock;
    }

}

