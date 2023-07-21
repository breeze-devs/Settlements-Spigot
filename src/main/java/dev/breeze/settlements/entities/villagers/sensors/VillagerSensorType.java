package dev.breeze.settlements.entities.villagers.sensors;

import java.util.Arrays;
import java.util.List;

public class VillagerSensorType {

    public static final VillagerSensor<VillagerNearbyWaterAreaSensor> NEAREST_WATER_AREA = VillagerSensor.<VillagerNearbyWaterAreaSensor>builder()
            .identifier("nearest_water_area")
            .sensorSupplier(VillagerNearbyWaterAreaSensor::new)
            .build();

    public static final VillagerSensor<VillagerMealTimeSensor> IS_MEAL_TIME = VillagerSensor.<VillagerMealTimeSensor>builder()
            .identifier("is_meal_time")
            .sensorSupplier(VillagerMealTimeSensor::new)
            .build();

    public static final VillagerSensor<VillagerNearbyEnchantingTableSensor> NEAREST_ENCHANTING_TABLE = VillagerSensor.<VillagerNearbyEnchantingTableSensor>builder()
            .identifier("nearest_enchanting_table")
            .sensorSupplier(VillagerNearbyEnchantingTableSensor::new)
            .build();

    public static final VillagerSensor<VillagerNearbyHarvestableSugarcaneSensor> NEAREST_HARVESTABLE_SUGARCANE = VillagerSensor.<VillagerNearbyHarvestableSugarcaneSensor>builder()
            .identifier("nearest_harvestable_sugarcane")
            .sensorSupplier(VillagerNearbyHarvestableSugarcaneSensor::new)
            .build();

    public static final VillagerSensor<VillagerHabitatSensor> CURRENT_HABITAT = VillagerSensor.<VillagerHabitatSensor>builder()
            .identifier("current_habitat")
            .sensorSupplier(VillagerHabitatSensor::new)
            .build();

    public static final VillagerSensor<VillagerNearbySellerSensor> NEARBY_SELLER = VillagerSensor.<VillagerNearbySellerSensor>builder()
            .identifier("nearby_seller")
            .sensorSupplier(VillagerNearbySellerSensor::new)
            .build();

    public static final VillagerSensor<VillagerNearbyArrowSensor> NEARBY_ARROWS = VillagerSensor.<VillagerNearbyArrowSensor>builder()
            .identifier("nearby_arrows")
            .sensorSupplier(VillagerNearbyArrowSensor::new)
            .build();

    public static final VillagerSensor<VillagerNearbyCraftingTableSensor> NEAREST_CRAFTING_TABLE = VillagerSensor.<VillagerNearbyCraftingTableSensor>builder()
            .identifier("nearest_crafting_table")
            .sensorSupplier(VillagerNearbyCraftingTableSensor::new)
            .build();

    /**
     * List of all memories for bulk memory operations such as save/load
     */
    public static final List<VillagerSensor<? extends BaseVillagerSensor>> ALL_SENSORS = Arrays.asList(
            // Block sensors
            NEAREST_WATER_AREA, NEAREST_HARVESTABLE_SUGARCANE, NEAREST_ENCHANTING_TABLE, NEAREST_CRAFTING_TABLE,
            // Time sensors
            IS_MEAL_TIME,
            // Trading sensors
            NEARBY_SELLER,
            // Miscellaneous sensors
            CURRENT_HABITAT, NEARBY_ARROWS
    );

}
