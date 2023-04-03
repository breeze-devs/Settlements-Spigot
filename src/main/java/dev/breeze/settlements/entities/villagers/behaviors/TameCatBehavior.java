package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.entities.cats.VillagerCat;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.SoundUtil;
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
import net.minecraft.world.entity.animal.Cat;
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

public final class TameCatBehavior extends InteractAtTargetBehavior {

    private static final ItemStack[] FISHES = new ItemStack[]{
            CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.COD).build()),
            CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.SALMON).build())
    };

    /**
     * The chance of the villager taming a cat
     */
    private static final double TAME_CHANCE = 0.3;

    @Nullable
    private Cat target;

    @Nullable
    private ItemStack heldItem;

    public TameCatBehavior() {
        // Preconditions to this behavior
        super(Map.of(
                        // The villager should not already have a cat
                        VillagerMemoryType.OWNED_CAT, MemoryStatus.VALUE_ABSENT,
                        // There should be living entities nearby
                        MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
                ), TimeUtil.seconds(40), Math.pow(20, 2),
                TimeUtil.minutes(2), Math.pow(1.5, 2),
                5, TimeUtil.seconds(1),
                TimeUtil.seconds(30), TimeUtil.seconds(10));

        this.target = null;
    }

    @Override
    protected boolean scan(ServerLevel level, Villager villager) {
        Brain<Villager> brain = villager.getBrain();

        // If there are no nearby living entities, ignore
        if (brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).isEmpty())
            return false;

        if (this.target == null) {
            // Check for nearby untamed cats
            List<LivingEntity> target = brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).get();
            Optional<LivingEntity> nearestCat = target.stream().filter(e -> {
                if (e.getType() != EntityType.CAT || !(e instanceof Cat cat))
                    return false;
                // Should not tame villager cat (already tamed by another villager)
                // - only tame vanilla cats to convert them to a villager cat
                if (e instanceof VillagerCat)
                    return false;
                // Check if cat is already owned/tamed
                return !cat.isTame();
            }).findFirst();

            if (nearestCat.isEmpty())
                return false;

            this.target = (Cat) nearestCat.get();
            villager.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, nearestCat.get());
        }
        return true;
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.start(level, villager, gameTime);
        this.heldItem = RandomUtil.choice(FISHES);
    }

    @Override
    protected boolean checkExtraCanStillUseConditions(ServerLevel level, Villager villager, long gameTime) {
        if (this.target == null || !this.target.isAlive())
            return false;
        return this.target.getOwnerUUID() == null;
    }

    @Override
    protected void tickExtra(ServerLevel level, Villager villager, long gameTime) {
        if (this.heldItem != null && villager instanceof BaseVillager baseVillager) {
            baseVillager.setHeldItem(this.heldItem);
        }

        if (this.target != null)
            villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.target, true));
    }

    @Override
    protected void navigateToTarget(ServerLevel level, Villager villager, long gameTime) {
        if (this.target == null)
            return;
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.target, 0.5F, 1));
    }

    @Override
    protected void interactWithTarget(ServerLevel level, Villager villager, long gameTime) {
        // Safety check
        if (this.target == null)
            return;

        // Attempt to tame the cat
        boolean successful = RandomUtil.RANDOM.nextDouble() < TAME_CHANCE;

        // Play effect
        Location villagerLoc = new Location(level.getWorld(), villager.getX(), villager.getY() + 1.8, villager.getZ());
        ParticleUtil.globalParticle(villagerLoc, successful ? Particle.VILLAGER_HAPPY : Particle.VILLAGER_ANGRY, 3, 0.2, 0.2, 0.2, 0.1);
        SoundUtil.playSoundPublic(villagerLoc, successful ? Sound.ENTITY_VILLAGER_YES : Sound.ENTITY_VILLAGER_NO, 1.2f);

        Location catLoc = new Location(level.getWorld(), this.target.getX(), this.target.getEyeY(), this.target.getZ());
        ParticleUtil.globalParticle(catLoc, successful ? Particle.HEART : Particle.SMOKE_NORMAL, successful ? 3 : 8, 0.2, 0.2, 0.2, 0);

        // Successful taming logic
        if (successful) {
            // Remove the vanilla cat and spawn a VillagerCat instead
            Location catLocation = new Location(level.getWorld(), this.target.getX(), this.target.getY(), this.target.getZ());

            // Spawn a VillagerCat instead
            VillagerCat villagerCat = new VillagerCat(catLocation);
            if (villager instanceof BaseVillager baseVillager)
                villagerCat.tameByVillager(baseVillager, this.target.getVariant());

            // Remove original cat
            this.target.remove(Entity.RemovalReason.DISCARDED);
            this.target = null;

            // Set memory
            villager.getBrain().setMemory(VillagerMemoryType.OWNED_CAT, Optional.of(villagerCat.getUUID()));

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
        this.target = null;
        this.heldItem = null;
    }

    @Override
    protected boolean hasTarget() {
        return this.target != null;
    }

    @Override
    protected boolean isTargetReachable(Villager villager) {
        return this.target != null && villager.distanceToSqr(this.target) < this.getInteractRangeSquared();
    }

}
