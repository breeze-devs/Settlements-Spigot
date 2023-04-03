package dev.breeze.settlements.test;

import dev.breeze.settlements.Main;
import dev.breeze.settlements.entities.cats.VillagerCat;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.inventory.VillagerInventory;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.entities.wolves.memories.WolfMemoryType;
import dev.breeze.settlements.entities.wolves.sensors.WolfFenceAreaSensor;
import dev.breeze.settlements.guis.CustomInventory;
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
import net.minecraft.world.entity.npc.Villager;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MemoryEvent implements Listener {

    @EventHandler
    public void onClickEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        ItemStack item = player.getInventory().getItem(event.getHand());

        if (item.getType() != Material.DEBUG_STICK)
            return;
        Entity entity = ((CraftEntity) event.getRightClicked()).getHandle();
        if (entity instanceof VillagerWolf wolf) {
            event.setCancelled(true);

            Brain<Wolf> brain = wolf.getBrain();
            Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories = brain.getMemories();
            MessageUtil.sendMessage(player, "&bMemories of wolf:");
            for (Map.Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> entry : memories.entrySet())
                MessageUtil.sendMessage(player, "&b - %s : %s", entry.getKey().toString(), entry.getValue().toString());
            MessageUtil.sendMessage(player, "&b - Owned by: %s (%s)", wolf.getOwner(), wolf.getOwnerUUID());

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
            MessageUtil.sendMessage(player, "&bMemories of cat:");
            for (Map.Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> entry : memories.entrySet())
                MessageUtil.sendMessage(player, "&b - %s : %s", entry.getKey().toString(), entry.getValue().toString());
            MessageUtil.sendMessage(player, "&b - Owned by: %s (%s)", cat.getOwner(), cat.getOwnerUUID());

            // Draw line to owner
            if (cat.getOwner() != null && cat.getOwner().isAlive()) {
                ParticlePreset.displayLine(event.getRightClicked().getLocation(), cat.getOwner().getBukkitEntity().getLocation(),
                        20, Particle.END_ROD, 1, 0, 0, 0, 0);
                cat.getOwner().addEffect(new MobEffectInstance(MobEffects.GLOWING, TimeUtil.seconds(5), 0, false, false));
            }
        } else if (entity instanceof BaseVillager villager) {
            event.setCancelled(true);

            Brain<Villager> brain = villager.getBrain();
            Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories = brain.getMemories();
            MessageUtil.sendMessage(player, "&bMemories of villager:");
            for (Map.Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> entry : memories.entrySet()) {
                String prefix = entry.getKey().toString().contains("settlements") ? "&e" : "&b";
                MessageUtil.sendMessage(player, "%s - %s : %s", prefix, entry.getKey().toString(), entry.getValue().toString());
            }

            MessageUtil.sendMessage(player, "&eProfession: %s", villager.getProfession().toString());
            MessageUtil.sendMessage(player, "&eReputation: %d", villager.getPlayerReputation(((CraftPlayer) player).getHandle()));

            // Display water area visually
            if (brain.hasMemoryValue(VillagerMemoryType.NEAREST_WATER_AREA)) {
                BlockPos waterPos = brain.getMemory(VillagerMemoryType.NEAREST_WATER_AREA).get();
                Location waterLocation = new Location(world, waterPos.getX(), waterPos.getY(), waterPos.getZ());
                ParticleUtil.globalParticle(waterLocation, Particle.VILLAGER_HAPPY, 20, 0.5, 0.5, 0.5, 0);
                ParticlePreset.displayLine(villager.getBukkitEntity().getLocation(), waterLocation, 20, Particle.END_ROD, 1, 0, 0, 0, 0);
            }

            // Display owned dogs
            if (brain.hasMemoryValue(VillagerMemoryType.OWNED_DOG)) {
                UUID wolfUuid = brain.getMemory(VillagerMemoryType.OWNED_DOG).get();
                Wolf wolf = (Wolf) villager.level.getMinecraftWorld().getEntity(wolfUuid);

                if (wolf == null || !wolf.isAlive()) {
                    // If wolf is not alive, reset memory
                    villager.getBrain().eraseMemory(VillagerMemoryType.OWNED_DOG);
                } else {
                    // Otherwise, draw line to it
                    Location wolfLocation = new Location(world, wolf.getX(), wolf.getY(), wolf.getZ());
                    ParticlePreset.displayLine(villager.getBukkitEntity().getLocation(), wolfLocation, 20, Particle.END_ROD, 1, 0, 0, 0, 0);
                    wolf.addEffect(new MobEffectInstance(MobEffects.GLOWING, TimeUtil.seconds(5), 0, false, false));
                }
            }

            // Display owned cats
            if (brain.hasMemoryValue(VillagerMemoryType.OWNED_CAT)) {
                UUID catUuid = brain.getMemory(VillagerMemoryType.OWNED_CAT).get();
                Cat cat = (Cat) villager.level.getMinecraftWorld().getEntity(catUuid);

                if (cat == null || !cat.isAlive()) {
                    // If cat is not alive, reset memory
                    villager.getBrain().eraseMemory(VillagerMemoryType.OWNED_CAT);
                } else {
                    // Otherwise, draw line to it
                    Location catLocation = new Location(world, cat.getX(), cat.getY(), cat.getZ());
                    ParticlePreset.displayLine(villager.getBukkitEntity().getLocation(), catLocation, 20, Particle.END_ROD, 1, 0, 0, 0, 0);
                    cat.addEffect(new MobEffectInstance(MobEffects.GLOWING, TimeUtil.seconds(5), 0, false, false));
                }
            }

            // Display inventory
            VillagerInventory inventory = villager.getCustomInventory();
            CustomInventory inventoryPreview = inventory.getViewableInventory();
            Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), () -> inventoryPreview.showToPlayer(player), TimeUtil.ticks(5));
        }

    }

}
