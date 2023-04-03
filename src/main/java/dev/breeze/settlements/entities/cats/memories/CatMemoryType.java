package dev.breeze.settlements.entities.cats.memories;

import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.List;

public class CatMemoryType {

    /**
     * Nearby dropped items
     */
    public static final String REGISTRY_KEY_NEARBY_ITEMS = "settlements_cat_nearby_items_memory";
    public static MemoryModuleType<List<ItemEntity>> NEARBY_ITEMS;

}
