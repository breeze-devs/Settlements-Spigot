package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class ScanForPetsBehaviorController {

    private static final int DETECT_COOLDOWN = TimeUtil.minutes(5);

    public static OneShot<Villager> scanForPets() {
        return new OneShot<>() {
            private int cooldown;

            @Override
            public boolean trigger(@Nonnull ServerLevel world, @Nonnull Villager villager, long time) {
                if (--this.cooldown > 0)
                    return false;
                this.cooldown = DETECT_COOLDOWN;

                Brain<Villager> brain = villager.getBrain();

                // Scan for wolves
                if (brain.hasMemoryValue(VillagerMemoryType.OWNED_DOG)) {
                    UUID wolfUuid = brain.getMemory(VillagerMemoryType.OWNED_DOG).get();
                    Wolf wolf = (Wolf) villager.level.getMinecraftWorld().getEntity(wolfUuid);
                    if (wolf == null || !wolf.isAlive()) {
                        villager.getBrain().eraseMemory(VillagerMemoryType.OWNED_DOG);
                        return false;
                    }
                }

                // Scan for cats
                if (brain.hasMemoryValue(VillagerMemoryType.OWNED_CAT)) {
                    UUID wolfUuid = brain.getMemory(VillagerMemoryType.OWNED_CAT).get();
                    Cat cat = (Cat) villager.level.getMinecraftWorld().getEntity(wolfUuid);
                    if (cat == null || !cat.isAlive()) {
                        villager.getBrain().eraseMemory(VillagerMemoryType.OWNED_CAT);
                        return false;
                    }
                }

                return true;
            }
        };
    }

}
