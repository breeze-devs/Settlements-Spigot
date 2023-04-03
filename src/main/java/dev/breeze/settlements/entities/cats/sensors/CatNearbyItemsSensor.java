package dev.breeze.settlements.entities.cats.sensors;

import dev.breeze.settlements.entities.cats.VillagerCat;
import dev.breeze.settlements.entities.cats.memories.CatMemoryType;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CatNearbyItemsSensor extends Sensor<Cat> {

    /**
     * How far away to scan for items horizontally
     */
    private static final double RANGE_HORIZONTAL = 10.0D;

    /**
     * How far away to scan for items vertically
     */
    private static final double RANGE_VERTICAL = 5.5D;

    /**
     * How often will the cat scans for nearby items
     */
    private static final int SENSE_COOLDOWN = TimeUtil.seconds(10);

    public CatNearbyItemsSensor() {
        super(SENSE_COOLDOWN);
    }

    @Override
    protected void doTick(@Nonnull ServerLevel world, @Nonnull Cat cat) {
        // Type cast checking
        if (!(cat instanceof VillagerCat villagerCat))
            return;

        // Scan for nearby dropped items
        List<ItemEntity> list = world.getEntitiesOfClass(ItemEntity.class, villagerCat.getBoundingBox().inflate(RANGE_HORIZONTAL, RANGE_VERTICAL,
                RANGE_HORIZONTAL), (itemEntity -> itemEntity != null && !itemEntity.isPassenger()));

        // Set or erase memory
        Brain<Cat> brain = villagerCat.getBrain();
        if (list.isEmpty())
            brain.eraseMemory(CatMemoryType.NEARBY_ITEMS);
        else
            brain.setMemory(CatMemoryType.NEARBY_ITEMS, Optional.of(list));
    }

    @Override
    @Nonnull
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(CatMemoryType.NEARBY_ITEMS);
    }

}
