package dev.breeze.settlements.entities.villagers.behaviors.farmer;

import dev.breeze.settlements.config.files.FarmerHarvestConfig;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.behaviors.InteractAtTargetBehavior;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.entities.villagers.sensors.VillagerNearbyHarvestableSugarcaneSensor;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.VillagerUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import org.bukkit.Material;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class HarvestSugarcaneBehavior extends InteractAtTargetBehavior {

    /**
     * How far away from the base sugarcane (the one in set by the sensor) should we scan for nearby sugarcane blocks
     * - we are scanning in the same Y level under the assumption that the plantation is flat
     * - this accounts for both the radius in X and Z axis, so a total of n^2 blocks to scan
     */
    private static final int SCAN_RADIUS_HORIZONTAL = 8;

    /**
     * How close does the villager need to be to harvest the sugarcane
     */
    private static final int HARVEST_RANGE = 2;

    private static final ItemStack HOE = new ItemStackBuilder(Material.IRON_HOE).buildNms();

    @Nonnull
    private final List<BlockPos> harvestableSugarcaneBlocks;

    /**
     * Which position in the list 'harvestableSugarcaneBlocks' is our next target?
     * - can be -1, which means we have no more target
     */
    private int targetBlockPosIndex;

    private int harvestedCount;

    public HarvestSugarcaneBehavior() {
        // Preconditions to this behavior
        super(Map.of(
                        // There should be harvestable sugarcane nearby
                        VillagerMemoryType.NEAREST_HARVESTABLE_SUGARCANE.getMemoryModuleType(), MemoryStatus.VALUE_PRESENT
                ), TimeUtil.minutes(1), 0,
                FarmerHarvestConfig.getInstance().getSugarcaneCooldown().getValue(), Math.pow(HARVEST_RANGE, 2),
                5, 5,
                TimeUtil.seconds(40), TimeUtil.seconds(5));

        this.harvestableSugarcaneBlocks = new ArrayList<>();
        this.targetBlockPosIndex = -1;
        this.harvestedCount = 0;
    }

    @Override
    protected boolean scan(@Nonnull ServerLevel level, @Nonnull Villager villager) {
        if (!(villager instanceof BaseVillager baseVillager)) {
            return false;
        }

        // Check mob griefing rule
        if (FarmerHarvestConfig.getInstance().getRespectMobGriefing().getValue() && !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return false;
        }

        // Check that we can actually harvest sugarcanes
        if (FarmerHarvestConfig.getInstance().getSugarcaneExpertiseMap().get(baseVillager.getExpertiseLevel()) == 0) {
            return false;
        }

        // Attempt to get the closest harvestable sugarcane
        BlockPos baseSugarcane = VillagerMemoryType.NEAREST_HARVESTABLE_SUGARCANE.get(villager.getBrain());
        if (baseSugarcane == null) {
            return false;
        }

        // Check that the target block is actually sugarcane
        if (!VillagerNearbyHarvestableSugarcaneSensor.isHarvestableSugarcane(level, baseSugarcane)) {
            // Not a harvestable sugarcane block, reset memory
            VillagerMemoryType.NEAREST_HARVESTABLE_SUGARCANE.set(villager.getBrain(), null);
            return false;
        }

        // Scan nearby blocks for harvestable sugarcane blocks
        BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos(baseSugarcane.getX(), baseSugarcane.getY(), baseSugarcane.getZ());
        for (int dx = -SCAN_RADIUS_HORIZONTAL; dx <= SCAN_RADIUS_HORIZONTAL; dx++) {
            for (int dz = -SCAN_RADIUS_HORIZONTAL; dz <= SCAN_RADIUS_HORIZONTAL; dz++) {
                // Apply offset
                scan.move(dx, 0, dz);

                // Check if sugarcane is ready for harvest
                if (VillagerNearbyHarvestableSugarcaneSensor.isHarvestableSugarcane(level, scan)) {
                    this.harvestableSugarcaneBlocks.add(scan.immutable());
                }

                // Reset offset
                scan.move(-dx, 0, -dz);
            }
        }

        // Return & set variable
        if (this.harvestableSugarcaneBlocks.isEmpty()) {
            this.targetBlockPosIndex = -1;
            return false;
        }

        this.targetBlockPosIndex = 0;
        return true;
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.start(level, villager, gameTime);
        if (!(villager instanceof BaseVillager baseVillager)) {
            return;
        }

        // Disable default walking
        baseVillager.setDefaultWalkTargetDisabled(true);
    }

    @Override
    protected boolean checkExtraCanStillUseConditions(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        if (!(villager instanceof BaseVillager baseVillager)) {
            return false;
        }

        return this.targetBlockPosIndex != -1 && this.harvestedCount < FarmerHarvestConfig.getInstance().getSugarcaneExpertiseMap().get(baseVillager.getExpertiseLevel());
    }

    @Override
    protected void tickExtra(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        if (!(villager instanceof BaseVillager baseVillager) || this.targetBlockPosIndex == -1) {
            return;
        }

        // Hold item
        baseVillager.setHeldItem(HOE);

        // Look at target
        baseVillager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.harvestableSugarcaneBlocks.get(this.targetBlockPosIndex)));
    }

    @Override
    protected void navigateToTarget(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        if (this.targetBlockPosIndex == -1 || !villager.getNavigation().isDone()) {
            return;
        }

        // Walk to the sugarcane
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.harvestableSugarcaneBlocks.get(this.targetBlockPosIndex), 0.5F, 1));
    }

    @Override
    protected void interactWithTarget(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        if (this.targetBlockPosIndex == -1) {
            return;
        }

        // We are close enough to harvest the sugarcane
        BlockPos target = this.harvestableSugarcaneBlocks.get(this.targetBlockPosIndex);

        // Check if it's actually a harvestable sugarcane block
        boolean canHarvest = VillagerNearbyHarvestableSugarcaneSensor.isHarvestableSugarcane(level, target);
        if (canHarvest) {
            level.destroyBlock(target, true, villager);
            this.harvestedCount++;
        }

        // Increment target pointer
        if (this.targetBlockPosIndex + 1 < this.harvestableSugarcaneBlocks.size()) {
            this.targetBlockPosIndex++;
        } else {
            this.targetBlockPosIndex = -1;
            this.doStop(level, villager, gameTime);
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
        this.harvestableSugarcaneBlocks.clear();
        this.targetBlockPosIndex = -1;
        this.harvestedCount = 0;
    }

    @Override
    protected boolean hasTarget() {
        return this.targetBlockPosIndex != -1;
    }

    @Override
    protected boolean isTargetReachable(Villager villager) {
        if (this.targetBlockPosIndex == -1) {
            return false;
        }

        BlockPos target = this.harvestableSugarcaneBlocks.get(this.targetBlockPosIndex);
        return villager.distanceToSqr(target.getX(), target.getY(), target.getZ()) < HARVEST_RANGE;
    }

    @Nonnull
    @Override
    public ItemStackBuilder getGuiItemBuilderAbstract() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Frequently harvest nearby sugarcanes that are 2+ blocks tall");
        lore.add("&7Number of sugarcanes harvested depends on expertise:");
        for (int key : FarmerHarvestConfig.getInstance().getSugarcaneExpertiseMap().keySet().stream().sorted().toList()) {
            if (FarmerHarvestConfig.getInstance().getSugarcaneExpertiseMap().get(key) == 0) {
                lore.add("&7- %s&7: unable to harvest".formatted(VillagerUtil.getExpertiseName(key, true)));
            } else {
                lore.add("&7- %s&7: %d sugarcanes".formatted(VillagerUtil.getExpertiseName(key, true),
                        FarmerHarvestConfig.getInstance().getSugarcaneExpertiseMap().get(key)));
            }
        }

        return new ItemStackBuilder(Material.SUGAR_CANE)
                .setDisplayName("&eHarvest nearby sugarcane behavior")
                .setLore(lore);
    }

}

