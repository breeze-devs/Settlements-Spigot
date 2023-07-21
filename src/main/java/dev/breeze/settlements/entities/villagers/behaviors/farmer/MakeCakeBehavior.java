package dev.breeze.settlements.entities.villagers.behaviors.farmer;

import dev.breeze.settlements.displays.cakes.CakeDisplay;
import dev.breeze.settlements.displays.cakes.WhiteCakeDisplay;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.behaviors.InteractAtTargetBehavior;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.utils.LocationUtil;
import dev.breeze.settlements.utils.SoundUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public final class MakeCakeBehavior extends InteractAtTargetBehavior {

    private static final int INTERACT_RANGE = 2;

    @Nullable
    private BlockPos craftingTable;
    @Nullable
    private CakeDisplay cakeDisplay;

    private int animationTicksBeforeNextStep;

    public MakeCakeBehavior() {
        // Preconditions to this behavior
        // TODO: scan 30 / interact 10m
        super(Map.of(
                        VillagerMemoryType.NEAREST_CRAFTING_TABLE.getMemoryModuleType(), MemoryStatus.VALUE_PRESENT
                ), TimeUtil.seconds(10), 0,
                TimeUtil.seconds(20), Math.pow(INTERACT_RANGE, 2),
                5, 1,
                TimeUtil.seconds(20), TimeUtil.seconds(10)); // TODO: how many seconds for cake?

        this.craftingTable = null;
        this.cakeDisplay = null;

        this.animationTicksBeforeNextStep = 0;
    }

    @Override
    protected boolean scan(@Nonnull ServerLevel level, @Nonnull Villager villager) {
        BlockPos pos = VillagerMemoryType.NEAREST_CRAFTING_TABLE.get(villager.getBrain());
        if (pos == null || !villager.level().getBlockState(pos).is(Blocks.CRAFTING_TABLE)) {
            return false;
        }

        this.craftingTable = pos;
        this.cakeDisplay = WhiteCakeDisplay.getCakeDisplay(); // TODO: change to random cake type
        this.animationTicksBeforeNextStep = 0;
        return true;
    }

    @Override
    protected boolean checkExtraCanStillUseConditions(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        return this.craftingTable != null && villager.level().getBlockState(this.craftingTable).is(Blocks.CRAFTING_TABLE);
    }

    @Override
    protected void tickExtra(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        if (!(villager instanceof BaseVillager baseVillager))
            return;

        // Look at target
        if (this.craftingTable != null) {
            baseVillager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.craftingTable));
        }

    }

    @Override
    protected void navigateToTarget(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        if (this.craftingTable != null) {
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.craftingTable, 0.5F, 1));
        }
    }

    @Override
    protected void interactWithTarget(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        // We are close enough to the enchanting table
        if (this.craftingTable == null || this.cakeDisplay == null) {
            return;
        }

        if (--this.animationTicksBeforeNextStep > 0) {
            return;
        } else if (!this.cakeDisplay.hasNextStep()) {
            // We are done baking the cake
            this.doStop(level, villager, gameTime);
            return;
        }

        // Display animation & set animation delay
        Location tableTop = LocationUtil.fromBlockPos(level.getWorld(), this.craftingTable).add(0, 1, 0);
        this.animationTicksBeforeNextStep = this.cakeDisplay.spawnNextStep(tableTop);

        // Extra logic when finishing the cake
        if (!this.cakeDisplay.hasNextStep()) {
            // Add extra delays at the end (villager wants to appreciate his hard work)
            this.animationTicksBeforeNextStep = TimeUtil.seconds(2);

            // Play effects
            SoundUtil.playSoundPublic(tableTop, Sound.ENTITY_VILLAGER_CELEBRATE, 0.3F, SoundUtil.randomPitch(1, 0.3F));
            ParticleUtil.globalParticle(tableTop.add(0.5, 0, 0.5), Particle.FIREWORKS_SPARK, 3, 0.2, 0.1, 0.2, 0.1);
            ParticleUtil.globalParticle(LocationUtil.fromNmsEntity(villager).add(0, BaseVillager.getActualEyeHeight(), 0), Particle.VILLAGER_HAPPY, 5, 0.4, 0.4, 0.4, 0.1);
        }
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.stop(level, villager, gameTime);
        if (!(villager instanceof BaseVillager baseVillager)) {
            return;
        }

        // Remove cake display
        if (this.cakeDisplay != null) {
            this.cakeDisplay.removeAll();
        }

        // Clear held item
        baseVillager.clearHeldItem();

        // Reset variables
        this.craftingTable = null;
        this.cakeDisplay = null;

        this.animationTicksBeforeNextStep = 0;
    }

    @Override
    protected boolean hasTarget() {
        return true;
    }

    @Override
    protected boolean isTargetReachable(Villager villager) {
        if (this.craftingTable == null) {
            return false;
        }
        return villager.distanceToSqr(this.craftingTable.getX(), this.craftingTable.getY(), this.craftingTable.getZ()) < Math.pow(INTERACT_RANGE, 2);
    }

    @Nonnull
    @Override
    public ItemStackBuilder getGuiItemBuilderAbstract() {
        return new ItemStackBuilder(Material.CAKE)
                .setDisplayName("&eMake cake behavior behavior")
                .setLore(
                        "&7Occasionally bakes a unique flavored cake for everyone to enjoy",
                        "&7The cake is not a lie, but you can't have it"
                );
    }

}

