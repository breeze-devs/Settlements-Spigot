package dev.breeze.settlements.debug;

import dev.breeze.settlements.Main;
import dev.breeze.settlements.debug.guis.villager.VillagerDebugMainGui;
import dev.breeze.settlements.entities.cats.VillagerCat;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.entities.wolves.memories.WolfMemoryType;
import dev.breeze.settlements.entities.wolves.sensors.WolfFenceAreaSensor;
import dev.breeze.settlements.utils.MessageUtil;
import dev.breeze.settlements.utils.RayTraceUtil;
import dev.breeze.settlements.utils.SoundPresets;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.particle.ParticlePreset;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;

public class DebugStickEvent implements Listener {

    /**
     * Event handler for right-clicking with the debug stick
     */
    @EventHandler
    public void onClickEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getHand());
        if (item.getType() != Material.DEBUG_STICK || !item.hasItemMeta()) {
            return;
        }

        event.setCancelled(true);
        debug(player, ((CraftEntity) event.getRightClicked()).getHandle());
    }


    /**
     * Event handler for right-clicking air with debug stick (ray trace)
     */
    @EventHandler
    public void onClickAir(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.DEBUG_STICK || !item.hasItemMeta()) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();

        // Ray trace the entity
        int maxRayTraceDistance = 60;
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        RayTraceUtil.RayTraceResult rayTraceResult = RayTraceUtil.getTargetEntity(serverPlayer, maxRayTraceDistance);

        // We found no targets
        if (rayTraceResult == null) {
            // Display not found particles
            Location target = player.getEyeLocation().add(player.getLocation().getDirection().normalize().multiply(maxRayTraceDistance));
            ParticlePreset.displayLinePrivate(player, player.getEyeLocation(), target, maxRayTraceDistance, Particle.END_ROD, 1, 0, 0, 0, 0);
            ParticleUtil.playerParticle(player, target, Particle.END_ROD, 15, 0.1, 0.1, 0.1, 1);
            return;
        }

        // Display particles to the entity
        Entity entity = rayTraceResult.entity();
        Location target = new Location(player.getWorld(), entity.getX(), entity.getY(), entity.getZ());
        ParticlePreset.displayLinePrivate(player, player.getEyeLocation(), target, 15, Particle.END_ROD, 1, 0, 0, 0, 0);

        // Debug
        debug(player, entity);
    }

    private void debug(@Nonnull Player player, @Nonnull Entity entity) {
        World world = player.getWorld();
        Location location = new Location(world, entity.getX(), entity.getY(), entity.getZ());

        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.GLOWING, TimeUtil.seconds(1), 0, false, false));
        }

        if (entity instanceof VillagerWolf wolf) {
            Brain<Wolf> brain = wolf.getBrain();
            Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories = brain.getMemories();
            MessageUtil.sendMessageWithoutPrefix(player, "&bMemories of wolf:");
            for (Map.Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> entry : memories.entrySet())
                MessageUtil.sendMessageWithoutPrefix(player, "&b - %s : %s", entry.getKey().toString(), entry.getValue().toString());
            MessageUtil.sendMessageWithoutPrefix(player, "&b - Owned by: %s (%s)", wolf.getOwner(), wolf.getOwnerUUID());

            // Display fence area visually
            if (brain.hasMemoryValue(WolfMemoryType.NEAREST_FENCE_AREA)) {
                // Display fence positions
                WolfFenceAreaSensor.FenceArea fenceArea = brain.getMemory(WolfMemoryType.NEAREST_FENCE_AREA).get();
                for (BlockPos pos : fenceArea.getFences()) {
                    Location fence = new Location(world, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                    ParticleUtil.globalParticle(fence, Particle.SPELL_WITCH, 1, 0, 0, 0, 0);
                }

                // Display gate position
                BlockPos gate = fenceArea.getGatePosition();
                Location gateLoc = new Location(world, gate.getX() + 0.5, gate.getY() + 1, gate.getZ() + 0.5);
                ParticleUtil.globalParticle(gateLoc, Particle.VILLAGER_HAPPY, 20, 0.5, 0.5, 0.5, 0);

                // Draw line from the wolf to the gate
                ParticlePreset.displayLine(location, gateLoc, 20, Particle.END_ROD, 1, 0, 0, 0, 0);
            }

            // Draw line to owner
            if (wolf.getOwner() != null && wolf.getOwner().isAlive()) {
                ParticlePreset.displayLine(location, wolf.getOwner().getBukkitEntity().getLocation(), 20, Particle.END_ROD, 1, 0, 0, 0, 0);
                wolf.getOwner().addEffect(new MobEffectInstance(MobEffects.GLOWING, TimeUtil.seconds(5), 0, false, false));
            }
        } else if (entity instanceof VillagerCat cat) {
            Brain<Cat> brain = cat.getBrain();
            Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories = brain.getMemories();
            MessageUtil.sendMessageWithoutPrefix(player, "&bMemories of cat:");
            for (Map.Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> entry : memories.entrySet())
                MessageUtil.sendMessageWithoutPrefix(player, "&b - %s : %s", entry.getKey().toString(), entry.getValue().toString());
            MessageUtil.sendMessageWithoutPrefix(player, "&b - Owned by: %s (%s)", cat.getOwner(), cat.getOwnerUUID());

            // Draw line to owner
            if (cat.getOwner() != null && cat.getOwner().isAlive()) {
                ParticlePreset.displayLine(location, cat.getOwner().getBukkitEntity().getLocation(), 20, Particle.END_ROD, 1, 0, 0, 0, 0);
                cat.getOwner().addEffect(new MobEffectInstance(MobEffects.GLOWING, TimeUtil.seconds(5), 0, false, false));
            }
        } else if (entity instanceof BaseVillager villager) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), () -> {
                player.closeInventory();
                player.openInventory(VillagerDebugMainGui.getViewableInventory(player, villager).getBukkitInventory());
                SoundPresets.inventoryOpen(player);
            }, TimeUtil.ticks(5));
        }
    }

}
