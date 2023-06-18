package dev.breeze.settlements.entities.villagers.sensors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.utils.DebugUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import org.bukkit.Material;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class VillagerNearbySellerSensor extends BaseVillagerSensor {

    private static final int SENSE_COOLDOWN = TimeUtil.minutes(3);

    public VillagerNearbySellerSensor() {
        super(SENSE_COOLDOWN);
    }

    @Override
    protected void tickSensor(@Nonnull ServerLevel world, @Nonnull BaseVillager baseVillager, @Nonnull Brain<Villager> brain) {
        if (!brain.hasMemoryValue(MemoryModuleType.NEAREST_LIVING_ENTITIES)
                || !brain.hasMemoryValue(VillagerMemoryType.SHOPPING_LIST.getMemoryModuleType())) {
            return;
        }

        // Get the villager's shopping list
        Map<Material, Integer> shoppingList = brain.getMemory(VillagerMemoryType.SHOPPING_LIST.getMemoryModuleType()).get();
        if (shoppingList.isEmpty()) {
            return;
        }

        // Scan for nearby villagers that sell the item
        Map<UUID, Material> sellerMap = new HashMap<>();
        for (LivingEntity nearbyEntity : brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).get()) {
            if (!(nearbyEntity instanceof BaseVillager nearbyVillager)) {
                continue;
            }

            // Loop through each item in the shopping list
            // - note that this does not remove the item from the shopping list
            // - meaning that the same item can be bought from multiple sellers (albeit at different prices)
            // - the item should be removed from the shopping list when the villager buys the item
            for (Map.Entry<Material, Integer> entry : shoppingList.entrySet()) {
                // Evaluate the price & check if we can afford it
                int evaluatedPrice = baseVillager.evaluatePrice(entry.getKey());
                int canAfford = baseVillager.getEmeraldBalance() / evaluatedPrice;
                if (canAfford <= 0) {
                    continue;
                }

                // Check stock and friendship
                if (nearbyVillager.getStock(entry.getKey()) <= 0
                        || nearbyVillager.getFriendshipTowards(baseVillager) < BaseVillager.MIN_FRIENDSHIP_TO_TRADE
                        || baseVillager.getFriendshipTowards(nearbyVillager) < BaseVillager.MIN_FRIENDSHIP_TO_TRADE) {
                    continue;
                }

                // Add the seller
                sellerMap.put(nearbyVillager.getUUID(), entry.getKey());
                DebugUtil.broadcastEntity("Added seller with %dx %s".formatted(nearbyVillager.getStock(entry.getKey()), entry.getKey().name()),
                        nearbyVillager.getStringUUID(), nearbyVillager.getHoverDescription());

                // Break the loop because we don't want to add the same seller twice
                break;
            }
        }

        // Set memory
        VillagerMemoryType.NEARBY_SELLERS.set(brain, sellerMap);
    }

    @Override
    @Nonnull
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(VillagerMemoryType.NEARBY_SELLERS.getMemoryModuleType());
    }

    @Nonnull
    @Override
    public ItemStackBuilder getGuiItemBuilderAbstract() {
        return new ItemStackBuilder(Material.VILLAGER_SPAWN_EGG)
                .setDisplayName("&eNearby seller sensor")
                .setLore("&fScans for nearby villagers that sells the wanted items");
    }

}
