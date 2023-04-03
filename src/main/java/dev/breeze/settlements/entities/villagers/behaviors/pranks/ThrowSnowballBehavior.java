package dev.breeze.settlements.entities.villagers.behaviors.pranks;

import dev.breeze.settlements.Main;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.SafeRunnable;
import dev.breeze.settlements.utils.SoundUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Snowball;
import org.bukkit.util.Vector;

public final class ThrowSnowballBehavior extends PrankEntityBehavior {

    private static final ItemStack SNOWBALL = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.SNOWBALL).build());

    public ThrowSnowballBehavior() {
        // Preconditions to this behavior
        super(TimeUtil.minutes(3), Math.pow(20, 2), TimeUtil.seconds(3));
    }

    @Override
    protected void performAnnoy(ServerLevel level, Villager villager, long gameTime, World world, Location selfLoc, Location targetLoc) {
        final long period = 7;
        final LivingEntity annoyTarget = this.getAnnoyTarget();
        new SafeRunnable() {
            private int elapsed = 0;

            @Override
            public void safeRun() {
                if (this.elapsed > TimeUtil.seconds(2) || annoyTarget == null || !annoyTarget.isAlive()) {
                    if (villager instanceof BaseVillager baseVillager)
                        baseVillager.clearHeldItem();
                    this.cancel();
                    return;
                }

                Location shootLocation = new Location(world, villager.getX(), villager.getEyeY(), villager.getZ());
                Location targetLocation = new Location(world, annoyTarget.getX(), annoyTarget.getEyeY(), annoyTarget.getZ());
                Vector velocity = targetLocation.toVector().subtract(shootLocation.toVector())
                        .normalize()
                        .add(randomOffset())
                        .setY(0.1 + RandomUtil.RANDOM.nextDouble() / 3);

                Snowball snowball = (Snowball) world.spawnEntity(shootLocation, EntityType.SNOWBALL);
                snowball.setVelocity(velocity);

                SoundUtil.playSoundPublic(shootLocation, Sound.ENTITY_SNOWBALL_THROW, 0.1F, 1.1F);
                villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(annoyTarget, true));

                this.elapsed += period;
            }
        }.runTaskTimer(Main.getPlugin(), 0L, period);
    }

    @Override
    protected ItemStack getItemToHold() {
        return SNOWBALL;
    }

    private Vector randomOffset() {
        double factor = 1.5;
        return new Vector(
                (RandomUtil.RANDOM.nextDouble() - 0.5) / factor,
                (RandomUtil.RANDOM.nextDouble() - 0.5) / factor,
                (RandomUtil.RANDOM.nextDouble() - 0.5) / factor
        );
    }

}
