package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.Main;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.utils.*;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.particle.ParticlePreset;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RepairIronGolemBehavior extends InteractAtTargetBehavior {

    private static final ItemStack IRON_INGOT = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.IRON_INGOT).build());

    /**
     * The amount of HP to heal per repair action
     * - 1-5: novice, apprentice, journeyman, expert, master
     */
    private static final Map<Integer, Float> REPAIR_AMOUNT_MAP = Map.of(
            1, 5F,
            2, 7F,
            3, 9F,
            4, 12F,
            5, 15F
    );

    /**
     * Golems above this HP% will not be considered as a repair target
     */
    private static final double REPAIR_WHEN_BELOW_HP_PERCENTAGE = 0.999;

    @Nullable
    private IronGolem targetGolem;

    public RepairIronGolemBehavior() {
        // Preconditions to this behavior
        super(Map.of(
                        // There should be iron golems detected recently to run this behavior
                        MemoryModuleType.GOLEM_DETECTED_RECENTLY, MemoryStatus.VALUE_PRESENT,
                        // There should be living entities nearby
                        MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
                ), TimeUtil.seconds(20), Math.pow(30, 2),
                TimeUtil.minutes(2), Math.pow(2, 2),
                5, TimeUtil.seconds(2),
                TimeUtil.seconds(20), TimeUtil.seconds(10));

        this.targetGolem = null;
    }

    /**
     * Scans for nearby golems to repair
     */
    @Override
    protected boolean scan(ServerLevel level, Villager villager) {
        Brain<Villager> brain = villager.getBrain();

        // If there are no nearby living entities, ignore
        if (brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).isEmpty()) {
            return false;
        }

        if (this.targetGolem == null) {
            // Check for nearby iron golems
            List<LivingEntity> target = brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).get();
            Optional<LivingEntity> nearestGolem = target.stream().filter(e -> e.getType() == EntityType.IRON_GOLEM && this.needHealing(e)).findFirst();

            // If no nearby iron golems, ignore
            if (nearestGolem.isEmpty()) {
                return false;
            }

            IronGolem golem = (IronGolem) nearestGolem.get();

            // If golem is too far away, ignore
            if (villager.distanceToSqr(golem) > this.getScanRangeSquared()) {
                return false;
            }

            this.targetGolem = golem;
            villager.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, nearestGolem.get());
        }
        return true;
    }

    @Override
    protected boolean checkExtraCanStillUseConditions(ServerLevel level, Villager villager, long gameTime) {
        if (this.targetGolem == null || !this.targetGolem.isAlive()) {
            return false;
        }
        return this.needHealing(this.targetGolem);
    }

    @Override
    protected void tickExtra(ServerLevel level, Villager villager, long gameTime) {
        if (villager instanceof BaseVillager baseVillager) {
            baseVillager.setHeldItem(IRON_INGOT);
        }
        if (this.targetGolem != null) {
            villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.targetGolem, true));
        }
    }

    @Override
    protected void navigateToTarget(ServerLevel level, Villager villager, long gameTime) {
        if (this.targetGolem == null) {
            return;
        }
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetGolem, 0.5F, 1));
    }

    @Override
    protected void interactWithTarget(ServerLevel level, Villager villager, long gameTime) {
        // Safety check
        if (this.targetGolem == null) {
            return;
        }

        // TODO: have the golem look at the villager
//        this.targetGolem.lookAt(self, 30f, 30f);
//        this.targetGolem.getLookControl().setLookAt(self);

        // Heal golem
        this.targetGolem.heal(REPAIR_AMOUNT_MAP.getOrDefault(villager.getVillagerData().getLevel(), 5F), EntityRegainHealthEvent.RegainReason.CUSTOM);

        // TODO: random chance to offer flower?
//        if (RandomUtil.RANDOM.nextDouble() < 0.2)
//            this.targetGolem.offerFlower(true);

        // Display effects
        Location golemLocation = new Location(level.getWorld(), this.targetGolem.getX(), this.targetGolem.getY() + 1.2, this.targetGolem.getZ());
        ParticleUtil.globalParticle(golemLocation, Particle.WAX_OFF, 25, 0.4, 0.6, 0.4, 1);
        SoundUtil.playSoundPublic(golemLocation, Sound.ENTITY_VILLAGER_WORK_TOOLSMITH, 0.7f);
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.stop(level, villager, gameTime);
        if (!(villager instanceof BaseVillager baseVillager)) {
            return;
        }

        // Apply repair effects
        this.addExtraRepairBuffs(baseVillager);

        // Reset states
        baseVillager.clearHeldItem();
        baseVillager.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);

        this.targetGolem = null;
    }

    @Override
    protected boolean hasTarget() {
        return this.targetGolem != null;
    }

    @Override
    protected boolean isTargetReachable(Villager villager) {
        return this.targetGolem != null && villager.distanceToSqr(this.targetGolem) < this.getInteractRangeSquared();
    }

    @Nonnull
    @Override
    public ItemStackBuilder getGuiItemBuilderAbstract() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Occasionally repairs nearby injured iron golems");
        lore.add("&7Health regenerated depends on expertise:");
        for (int key : REPAIR_AMOUNT_MAP.keySet().stream().sorted().toList()) {
            lore.add("&7- %s&7: %d HP per ingot".formatted(VillagerUtil.getExpertiseName(key, true), Math.round(REPAIR_AMOUNT_MAP.get(key))));
        }
        lore.add("&7Applies extra buffs based on profession");

        return new ItemStackBuilder(Material.IRON_INGOT)
                .setDisplayName("&eRepair iron golem behavior")
                .setLore(lore);
    }

    private boolean needHealing(@Nonnull LivingEntity entity) {
        return entity.getHealth() < entity.getMaxHealth() * REPAIR_WHEN_BELOW_HP_PERCENTAGE;
    }

    private void addExtraRepairBuffs(@Nonnull BaseVillager baseVillager) {
        // Safety check
        if (this.targetGolem == null) {
            return;
        }

        Location golemLocation = LocationUtil.fromNmsEntity(this.targetGolem);

        // Apply buffs based on profession
        if (baseVillager.getProfession() == VillagerProfession.WEAPONSMITH) {
            this.targetGolem.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, TimeUtil.seconds(40), 0));

            // Play effects
            SoundUtil.playSoundPublic(golemLocation, Sound.ENTITY_IRON_GOLEM_DEATH, 0.3F, 0F);
            ParticleUtil.globalParticle(golemLocation, Particle.LAVA, 15, 0.6, 1, 0.6, 1);
        } else if (baseVillager.getProfession() == VillagerProfession.ARMORER) {
            this.targetGolem.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, TimeUtil.minutes(2), 0));
            this.targetGolem.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, TimeUtil.seconds(15), 0));

            // Play effects
            SoundUtil.playSoundPublic(golemLocation, Sound.BLOCK_ANVIL_USE, 0.3F, 0F);

            double delta = 1.5;
            displayShield(golemLocation.clone().add(0, 1.5, delta), 0, 20);
            displayShield(golemLocation.clone().add(0, 1.5, -delta), 180, 20);
        } else if (baseVillager.getProfession() == VillagerProfession.TOOLSMITH) {
            this.targetGolem.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, TimeUtil.minutes(1), 1));

            // Play effects
            SoundUtil.playSoundPublic(golemLocation, Sound.BLOCK_CONDUIT_ACTIVATE, 0.3F, 0F);
            ParticleUtil.globalParticle(LocationUtil.fromNmsEntity(this.targetGolem), Particle.EXPLOSION_NORMAL, 10, 0.2, 0.3, 0.2, 0.2);
            final int intervalTicks = TimeUtil.seconds(1);
            final IronGolem ironGolem = this.targetGolem;
            new SafeRunnable() {
                int elapsed = 0;

                @Override
                public void safeRun() {
                    if (elapsed > TimeUtil.minutes(1) || !ironGolem.isAlive() || !ironGolem.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                        this.cancel();
                        return;
                    }
                    ParticleUtil.globalParticle(LocationUtil.fromNmsEntity(ironGolem), Particle.EXPLOSION_NORMAL, 1, 0.1, 0.1, 0.1, 0.1);
                    elapsed += intervalTicks;
                }
            }.runTaskTimer(Main.getPlugin(), 0L, intervalTicks);
        }
    }

    private static void displayShield(Location center, double yaw, double scalingFactor) {
        double[] xOffsets = new double[]{0, 1, 2, 3, 4, 5, 6, 6, 7, 7, 7, 8, 8, 8, 8, 9, 9, 9, 9, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
        double[] yOffsets = new double[]{-12.5, -12, -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 5, 5, 6, 6, 7, 7, 8, 8, 9};
        for (int a = 0; a < xOffsets.length; a++) {
            Location loc1 = center.clone().add(xOffsets[a] / scalingFactor, yOffsets[a] / scalingFactor, 0);
            Location loc2 = center.clone().add(-xOffsets[a] / scalingFactor, yOffsets[a] / scalingFactor, 0);
            ArrayList<Location> rLocs = LocationUtil.rotateLocations(center, List.of(loc1, loc2), yaw, 0, 0, 1);
            ParticlePreset.displayLine(rLocs.get(0), rLocs.get(1), 2, Particle.SMOKE_NORMAL, 1, 0, 0, 0, 0);
        }
    }

}
