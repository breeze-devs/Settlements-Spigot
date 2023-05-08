package dev.breeze.settlements.entities.villagers.sensors;

import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.utils.Habitat;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.npc.Villager;
import org.bukkit.block.Biome;

import javax.annotation.Nonnull;
import java.util.Set;

public class VillagerHabitatSensor extends Sensor<Villager> {

    private static final int SENSE_COOLDOWN = TimeUtil.seconds(10); // TODO: 5 minutes

    public VillagerHabitatSensor() {
        super(SENSE_COOLDOWN);
    }

    @Override
    protected void doTick(@Nonnull ServerLevel world, @Nonnull Villager villager) {
        Brain<Villager> brain = villager.getBrain();
        Biome biome = world.getWorld().getBiome(villager.getBlockX(), villager.getBlockZ());
        VillagerMemoryType.CURRENT_HABITAT.set(brain, Habitat.fromBiome(biome));
    }

    @Override
    @Nonnull
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(VillagerMemoryType.CURRENT_HABITAT.getMemoryModuleType());
    }

}
