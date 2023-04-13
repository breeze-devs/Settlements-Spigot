package dev.breeze.settlements.entities.wolves.sensors;

import net.minecraft.world.entity.ai.sensing.SensorType;

public class WolfSensorType {

    /**
     * Sensor for detecting owner
     */
    public static final String REGISTRY_KEY_OWNER = "settlements_wolf_owner_sensor";
    public static SensorType<WolfOwnerSensor> OWNER;

    /**
     * Sensor for scanning nearby items
     */
    public static final String REGISTRY_KEY_NEARBY_ITEMS = "settlements_wolf_nearby_items_sensor";
    public static SensorType<WolfNearbyItemsSensor> NEARBY_ITEMS;

    /**
     * Sensor for scanning nearby sniffable living entities
     */
    public static final String REGISTRY_KEY_NEARBY_SNIFFABLE_ENTITIES = "settlements_wolf_nearby_sniffable_entities_sensor";
    public static SensorType<WolfSniffableEntitiesSensor> NEARBY_SNIFFABLE_ENTITIES;

    /**
     * Sensor for scanning the nearest fence area
     */
    public static final String REGISTRY_KEY_NEAREST_FENCE_AREA = "settlements_wolf_nearest_fence_area_sensor";
    public static SensorType<WolfFenceAreaSensor> NEAREST_FENCE_AREA;

    /**
     * Sensor for scanning nearby sheep
     */
    public static final String REGISTRY_KEY_NEARBY_SHEEP = "settlements_wolf_nearby_sheep_sensor";
    public static SensorType<WolfNearbySheepSensor> NEARBY_SHEEP;

}
