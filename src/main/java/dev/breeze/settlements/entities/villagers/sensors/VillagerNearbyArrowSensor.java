package dev.breeze.settlements.entities.villagers.sensors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.AABB;
import org.bukkit.Material;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

public class VillagerNearbyArrowSensor extends BaseVillagerSensor {

    private static final int SENSE_COOLDOWN = TimeUtil.seconds(20);

    private static final double RADIUS_HORIZONTAL = 15.0;
    private static final double RADIUS_VERTICAL = 5.0;

    public VillagerNearbyArrowSensor() {
        super(SENSE_COOLDOWN);
    }

    @Override
    protected void tickSensor(@Nonnull ServerLevel world, @Nonnull BaseVillager baseVillager, @Nonnull Brain<Villager> brain) {
        AABB aABB = baseVillager.getBoundingBox().inflate(RADIUS_HORIZONTAL, RADIUS_VERTICAL, RADIUS_HORIZONTAL);
        List<AbstractArrow> nearbyArrows = world.getEntitiesOfClass(AbstractArrow.class, aABB, abstractArrow -> !(abstractArrow instanceof ThrownTrident) && abstractArrow.inGround);
        VillagerMemoryType.NEARBY_ARROWS.set(brain, nearbyArrows);
    }

    @Override
    @Nonnull
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(VillagerMemoryType.NEARBY_ARROWS.getMemoryModuleType());
    }

    @Nonnull
    @Override
    public ItemStackBuilder getGuiItemBuilderAbstract() {
        return new ItemStackBuilder(Material.ARROW)
                .setDisplayName("&eNearby arrow sensor")
                .setLore("&fScans for nearby arrows that the villager can pick up");
    }

}
