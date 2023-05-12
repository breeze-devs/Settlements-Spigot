package dev.breeze.settlements.entities.villagers.sensors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.utils.Habitat;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import org.bukkit.Material;
import org.bukkit.block.Biome;

import javax.annotation.Nonnull;
import java.util.Set;

public class VillagerHabitatSensor extends BaseVillagerSensor {

    private static final int SENSE_COOLDOWN = TimeUtil.minutes(5);

    public VillagerHabitatSensor() {
        super(SENSE_COOLDOWN);
    }

    @Override
    protected void tickSensor(@Nonnull ServerLevel world, @Nonnull BaseVillager villager, @Nonnull Brain<Villager> brain) {
        Biome biome = world.getWorld().getBiome(villager.getBlockX(), villager.getBlockZ());
        VillagerMemoryType.CURRENT_HABITAT.set(brain, Habitat.fromBiome(biome));
    }

    @Override
    @Nonnull
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(VillagerMemoryType.CURRENT_HABITAT.getMemoryModuleType());
    }

    @Nonnull
    @Override
    public ItemStackBuilder getGuiItemBuilderAbstract() {
        return new ItemStackBuilder(Material.GRASS_BLOCK)
                .setDisplayName("&eCurrent habitat sensor")
                .setLore("&fOccasionally checks what biome the villager is currently in");
    }


}
