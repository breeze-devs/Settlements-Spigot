package dev.breeze.settlements.entities.villagers.behaviors.habitat.desert;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.behaviors.BaseVillagerBehavior;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.utils.*;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.itemstack.PotionItemStackBuilder;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import javax.annotation.Nonnull;
import java.util.Map;

public final class DrinkWaterBehavior extends BaseVillagerBehavior {

    private static final ItemStack WATER_BOTTLE = CraftItemStack.asNMSCopy(
            new PotionItemStackBuilder(PotionItemStackBuilder.PotionType.NORMAL)
                    .setBasePotionEffect(new PotionData(PotionType.WATER))
                    .build());

    private static final int SCAN_COOLDOWN = TimeUtil.minutes(1);
    public static final int TICK_INTERVAL = TimeUtil.ticks(5);

    private static final int MIN_OVERHEAT_DURATION = TimeUtil.seconds(5);
    private static final int MAX_OVERHEAT_DURATION = TimeUtil.seconds(10);
    private static final int DRINK_DURATION = TimeUtil.seconds(1);

    public static final int MIN_COOLDOWN = TimeUtil.minutes(2);
    public static final int MAX_COOLDOWN = TimeUtil.minutes(8);

    private int cooldown;
    private int overheatTimeLeft;
    private int drinkTimeLeft;

    public DrinkWaterBehavior() {
        super(Map.of(
                VillagerMemoryType.CURRENT_HABITAT.getMemoryModuleType(), MemoryStatus.VALUE_PRESENT
        ), MAX_OVERHEAT_DURATION + DRINK_DURATION, SCAN_COOLDOWN);

        this.cooldown = this.randomCooldown();
        this.overheatTimeLeft = 0;
        this.drinkTimeLeft = 0;
    }

    @Override
    protected boolean checkExtraStartConditionsRateLimited(@Nonnull ServerLevel level, @Nonnull BaseVillager baseVillager) {
        // Not -1 because this method is rate limited
        this.cooldown -= this.getMaxStartConditionCheckCooldown();
        DebugUtil.broadcastEntity("&7Cooldown for %s is %s".formatted(this.getClass().getSimpleName(), TimeUtil.ticksToReadableTime(Math.max(0,
                this.cooldown))), baseVillager.getStringUUID(), baseVillager.getHoverDescription());
        if (this.cooldown > 0) {
            return false;
        }

        // Check if it's a hot habitat
        Habitat habitat = VillagerMemoryType.CURRENT_HABITAT.get(baseVillager.getBrain());
        return habitat != null && habitat.isHot();
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.start(level, villager, gameTime);

        this.overheatTimeLeft = RandomUtil.RANDOM.nextInt(MIN_OVERHEAT_DURATION, MAX_OVERHEAT_DURATION);
        this.drinkTimeLeft = DRINK_DURATION;
    }

    @Override
    protected void tick(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        // Decrement cooldowns
        if (--this.overheatTimeLeft < 0) {
            this.drinkTimeLeft--;
        }

        if (gameTime % TICK_INTERVAL != 0 || !(villager instanceof BaseVillager baseVillager)) {
            return;
        }

        Location location = new Location(level.getWorld(), villager.getX(), villager.getEyeY(), villager.getZ());

        // Display sweat particles
        ParticleUtil.globalParticle(location, Particle.WATER_SPLASH, 3, 0.1, 0.1, 0.1, 0.1);
        ParticleUtil.coloredPotion(location, 255, 162, 51);

        if (this.overheatTimeLeft < 0 && this.drinkTimeLeft >= 0) {
            // Display drinking effect
            baseVillager.setHeldItem(WATER_BOTTLE);
            SoundUtil.playSoundPublic(location, Sound.ENTITY_GENERIC_DRINK, 0.03F, 1.2f);
        }
    }

    @Override
    protected boolean canStillUse(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        return this.overheatTimeLeft > 0 || this.drinkTimeLeft > 0;
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.stop(level, villager, gameTime);

        // Reset variables
        this.cooldown = this.randomCooldown();
        this.overheatTimeLeft = 0;
        this.drinkTimeLeft = 0;

        // Clear held item
        if (villager instanceof BaseVillager baseVillager) {
            baseVillager.clearHeldItem();
        }
    }

    @Override
    public int getCurrentCooldown() {
        return this.cooldown;
    }

    @Nonnull
    @Override
    public ItemStackBuilder getGuiItemBuilderAbstract() {
        return new ItemStackBuilder(WATER_BOTTLE)
                .setDisplayName("&eDrink water behavior")
                .setLore("&fOccasionally it gets too hot for the villager", "&fRemember to &bhydrate &fyourself as well~");
    }

    private int randomCooldown() {
        return RandomUtil.RANDOM.nextInt(MIN_COOLDOWN, MAX_COOLDOWN);
    }

}

