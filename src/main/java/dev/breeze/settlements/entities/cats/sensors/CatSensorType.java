package dev.breeze.settlements.entities.cats.sensors;

import net.minecraft.world.entity.ai.sensing.SensorType;

public class CatSensorType {

    /**
     * Sensor for scanning nearby items
     */
    public static final String REGISTRY_KEY_NEARBY_ITEMS = "settlements_cat_nearby_items_sensor";
    public static SensorType<CatNearbyItemsSensor> NEARBY_ITEMS;

}
