package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.SoundUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FeedWolfBehavior extends InteractAtTargetBehavior {

    /**
     * Feedable by butchers level 2 or lower
     * - i.e. novice or apprentice
     */
    private static final ItemStack[] FEEDABLE_ITEMS_RAW = new ItemStack[]{
            CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.CHICKEN).build()),
            CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.PORKCHOP).build()),
            CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.BEEF).build()),
            CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.RABBIT).build()),
            CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.MUTTON).build()),
    };

    /**
     * Feedable by butchers level 3 or higher
     * - i.e. journeyman, expert, or master
     */
    private static final ItemStack[] FEEDABLE_ITEMS_COOKED = new ItemStack[]{
            CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.COOKED_CHICKEN).build()),
            CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.COOKED_PORKCHOP).build()),
            CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.COOKED_BEEF).build()),
            CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.COOKED_RABBIT).build()),
            CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.COOKED_MUTTON).build()),
    };

    @Nullable
    private Wolf targetWolf;
    @Nullable
    private ItemStack heldItem;

    public FeedWolfBehavior() {
        // Preconditions to this behavior
        super(Map.of(
                        // There should be living entities nearby
                        MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
                ), TimeUtil.seconds(10), Math.pow(20, 2),
                TimeUtil.seconds(30), Math.pow(1.5, 2),
                5, 1,
                TimeUtil.seconds(20), 1);

        this.targetWolf = null;
    }

    @Override
    protected boolean scan(ServerLevel level, Villager villager) {
        Brain<Villager> brain = villager.getBrain();

        // If there are no nearby living entities, ignore
        if (brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).isEmpty())
            return false;

        if (this.targetWolf == null) {
            // Check for nearby iron golems
            List<LivingEntity> target = brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).get();
            Optional<LivingEntity> nearestWolf = target.stream().filter(e -> e.getType() == EntityType.WOLF && this.canBeFed(((Wolf) e))).findFirst();

            // If no nearby iron golems, ignore
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
        return this.canBeFed(this.targetWolf);
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.start(level, villager, gameTime);
        if (villager.getVillagerData().getLevel() < 3) {
            this.heldItem = RandomUtil.choice(FEEDABLE_ITEMS_RAW);
        } else {
            this.heldItem = RandomUtil.choice(FEEDABLE_ITEMS_COOKED);
        }
    }

    @Override
    protected void tickExtra(ServerLevel level, Villager villager, long gameTime) {
        if (this.heldItem == null)
            return;

        if (villager instanceof BaseVillager baseVillager)
            baseVillager.setHeldItem(this.heldItem);

        if (this.targetWolf != null)
            villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.targetWolf, true));
    }

    @Override
    protected void navigateToTarget(ServerLevel level, Villager villager, long gameTime) {
        if (this.targetWolf == null)
            return;
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetWolf, 0.5F, 1));
    }

    @Override
    protected void interactWithTarget(ServerLevel level, Villager villager, long gameTime) {
        // Safety check
        if (this.targetWolf == null || this.heldItem == null)
            return;

        FoodProperties foodProperties = this.heldItem.getItem().getFoodProperties();
        int healAmount = foodProperties == null ? 1 : foodProperties.getNutrition();
        this.targetWolf.heal(healAmount, EntityRegainHealthEvent.RegainReason.EATING);

        // Display effects
        Location location = new Location(level.getWorld(), this.targetWolf.getX(), this.targetWolf.getY() + 0.3, this.targetWolf.getZ());
        ParticleUtil.itemBreak(location, CraftItemStack.asBukkitCopy(this.heldItem), 25, 0.2, 0.2, 0.2, 0.1);
        SoundUtil.playSoundPublic(location, Sound.ENTITY_GENERIC_EAT, 1.2f);

        // Stop after feeding once
        this.doStop(level, villager, gameTime);
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.stop(level, villager, gameTime);

        // Reset held item
        if (villager instanceof BaseVillager baseVillager)
            baseVillager.clearHeldItem();

        // Remove golem from interaction memory
        villager.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);

        // Reset variables
        this.targetWolf = null;
        this.heldItem = null;
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
        return new ItemStackBuilder(Material.COOKED_BEEF)
                .setDisplayName("&eFeed tamed wolves behavior")
                .setLore(
                        "&7Fed wolves will regenerate some health depending on the food used",
                        "&7Villager with at least &ejourneyman &7expertise will feed using higher quality food"
                );
    }

    private boolean canBeFed(@Nonnull Wolf wolf) {
        return wolf.isTame() && wolf.getHealth() < wolf.getMaxHealth();
    }

}
