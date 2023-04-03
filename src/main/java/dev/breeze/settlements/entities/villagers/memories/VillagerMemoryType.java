package dev.breeze.settlements.entities.villagers.memories;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.wolves.VillagerWolf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

public class VillagerMemoryType {

    private static final String NBT_TAG_NAME = "settlements_memories";

    public static final String REGISTRY_KEY_FENCE_GATE_TO_CLOSE = "settlements_villager_fence_gates_to_close_memory";
    public static MemoryModuleType<Set<GlobalPos>> FENCE_GATE_TO_CLOSE;

    public static final String REGISTRY_KEY_OWNED_DOG = "settlements_villager_owned_dog_memory";
    public static MemoryModuleType<UUID> OWNED_DOG;

    public static final String REGISTRY_KEY_OWNED_CAT = "settlements_villager_owned_cat_memory";
    public static MemoryModuleType<UUID> OWNED_CAT;

    public static final String REGISTRY_KEY_WALK_DOG_TARGET = "settlements_villager_walk_dog_target_memory";
    public static MemoryModuleType<VillagerWolf> WALK_DOG_TARGET;

    public static final String REGISTRY_KEY_NEAREST_WATER_AREA = "settlements_villager_nearest_water_area_memory";
    public static MemoryModuleType<BlockPos> NEAREST_WATER_AREA;

    public static final String REGISTRY_KEY_IS_MEAL_TIME = "settlements_villager_is_meal_time_memory";
    public static MemoryModuleType<Boolean> IS_MEAL_TIME;


    /**
     * Export important memories to NBT
     * - only certain memories are persistent
     * - other are deleted upon unloading
     */
    public static void save(@Nonnull CompoundTag nbt, @Nonnull BaseVillager villager) {
        Brain<Villager> brain = villager.getBrain();
        CompoundTag memories = new CompoundTag();

        if (brain.hasMemoryValue(OWNED_DOG)) {
            UUID uuid = brain.getMemory(OWNED_DOG).get();
            memories.put(REGISTRY_KEY_OWNED_DOG, StringTag.valueOf(uuid.toString()));
        }

        if (brain.hasMemoryValue(OWNED_CAT)) {
            UUID uuid = brain.getMemory(OWNED_CAT).get();
            memories.put(REGISTRY_KEY_OWNED_CAT, StringTag.valueOf(uuid.toString()));
        }

        if (brain.hasMemoryValue(NEAREST_WATER_AREA)) {
            BlockPos pos = brain.getMemory(NEAREST_WATER_AREA).get();
            memories.put(REGISTRY_KEY_NEAREST_WATER_AREA, LongTag.valueOf(pos.asLong()));
        }

        // Write to NBT tag
        nbt.put(NBT_TAG_NAME, memories);
    }

    /**
     * Attempts to load the custom memories to the villager brain
     */
    public static void load(@Nonnull CompoundTag nbt, @Nonnull BaseVillager villager) {
        // Safety check
        if (!nbt.contains(NBT_TAG_NAME))
            return;

        // Load memories to brain
        Brain<Villager> brain = villager.getBrain();
        CompoundTag memories = nbt.getCompound(NBT_TAG_NAME);
        if (memories.contains(REGISTRY_KEY_OWNED_DOG)) {
            brain.setMemory(OWNED_DOG, UUID.fromString(memories.getString(REGISTRY_KEY_OWNED_DOG)));
        }

        if (memories.contains(REGISTRY_KEY_OWNED_CAT)) {
            brain.setMemory(OWNED_CAT, UUID.fromString(memories.getString(REGISTRY_KEY_OWNED_CAT)));
        }

        if (memories.contains(REGISTRY_KEY_NEAREST_WATER_AREA)) {
            brain.setMemory(NEAREST_WATER_AREA, BlockPos.of(memories.getLong(REGISTRY_KEY_NEAREST_WATER_AREA)));
        }
    }

}
