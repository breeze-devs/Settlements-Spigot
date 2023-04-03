package dev.breeze.settlements.entities.villagers.goals.item_toss;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.utils.PersistentUtil;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;

public class TossItemGoal extends Goal {

    /**
     * Snowball removal packet will be sent to players within this distance (squared)
     */
    public static final double MAX_PACKET_REMOVAL_DISTANCE_SQUARED = Math.pow(200, 2); // TODO: put in config

    /**
     * The base number of ticks between two attacks
     */
    private static final int ATTACK_INTERVAL_TICKS = 30;
    /**
     * The "delta" window of the attack interval ticks (i.e. +- half)
     */
    private static final int ATTACK_INTERVAL_TICKS_DELTA = 10;

    /**
     * Villager has this much chance to enter a "scared" state per tick if not already scared
     */
    private static final double SCARED_CHANCE_PER_TICK = 0.2 / 20;

    /**
     * The duration of the "scared" state in ticks
     */
    private static final int MAX_SCARED_TICKS = 5 * 20;

    /**
     * Will only attack when the villager's HP is above this percentage
     */
    private static final double MIN_HEALTH_PERCENTAGE = 0.35;

    @Nonnull
    private final BaseVillager villager;

    @Nonnull
    private final ItemEntry[] itemsAvailable;

    // Computed in the constructor by summing the weights
    private final int maxWeight;

    @Nullable
    private LivingEntity target;

    private int attackTicksLeft;
    private int scaredTicksLeft;
    private final float attackRadius;
    private final float attackRadiusSqr;


