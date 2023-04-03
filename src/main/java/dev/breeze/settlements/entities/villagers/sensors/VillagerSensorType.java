package dev.breeze.settlements.entities.villagers.sensors;

import net.minecraft.world.entity.ai.sensing.SensorType;

public class VillagerSensorType {

    /**
     * Sensor for scanning nearby water areas
     */
    public static final String REGISTRY_KEY_NEAREST_WATER_AREA = "settlements_villager_nearest_water_area_sensor";
    public static SensorType<VillagerNearbyWaterAreaSensor> NEAREST_WATER_AREA;

    /**
     * Sensor for meal time
     * - breakfast: 1800-2200
     * - lunch: 5800-6200
     * - dinner: 10800-11200
     */
    public static final String REGISTRY_KEY_IS_MEAL_TIME = "settlements_villager_is_meal_time_sensor";
    public static SensorType<VillagerMealTimeSensor> IS_MEAL_TIME;

}
