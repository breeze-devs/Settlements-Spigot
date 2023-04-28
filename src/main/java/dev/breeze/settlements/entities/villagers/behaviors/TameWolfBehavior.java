package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.SoundUtil;
import dev.breeze.settlements.utils.StringUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TameWolfBehavior extends InteractAtTargetBehavior {

    private static final ItemStack BONE = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.BONE).build());

    /**
     * The chance of the villager taming a wolf
     */
    private static final double TAME_CHANCE = 0.3;

    @Nullable
    private Wolf targetWolf;

    public TameWolfBehavior() {
        // Preconditions to this behavior
        super(Map.of(
                        // The villager should not already have a wolf
                        VillagerMemoryType.OWNED_DOG.getMemoryModuleType(), MemoryStatus.VALUE_ABSENT,
                        // There should be living entities nearby
                        MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
                ), TimeUtil.seconds(30), Math.pow(20, 2),
                TimeUtil.minutes(2), Math.pow(1.5, 2),
                5, TimeUtil.seconds(1),
                TimeUtil.seconds(20), TimeUtil.seconds(10));

        this.targetWolf = null;
    }

    @Override
    protected boolean scan(ServerLevel level, Villager villager) {
        Brain<Villager> brain = villager.getBrain();

        // If there are no nearby living entities, ignore
        if (brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).isEmpty())
            return false;

        if (this.targetWolf == null) {
            // Check for nearby untamed wolves
            List<LivingEntity> target = brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).get();
            Optional<LivingEntity> nearestWolf = target.stream().filter(e -> {
                if (e.getType() != EntityType.WOLF || !(e instanceof Wolf wolf))
                    return false;
                // Should not tame villager wolf
                if (e instanceof VillagerWolf)
                    return false;
                // Check if wolf is already owned/tamed
                return !wolf.isTame();
            }).findFirst();

            if (nearestWolf.isEmpty())
                return false;

            this.targetWolf = (Wolf) nearestWolf.get();
            villager.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, nearestWolf.get());
        }
        return true;
    }

    @Override
    protected boolean checkExtraCanStillUseConditions(ServerLevel level, Villager villager, long gameTime) {
        if (this.targetWolf == null || !this.targetWolf.isAlive())
            return false;
        return this.targetWolf.getOwnerUUID() == null;
    }

    @Override
    protected void tickExtra(ServerLevel level, Villager villager, long gameTime) {
        if (villager instanceof BaseVillager baseVillager)
            baseVillager.setHeldItem(BONE);

        if (this.targetWolf != null)
            villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.targetWolf, true));
    }

    @Override
    protected void navigateToTarget(ServerLevel level, Villager villager, long gameTime) {
        if (this.targetWolf == null)
            return;
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetWolf, 0.6F, 1));
    }

    @Override
    protected void interactWithTarget(ServerLevel level, Villager villager, long gameTime) {
        // Safety check
        if (this.targetWolf == null)
            return;

        // Attempt to tame the wolf
        boolean successful = RandomUtil.RANDOM.nextDouble() < TAME_CHANCE;

        // Play effect
        Location villagerLoc = new Location(level.getWorld(), villager.getX(), villager.getY() + 1.8, villager.getZ());
        ParticleUtil.globalParticle(villagerLoc, successful ? Particle.VILLAGER_HAPPY : Particle.VILLAGER_ANGRY, 3, 0.2, 0.2, 0.2, 0.1);
        SoundUtil.playSoundPublic(villagerLoc, successful ? Sound.ENTITY_VILLAGER_YES : Sound.ENTITY_VILLAGER_NO, 1.2f);

        Location wolfLoc = new Location(level.getWorld(), this.targetWolf.getX(), this.targetWolf.getEyeY(), this.targetWolf.getZ());
        ParticleUtil.globalParticle(wolfLoc, successful ? Particle.HEART : Particle.SMOKE_NORMAL, successful ? 3 : 8, 0.2, 0.2, 0.2, 0);

        // Successful taming logic
        if (successful) {
            // Remove the vanilla wolf and spawn a VillagerWolf instead
            Location wolfLocation = new Location(level.getWorld(), this.targetWolf.getX(), this.targetWolf.getY(), this.targetWolf.getZ());
            this.targetWolf.remove(Entity.RemovalReason.KILLED);
            this.targetWolf = null;

            // Spawn a VillagerWolf instead
            VillagerWolf villagerWolf = new VillagerWolf(wolfLocation);
            if (villager instanceof BaseVillager baseVillager)
                villagerWolf.tameByVillager(baseVillager);

            // Set memory
            VillagerMemoryType.OWNED_DOG.set(villager.getBrain(), villagerWolf.getUUID());

            // Stop after taming
            this.doStop(level, villager, gameTime);
        }
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.stop(level, villager, gameTime);

        // Reset held item
        if (villager instanceof BaseVillager baseVillager)
            baseVillager.clearHeldItem();

        // Remove interaction memory
        villager.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);

        // Reset variables
        this.targetWolf = null;
    }

    @Override
    protected boolean hasTarget() {
        return this.targetWolf != null;
    }

    @Override
    protected boolean isTargetReachable(Villager villager) {
        return this.targetWolf != null && villager.distanceToSqr(this.targetWolf) < this.getInteractRangeSquared();
    }

    @Nonnull
    @Override
    public ItemStackBuilder getGuiItemBuilderAbstract() {
        return new ItemStackBuilder(Material.BONE)
                .setDisplayName("&eTame wolf behavior")
                .setLore(
                        "&7Attempts to adopt a nearby stray wolf if no wolf owned",
                        "&7Taming has a %s chance to succeed".formatted(StringUtil.getPercentageDisplay(TAME_CHANCE))
                );
    }

}