    public TossItemGoal(@Nonnull BaseVillager villager, @Nonnull ItemEntry[] itemsAvailable, float range) {
        this.villager = villager;
        this.itemsAvailable = itemsAvailable;

        // Calculate total weight of the items
        this.maxWeight = Arrays.stream(this.itemsAvailable).mapToInt(ItemEntry::weight).sum();

        // Calculate radius-related params
        this.attackRadius = range;
        this.attackRadiusSqr = range * range;

        // Set flags locked by this goal
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));

        // Reset state-based variables
        this.attackTicksLeft = -1;
        this.scaredTicksLeft = -1;
    }

    public boolean canUse() {
        // If no target, stop attacking
        LivingEntity newTarget = this.villager.getTarget();
        if (newTarget == null || !newTarget.isAlive()) {
            return false;
        }

        // If villager is at low HP, stop attacking entirely
        if (this.villager.getHealth() / this.villager.getMaxHealth() < MIN_HEALTH_PERCENTAGE) {
            return false;
        }

        // If the villager is sleeping, stop attacking
        if (this.villager.isSleeping()) {
            return false;
        }

        // If villager is "scared", stop attacking for a few seconds
        if (this.scaredTicksLeft == 0) {
            this.displayAngryParticles();
            this.scaredTicksLeft = -1;
        } else if (this.scaredTicksLeft >= 0) {
            this.displayScaredParticles();
            this.scaredTicksLeft--;
            return false;
        } else if (RandomUtil.RANDOM.nextDouble() < SCARED_CHANCE_PER_TICK) {
            // Random chance of getting scared for a while
            // - set scared time to max scared time
            this.scaredTicksLeft = MAX_SCARED_TICKS;
        }

        this.target = newTarget;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse() || Objects.requireNonNull(this.target).isAlive() && !this.villager.getNavigation().isDone();
    }

    @Override
    public void stop() {
        this.target = null;
        this.attackTicksLeft = -1;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        // Null check, should be unreachable
        if (this.target == null)
            return;

        double distanceSquared = this.villager.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
        boolean hasLineOfSight = this.villager.getSensing().hasLineOfSight(this.target);

        // Stop when target is within attack range
        boolean inRange = distanceSquared <= this.attackRadiusSqr && hasLineOfSight;
        if (inRange) {
            this.villager.getNavigation().stop();
        } else {
            this.villager.getNavigation().moveTo(this.target, 0.6D);
        }

        this.villager.getLookControl().setLookAt(this.target);

        if (inRange && --this.attackTicksLeft == 0) {
            // Execute the attack
            int delta = this.tossItem(this.target, 1D);
            // Reset attack cooldown
            this.attackTicksLeft = Math.max(1, getTicksBeforeNextAttack() + delta);
        } else if (this.attackTicksLeft < 0) {
            this.attackTicksLeft = getTicksBeforeNextAttack();
        }
    }

    /**
     * Toss an item at the target with configurable force
     *
     * @return the attack speed delta for the next attack
     */
    public int tossItem(@Nonnull LivingEntity target, double force) {
        // Determine what item to toss
        ItemStack itemStack = null;
        double damage = 0;
        double knockback = 0;
        int attackSpeedDelta = 0;

        // Weighted random choice
        int targetWeight = RandomUtil.RANDOM.nextInt(this.maxWeight);
        int currentWeight = 0;
        for (ItemEntry itemEntry : this.itemsAvailable) {
            currentWeight += itemEntry.weight();
            if (currentWeight > targetWeight) {
                itemStack = itemEntry.item();
                damage = itemEntry.damage();
                knockback = itemEntry.knockback();
                attackSpeedDelta = itemEntry.attackSpeedDelta();
                break;
            }
        }

        // Null check, but this shouldn't be reached
        if (itemStack == null)
            return 0;

        // Spawn the item entity
        World world = this.villager.level.getWorld();
        Location shootLocation = new Location(world, this.villager.getX(), this.villager.getY() + 1.1, this.villager.getZ());

        Item item = world.dropItem(shootLocation, new ItemStackBuilder(itemStack).setLore(RandomUtil.randomString()).build());
        double x = target.getX() - this.villager.getX();
        double y = target.getEyeY() - item.getLocation().getY();
        double z = target.getZ() - this.villager.getZ();

        // Villager is right on top of the target, ignore
        // - this shouldn't happen unless /summon-ed
        if (x == 0 && y == 0 && z == 0) {
            return 0;
        }

        Vector velocity = new Vector(x, y, z).normalize().add(new Vector(
                this.villager.getRandom().triangle(0.0, 0.0172275 * 12),
                this.villager.getRandom().triangle(0.0, 0.0172275 * 12.0),
                this.villager.getRandom().triangle(0.0, 0.0172275 * 12.0))
        ).multiply(force);

        this.villager.playSound(SoundEvents.SNOW_GOLEM_SHOOT, 1.0F, 0.4F / (RandomUtil.RANDOM.nextFloat() * 0.4F + 0.8F));
        item.setVelocity(velocity);
        item.setTicksLived(6000 - 3 * 20);
        item.setPickupDelay(32767);

        // Spawn invisible snowball for physics
        Snowball snowball = (Snowball) world.spawnEntity(shootLocation, org.bukkit.entity.EntityType.SNOWBALL);
        snowball.setVelocity(velocity.multiply(0.8D));
        snowball.setShooter((ProjectileSource) this.villager.getBukkitEntity());

        TossedItemData data = new TossedItemData(item.getUniqueId().toString(), damage, knockback);
        PersistentUtil.setEntityEntry(snowball, PersistentDataType.STRING, VillagerTossItemEvent.PERSISTENT_KEY, data.toString());

        // Send removal packet to all nearby players
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getLocation().distanceSquared(shootLocation) < MAX_PACKET_REMOVAL_DISTANCE_SQUARED) {
                ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(snowball.getEntityId());
                ((CraftPlayer) online).getHandle().connection.send(packet);
            }
        }
        return attackSpeedDelta;
    }

    private int getTicksBeforeNextAttack() {
        double ticksOffset = (RandomUtil.RANDOM.nextDouble() - 0.5) * ATTACK_INTERVAL_TICKS_DELTA;
        return (int) (ATTACK_INTERVAL_TICKS + ticksOffset);
    }

    private void displayAngryParticles() {
        ParticleUtil.globalParticle(this.villager.getBukkitEntity().getLocation().add(0, 1.8, 0),
                Particle.VILLAGER_ANGRY, 5, 0.2, 0.1, 0.2, 1);
    }

    private void displayScaredParticles() {
        ParticleUtil.globalParticle(this.villager.getBukkitEntity().getLocation().add(0, 1.8, 0),
                Particle.WATER_SPLASH, 3, 0.2, 0.1, 0.2, 1);
    }

    /**
     * Entry for an item that can be tossed by this villager
     *
     * @param weight           the weight of this item vs other items
     * @param item             the actual itemstack to be tossed
     * @param damage           the damage that this itemstack will deal
     * @param knockback        the knockback multiplier that this will inflict
     * @param attackSpeedDelta the attack speed bonus/penalty that will incur after tossing this item
     */
    public record ItemEntry(int weight, ItemStack item, double damage, double knockback, int attackSpeedDelta) {
    }

    public record TossedItemData(String itemEntityUuid, double damage, double knockback) {

        private static final String separator = ";";

        @Nullable
        public static TossedItemData fromString(@Nonnull String datastring) {
            String[] data = datastring.split(separator);
            try {
                return new TossedItemData(data[0], Double.parseDouble(data[1]), Double.parseDouble(data[2]));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public String toString() {
            return this.itemEntityUuid + ";" + this.damage + ";" + this.knockback;
        }

    }


}
