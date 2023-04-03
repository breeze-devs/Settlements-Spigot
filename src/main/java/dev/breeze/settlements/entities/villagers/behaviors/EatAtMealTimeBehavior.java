package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.utils.MessageUtil;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.SoundUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public final class EatAtMealTimeBehavior extends BaseVillagerBehavior {

    private static final ItemStack BREAD = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.BREAD).build());
    private static final ItemStack WATER_BOTTLE = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.POTION).build());

    private static final int MIN_EAT_DURATION = TimeUtil.seconds(1);
    private static final int MAX_EAT_DURATION = TimeUtil.seconds(3);
    private static final int MIN_DRINK_DURATION = TimeUtil.seconds(1);
    private static final int MAX_DRINK_DURATION = TimeUtil.seconds(2);

    // Not a long time because the CAN_EAT memory will only be TRUE at meal times
    // - and that cooldowns will not tick if in non-meal times
    // - this cooldown makes sure that the villager only eats once during meal time
    public static final int MIN_EAT_COOLDOWN = TimeUtil.seconds(30);
    public static final int MAX_EAT_COOLDOWN = TimeUtil.minutes(1);

    // How frequent to display eating effects
    public static final int EAT_EFFECT_COOLDOWN = TimeUtil.ticks(5);

    private int cooldown;
    private int eatTimeLeft;
    private int drinkTimeLeft;

    @Nullable
    private ItemStack foodToEat;

    public EatAtMealTimeBehavior() {
        super(Map.of(
                // Only run in meal times
                VillagerMemoryType.IS_MEAL_TIME, MemoryStatus.VALUE_PRESENT
        ), MAX_EAT_DURATION + MAX_DRINK_DURATION);

        this.cooldown = this.randomCooldown();
        this.eatTimeLeft = 0;
        this.drinkTimeLeft = 0;

        this.foodToEat = null;
    }

    @Override
    protected boolean checkExtraStartConditionsRateLimited(@Nonnull ServerLevel level, @Nonnull Villager villager) {
        // Not -1 because this method is rate limited
        this.cooldown -= this.getMaxStartConditionCheckCooldown();
        MessageUtil.debug("&a[Debug] Cooldown for " + this.getClass().getSimpleName() + " is " + this.cooldown);
        return this.cooldown <= 0;
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.start(level, villager, gameTime);

        this.eatTimeLeft = RandomUtil.RANDOM.nextInt(MIN_EAT_DURATION, MAX_EAT_DURATION) / EAT_EFFECT_COOLDOWN;
        this.drinkTimeLeft = RandomUtil.RANDOM.nextInt(MIN_DRINK_DURATION, MAX_DRINK_DURATION) / EAT_EFFECT_COOLDOWN;
        // TODO: randomize food item
        this.foodToEat = BREAD;

        // Set held item
        if (villager instanceof BaseVillager baseVillager)
            baseVillager.setHeldItem(this.foodToEat);
    }

    @Override
    protected void tick(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        if (gameTime % EAT_EFFECT_COOLDOWN != 0 || !(villager instanceof BaseVillager baseVillager))
            return;

        Location handLocation = new Location(level.getWorld(), villager.getX(), villager.getY() + 1.2, villager.getZ());
        if (--this.eatTimeLeft >= 0) {
            // Display eating effect
            if (this.foodToEat != null) {
                baseVillager.setHeldItem(this.foodToEat);
                ParticleUtil.itemBreak(handLocation, CraftItemStack.asBukkitCopy(this.foodToEat), 1, 0.1, 0.1, 0.1, 0.02);
            }

            SoundUtil.playSoundPublic(handLocation, Sound.ENTITY_GENERIC_EAT, 0.05F, 1.2f);
        } else if (--this.drinkTimeLeft >= 0) {
            // Display drinking effect
            baseVillager.setHeldItem(WATER_BOTTLE);
            ParticleUtil.globalParticle(handLocation, Particle.WATER_SPLASH, 1, 0.1, 0.1, 0.1, 0.05);
            SoundUtil.playSoundPublic(handLocation, Sound.ENTITY_GENERIC_DRINK, 0.05F, 1.2f);
        }
    }

    @Override
    protected boolean canStillUse(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        return this.eatTimeLeft > 0 || this.drinkTimeLeft > 0;
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.stop(level, villager, gameTime);

        // Display effects
        Location location = new Location(level.getWorld(), villager.getX(), villager.getEyeY(), villager.getZ());
        ParticleUtil.globalParticle(location, Particle.VILLAGER_HAPPY, 5, 0.2, 0.2, 0.2, 0.1);
        SoundUtil.playSoundPublic(location, Sound.ENTITY_PLAYER_BURP, 0.05F, 1.2f);
        SoundUtil.playSoundPublic(location, Sound.ENTITY_VILLAGER_YES, 0.05F, 1f);

        // Heal by 1 heart
        villager.heal(2, EntityRegainHealthEvent.RegainReason.EATING);

        // Reset variables
        this.cooldown = this.randomCooldown();
        this.foodToEat = null;

        // Clear held item
        if (villager instanceof BaseVillager baseVillager)
            baseVillager.clearHeldItem();
    }

    private int randomCooldown() {
        return RandomUtil.RANDOM.nextInt(MIN_EAT_COOLDOWN, MAX_EAT_COOLDOWN);
    }

}

