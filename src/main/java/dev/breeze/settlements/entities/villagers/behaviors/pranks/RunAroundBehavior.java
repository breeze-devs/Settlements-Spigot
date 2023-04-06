package dev.breeze.settlements.entities.villagers.behaviors.pranks;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.behaviors.BaseVillagerBehavior;
import dev.breeze.settlements.utils.MessageUtil;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.SoundUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public final class RunAroundBehavior extends BaseVillagerBehavior {

    private static final float SPEED_MODIFIER = 1.0F;

    private static final int MAX_RUN_DURATION = TimeUtil.seconds(10);
    private static final int BEHAVIOR_COOLDOWN = TimeUtil.minutes(3);

    private int cooldown;
    private int duration;

    public RunAroundBehavior() {
        super(Map.of(
                // The villager should not be interacting with anyone
                MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_ABSENT
        ), MAX_RUN_DURATION, TimeUtil.seconds(30));

        this.cooldown = RandomUtil.RANDOM.nextInt(BEHAVIOR_COOLDOWN);
        this.duration = 0;
    }

    @Override
    protected boolean checkExtraStartConditionsRateLimited(@Nonnull ServerLevel level, @Nonnull Villager villager) {
        // Not -1 because this method is rate limited
        this.cooldown -= this.getMaxStartConditionCheckCooldown();
        MessageUtil.debug("&b[Debug] Cooldown for " + this.getClass().getSimpleName() + " is " + this.cooldown);
        return this.cooldown <= 0;
    }

    @Override
    protected boolean canStillUse(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        return this.duration++ < MAX_RUN_DURATION;
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.start(level, villager, gameTime);

        // Disable default walk target setting
        if (villager instanceof BaseVillager baseVillager)
            baseVillager.setDefaultWalkTargetDisabled(true);

        // Clear relevant memories
        villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
    }

    @Override
    protected void tick(@Nonnull ServerLevel level, Villager villager, long gameTime) {
        Location loc = new Location(level.getWorld(), villager.getX(), villager.getY(), villager.getZ());
        // Display particles
        if (gameTime % 5 == 0) {
            ParticleUtil.globalParticle(loc, Particle.CLOUD, 1, 0.2, 0.2, 0.2, 0.03);
        }

        // Jump randomly
        if (gameTime % 10 == 0 && RandomUtil.RANDOM.nextBoolean()) {
            villager.getJumpControl().jump();
            SoundUtil.playSoundPublic(loc, Sound.ENTITY_VILLAGER_AMBIENT, RandomUtil.RANDOM.nextFloat() + 0.3F);
        }

        PathNavigation navigation = villager.getNavigation();
        if (!navigation.isDone())
            return;

        // Randomize a position
        BlockPos target = getVillageBoundRandomPos(level, villager);
        if (target == null)
            return;

        // Set route
        villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(target));
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, SPEED_MODIFIER, 2));
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.stop(level, villager, gameTime);

        // Enable default walk target setting
        if (villager instanceof BaseVillager baseVillager) {
            baseVillager.setDefaultWalkTargetDisabled(false);
        }

        // Reset variables
        this.cooldown = BEHAVIOR_COOLDOWN;
        this.duration = 0;
    }

    @Nullable
    private static BlockPos getVillageBoundRandomPos(@Nonnull ServerLevel level, @Nonnull Villager villager) {
        final int horizontalRange = 15;
        final int verticalRange = 7;

        BlockPos currentPos = villager.blockPosition();

        Vec3 target = null;
        if (!level.isVillage(currentPos)) {
            // We are outside the village, generate a position back into the village
            SectionPos pos1 = SectionPos.of(currentPos);
            SectionPos pos2 = BehaviorUtils.findSectionClosestToVillage(level, pos1, 2);
            if (pos1 != pos2) {
                target = DefaultRandomPos.getPosTowards(villager, horizontalRange, verticalRange,
                        Vec3.atBottomCenterOf(pos2.center()), (float) Math.PI / 2F);
            }
        }

        if (target == null) {
            target = LandRandomPos.getPos(villager, horizontalRange, verticalRange);
        }

        if (target == null)
            return null;

        return new BlockPos(target);
    }

}

