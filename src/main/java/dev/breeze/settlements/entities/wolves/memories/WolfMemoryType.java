package dev.breeze.settlements.entities.wolves.memories;

import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.entities.wolves.sensors.WolfFenceAreaSensor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

public class WolfMemoryType {

    private static final String NBT_TAG_NAME = "settlements_wolf_memories";

    /**
     * Nearby dropped items
     */
    public static final String REGISTRY_KEY_NEARBY_ITEMS = "settlements_wolf_nearby_items_memory";
    public static MemoryModuleType<List<ItemEntity>> NEARBY_ITEMS;

    /**
     * Nearby living entities that can be sniffed
     */
    public static final String REGISTRY_KEY_SNIFFABLE_ENTITIES = "settlements_wolf_sniffable_entities_memory";
    public static MemoryModuleType<List<LivingEntity>> NEARBY_SNIFFABLE_ENTITIES;

    /**
     * Nearby living entities that the wolf have recently sniffed
     * - i.e. sniffed in this current walk
     */
    public static final String REGISTRY_KEY_RECENTLY_SNIFFED_ENTITIES = "settlements_wolf_recently_sniffed_entities_memory";
    public static MemoryModuleType<Set<LivingEntity>> RECENTLY_SNIFFED_ENTITIES;

    /**
     * Nearest closed-off fence area with a gate
     */
    public static final String REGISTRY_KEY_NEAREST_FENCE_AREA = "settlements_wolf_nearest_fence_area_memory";
    public static MemoryModuleType<WolfFenceAreaSensor.FenceArea> NEAREST_FENCE_AREA;

    /**
     * Nearby sheep
     */
    public static final String REGISTRY_KEY_NEARBY_SHEEP = "settlements_wolf_nearby_sheep_memory";
    public static MemoryModuleType<List<Sheep>> NEARBY_SHEEP;

    /**
     * Export important memories to NBT
     * - only certain memories are persistent
     * - other are deleted upon unloading
     */
    public static void save(@Nonnull CompoundTag nbt, @Nonnull VillagerWolf wolf) {
        Brain<Wolf> brain = wolf.getBrain();
        CompoundTag memories = new CompoundTag();

        if (brain.hasMemoryValue(NEAREST_FENCE_AREA)) {
            WolfFenceAreaSensor.FenceArea fenceArea = brain.getMemory(NEAREST_FENCE_AREA).get();
            memories.put(REGISTRY_KEY_NEAREST_FENCE_AREA, fenceArea.toNbtTag());
        }

        // Write to NBT tag
        nbt.put(NBT_TAG_NAME, memories);
    }

    /**
     * Attempts to load the custom memories to the villager brain
     */
    public static void load(@Nonnull CompoundTag nbt, @Nonnull VillagerWolf wolf) {
        // Safety check
        if (!nbt.contains(NBT_TAG_NAME))
            return;

        // Load memories to brain
        Brain<Wolf> brain = wolf.getBrain();
        CompoundTag memories = nbt.getCompound(NBT_TAG_NAME);
        if (memories.contains(REGISTRY_KEY_NEAREST_FENCE_AREA)) {
            brain.setMemory(NEAREST_FENCE_AREA, WolfFenceAreaSensor.FenceArea.fromNbt(memories.getCompound(REGISTRY_KEY_NEAREST_FENCE_AREA)));
        }
    }

}
