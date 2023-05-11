package dev.breeze.settlements.debug;

import dev.breeze.settlements.Main;
import dev.breeze.settlements.debug.guis.villager.VillagerDebugMainGui;
import dev.breeze.settlements.entities.cats.VillagerCat;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.entities.wolves.memories.WolfMemoryType;
import dev.breeze.settlements.entities.wolves.sensors.WolfFenceAreaSensor;
import dev.breeze.settlements.utils.MessageUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.particle.ParticlePreset;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;

public class MemoryEvent implements Listener {

    @EventHandler
    public void onClickEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        ItemStack item = player.getInventory().getItem(event.getHand());

        if (item.getType() != Material.DEBUG_STICK || !item.hasItemMeta())
            return;
        Entity entity = ((CraftEntity) event.getRightClicked()).getHandle();
        event.setCancelled(true);

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
                    Location location = new Location(world, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                    ParticleUtil.globalParticle(location, Particle.SPELL_WITCH, 1, 0, 0, 0, 0);
                }

                // Display gate position
                BlockPos gate = fenceArea.getGatePosition();
                Location gateLoc = new Location(world, gate.getX() + 0.5, gate.getY() + 1, gate.getZ() + 0.5);
                ParticleUtil.globalParticle(gateLoc, Particle.VILLAGER_HAPPY, 20, 0.5, 0.5, 0.5, 0);

                // Draw line from the wolf to the gate
                ParticlePreset.displayLine(event.getRightClicked().getLocation(), gateLoc, 20, Particle.END_ROD, 1, 0, 0, 0, 0);
            }

            // Draw line to owner
            if (wolf.getOwner() != null && wolf.getOwner().isAlive()) {
                ParticlePreset.displayLine(event.getRightClicked().getLocation(), wolf.getOwner().getBukkitEntity().getLocation(),
                        20, Particle.END_ROD, 1, 0, 0, 0, 0);
                wolf.getOwner().addEffect(new MobEffectInstance(MobEffects.GLOWING, TimeUtil.seconds(5), 0, false, false));
            }
        } else if (entity instanceof VillagerCat cat) {
            event.setCancelled(true);

            Brain<Cat> brain = cat.getBrain();
            Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories = brain.getMemories();
            MessageUtil.sendMessageWithoutPrefix(player, "&bMemories of cat:");
            for (Map.Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> entry : memories.entrySet())
                MessageUtil.sendMessageWithoutPrefix(player, "&b - %s : %s", entry.getKey().toString(), entry.getValue().toString());
            MessageUtil.sendMessageWithoutPrefix(player, "&b - Owned by: %s (%s)", cat.getOwner(), cat.getOwnerUUID());

            // Draw line to owner
            if (cat.getOwner() != null && cat.getOwner().isAlive()) {
                ParticlePreset.displayLine(event.getRightClicked().getLocation(), cat.getOwner().getBukkitEntity().getLocation(),
                        20, Particle.END_ROD, 1, 0, 0, 0, 0);
                cat.getOwner().addEffect(new MobEffectInstance(MobEffects.GLOWING, TimeUtil.seconds(5), 0, false, false));
            }
        } else if (entity instanceof BaseVillager villager) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), () -> {
                player.closeInventory();
                player.openInventory(VillagerDebugMainGui.getViewableInventory(player, villager).getBukkitInventory());
            }, TimeUtil.ticks(5));
        }

    }

}
