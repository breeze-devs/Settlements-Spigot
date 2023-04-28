package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.ReputationLevels;
import dev.breeze.settlements.utils.SoundUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.itemstack.PotionItemStackBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public final class ThrowHealingPotionBehavior extends InteractAtTargetBehavior {

    private static final ItemStack POTION_REGEN = CraftItemStack.asNMSCopy(new PotionItemStackBuilder(PotionItemStackBuilder.PotionType.SPLASH)
            .addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, TimeUtil.seconds(10), 0), true)
            .build());

    private static final Map<Integer, List<PotionEffect>> EXPERTISE_POTION_MAP = Map.of(
            1, List.of(new PotionEffect(PotionEffectType.HEAL, 1, 0)),
            2, List.of(new PotionEffect(PotionEffectType.HEAL, 1, 1)),
            3, List.of(new PotionEffect(PotionEffectType.HEAL, 1, 1), new PotionEffect(PotionEffectType.ABSORPTION, TimeUtil.minutes(1), 0)),
            4, List.of(new PotionEffect(PotionEffectType.HEAL, 1, 1), new PotionEffect(PotionEffectType.ABSORPTION, TimeUtil.minutes(1), 1)),
            5, List.of(new PotionEffect(PotionEffectType.HEAL, 1, 1), new PotionEffect(PotionEffectType.ABSORPTION, TimeUtil.minutes(1), 2))
    );

    private static final double HEAL_WHEN_BELOW_HP_PERCENTAGE = 0.8;

    @Nullable
    private LivingEntity target;

    public ThrowHealingPotionBehavior() {
        // Preconditions to this behavior
        super(Map.of(
                        // There should be living entities nearby
                        MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
                ), TimeUtil.seconds(20), Math.pow(20, 2),
                TimeUtil.minutes(2), Math.pow(5, 2),
                5, 1,
                TimeUtil.seconds(20), TimeUtil.seconds(1));

        this.target = null;
    }

    @Override
    protected boolean scan(ServerLevel level, Villager villager) {
        Brain<Villager> brain = villager.getBrain();

        // If there are no nearby living entities, ignore
        if (!brain.hasMemoryValue(MemoryModuleType.NEAREST_LIVING_ENTITIES))
            return false;

        // Check for nearby hurt villagers or reputable players
        List<LivingEntity> nearbyEntities = villager.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).get();

        boolean matchFound = false;
        for (LivingEntity entity : nearbyEntities) {
            if (entity == null || !entity.isAlive())
                continue;

            boolean isVillageRelatedEntities = entity instanceof Villager || entity instanceof Player || entity instanceof WanderingTrader;
            if (!isVillageRelatedEntities)
                continue;

            // Ignore entities with more than X% of health
            if (entity.getHealth() / entity.getMaxHealth() >= HEAL_WHEN_BELOW_HP_PERCENTAGE)
                continue;

            // Check player reputation
            if (entity instanceof Player player && !ReputationLevels.isComrade(villager.getPlayerReputation(player)))
                continue;

            // Successfully found an animal to butcher
            this.target = entity;
            matchFound = true;
            break;
        }

        return matchFound;
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.start(level, villager, gameTime);
    }

    @Override
    protected boolean checkExtraCanStillUseConditions(ServerLevel level, Villager villager, long gameTime) {
        return this.target != null && this.target.isAlive();
    }

    @Override
    protected void tickExtra(ServerLevel level, Villager villager, long gameTime) {
        if (this.target == null)
            return;

        villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.target, true));

        if (villager instanceof BaseVillager baseVillager)
            baseVillager.setHeldItem(POTION_REGEN);
    }

    @Override
    protected void navigateToTarget(ServerLevel level, Villager villager, long gameTime) {
        if (this.target != null)
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.target, 0.45F, 3));
    }

    @Override
    protected void interactWithTarget(ServerLevel level, Villager villager, long gameTime) {
        // Safety check
        if (this.target == null || !(villager instanceof BaseVillager baseVillager))
            return;

        // Throw a potion
        Location eyeLocation = new Location(level.getWorld(), villager.getX(), villager.getEyeY(), villager.getZ());
        ThrownPotion potion = (ThrownPotion) level.getWorld().spawnEntity(eyeLocation, EntityType.SPLASH_POTION);

        PotionItemStackBuilder builder = new PotionItemStackBuilder(PotionItemStackBuilder.PotionType.SPLASH);
        for (PotionEffect effect : EXPERTISE_POTION_MAP.get(baseVillager.getExpertiseLevel())) {
            builder.addPotionEffect(effect, true);
        }
        potion.setItem(builder.build());

        // Set potion velocity direction
        Vector vector = this.target.getBukkitEntity().getLocation().toVector().subtract(villager.getBukkitEntity().getLocation().toVector());
        potion.setVelocity(vector.normalize().multiply(0.4).setY(0.25));

        // Play throw sound
        SoundUtil.playSoundPublic(eyeLocation, Sound.ENTITY_SPLASH_POTION_THROW, 0.2F, 0.6F);

        // Stop behavior
        this.doStop(level, villager, gameTime);
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.stop(level, villager, gameTime);

        if (villager instanceof BaseVillager baseVillager)
            baseVillager.clearHeldItem();

        // Reset variables
        this.target = null;
    }

    @Override
    protected boolean hasTarget() {
        return this.target != null;
    }

    @Override
    protected boolean isTargetReachable(Villager villager) {
        return this.target != null && villager.distanceToSqr(this.target) < this.getInteractRangeSquared();
    }

    @Nonnull
    @Override
    public ItemStackBuilder getGuiItemBuilderAbstract() {
        return new ItemStackBuilder(Material.GLISTERING_MELON_SLICE)
                .setDisplayName("&eThrow healing potion behavior")
                .setLore(
                        "&7Occasionally throws a healing potion at injured villagers",
                        "&7Villagers with higher expertise can brew more potent potions"
                );
    }

}
