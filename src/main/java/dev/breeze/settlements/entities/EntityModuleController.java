package dev.breeze.settlements.entities;

import com.mojang.serialization.Codec;
import dev.breeze.settlements.entities.cats.VillagerCat;
import dev.breeze.settlements.entities.cats.memories.CatMemoryType;
import dev.breeze.settlements.entities.cats.sensors.CatNearbyItemsSensor;
import dev.breeze.settlements.entities.cats.sensors.CatOwnerSensor;
import dev.breeze.settlements.entities.cats.sensors.CatSensorType;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.events.VillagerRestockEvent;
import dev.breeze.settlements.entities.villagers.inventory.VillagerInventoryEditEvents;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemory;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.entities.villagers.sensors.*;
import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.entities.wolves.memories.WolfMemoryType;
import dev.breeze.settlements.entities.wolves.sensors.*;
import dev.breeze.settlements.utils.BaseModuleController;
import dev.breeze.settlements.utils.LogUtil;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R2.CraftServer;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;

public class EntityModuleController extends BaseModuleController {

    public static final Set<Entity> temporaryEntities = new HashSet<>();

    @Override
    protected boolean preload(JavaPlugin plugin) {
        // Register all entities
        try {
            this.registerEntities(Map.of(
                    BaseVillager.ENTITY_TYPE, BaseVillager.getEntityTypeBuilder(),
                    VillagerWolf.ENTITY_TYPE, VillagerWolf.getEntityTypeBuilder(),
                    VillagerCat.ENTITY_TYPE, VillagerCat.getEntityTypeBuilder()
            ));

            // Register memories and sensors
            this.registerMemories();
            this.registerSensors();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | NoSuchFieldException | InstantiationException e) {
            LogUtil.exception(e, "Exception encountered while registering modules!");
            return false;
        }

        return true;
    }

    @Override
    protected boolean load(JavaPlugin plugin, PluginManager pm) {
        pm.registerEvents(new VillagerRestockEvent(), plugin);

        pm.registerEvents(new VillagerInventoryEditEvents(), plugin);
        return true;
    }

    @Override
    protected void teardown() {
        // Remove all temporary entities
        temporaryEntities.forEach(entity -> {
            if (entity == null || !entity.isAlive())
                return;
            entity.remove(Entity.RemovalReason.DISCARDED);
        });
    }

    /**
     * Registers all entities created in this module to the registry
     * - must be done before the world loads
     * - after registering, '/summon' works and the entity can persist restarts
     */
    @SuppressWarnings("JavaReflectionMemberAccess")
    private void registerEntities(Map<String, EntityType.Builder<Entity>> entityTypeMap) throws IllegalAccessException, InvocationTargetException,
            NoSuchMethodException, NoSuchFieldException {
        // Get entity type registry
        DedicatedServer dedicatedServer = ((CraftServer) Bukkit.getServer()).getServer();
        WritableRegistry<EntityType<?>> entityTypeRegistry =
                (WritableRegistry<EntityType<?>>) dedicatedServer.registryAccess().registryOrThrow(Registries.ENTITY_TYPE);

        // Unfreeze registry
        LogUtil.info("Unfreezing entity type registry (1/2)...");
        // l = private boolean frozen
        Field frozen = MappedRegistry.class.getDeclaredField("l");
        frozen.setAccessible(true);
        frozen.set(entityTypeRegistry, false);

        LogUtil.info("Unfreezing entity type registry (2/2)...");
        // m = private Map<T, Holder.Reference<T>> unregisteredIntrusiveHolders;
        Field unregisteredHolderMap = MappedRegistry.class.getDeclaredField("m");
        unregisteredHolderMap.setAccessible(true);
        unregisteredHolderMap.set(BuiltInRegistries.ENTITY_TYPE, new HashMap<>());

        // Unlock register method
        // a = private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder builder)
        Method register = EntityType.class.getDeclaredMethod("a", String.class, EntityType.Builder.class);
        register.setAccessible(true);

        // Build & register entities
        for (Map.Entry<String, EntityType.Builder<Entity>> entry : entityTypeMap.entrySet()) {
            register.invoke(null, entry.getKey(), entry.getValue());
        }

        // Re-freeze registry
        LogUtil.info("Re-freezing entity type registry...");
        BuiltInRegistries.ENTITY_TYPE.freeze();
        unregisteredHolderMap.set(BuiltInRegistries.ENTITY_TYPE, null);
    }

    /**
     * Registers all memory types created in this module to the registry
     * - must be done before the world loads
     */
    @SuppressWarnings("JavaReflectionMemberAccess")
    private void registerMemories() throws IllegalAccessException, NoSuchMethodException, NoSuchFieldException {
        // Get memory module type registry
        DedicatedServer dedicatedServer = ((CraftServer) Bukkit.getServer()).getServer();
        WritableRegistry<MemoryModuleType<?>> registry =
                (WritableRegistry<MemoryModuleType<?>>) dedicatedServer.registryAccess().registryOrThrow(Registries.MEMORY_MODULE_TYPE);

        // Unfreeze registry
        LogUtil.info("Unfreezing memory module type registry...");
        // l = private boolean frozen
        Field frozen = MappedRegistry.class.getDeclaredField("l");
        frozen.setAccessible(true);
        frozen.set(registry, false);

        /*
         * Build & register memories
         */
        // Villager memories
        for (VillagerMemory<?> memory : VillagerMemoryType.ALL_MEMORIES) {
            memory.setMemoryModuleType(registerMemory(memory.getIdentifier(), null));
        }

        // Wolf memories
        WolfMemoryType.NEARBY_ITEMS = registerMemory(WolfMemoryType.REGISTRY_KEY_NEARBY_ITEMS, null);
        WolfMemoryType.NEARBY_SNIFFABLE_ENTITIES = registerMemory(WolfMemoryType.REGISTRY_KEY_SNIFFABLE_ENTITIES, null);
        WolfMemoryType.RECENTLY_SNIFFED_ENTITIES = registerMemory(WolfMemoryType.REGISTRY_KEY_RECENTLY_SNIFFED_ENTITIES, null);
        WolfMemoryType.NEAREST_FENCE_AREA = registerMemory(WolfMemoryType.REGISTRY_KEY_NEAREST_FENCE_AREA, null);
        WolfMemoryType.NEARBY_SHEEP = registerMemory(WolfMemoryType.REGISTRY_KEY_NEARBY_SHEEP, null);

        // Cat memories
        CatMemoryType.NEARBY_ITEMS = registerMemory(CatMemoryType.REGISTRY_KEY_NEARBY_ITEMS, null);

        // Re-freeze registry
        LogUtil.info("Re-freezing memory module type registry...");
        BuiltInRegistries.MEMORY_MODULE_TYPE.freeze();
    }

    private <U> MemoryModuleType<U> registerMemory(@Nonnull String id, @Nullable Codec<U> codec) {
        if (codec == null) {
            return Registry.register(BuiltInRegistries.MEMORY_MODULE_TYPE, new ResourceLocation(id), new MemoryModuleType<>(Optional.empty()));
        }
        return Registry.register(BuiltInRegistries.MEMORY_MODULE_TYPE, new ResourceLocation(id), new MemoryModuleType<>(Optional.of(codec)));
    }

    /**
     * Registers all sensor types created in this module to the registry
     * - must be done before the world loads
     */
    @SuppressWarnings("JavaReflectionMemberAccess")
    private void registerSensors() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException,
            InstantiationException {
        // Get sensor type registry
        DedicatedServer dedicatedServer = ((CraftServer) Bukkit.getServer()).getServer();
        WritableRegistry<SensorType<?>> registry = (WritableRegistry<SensorType<?>>) dedicatedServer.registryAccess().registryOrThrow(Registries.SENSOR_TYPE);

        // Unfreeze registry
        LogUtil.info("Unfreezing sensor type registry...");
        // l = private boolean frozen
        Field frozen = MappedRegistry.class.getDeclaredField("l");
        frozen.setAccessible(true);
        frozen.set(registry, false);

        /*
         * Build & register sensors
         */
        // Villager sensors
        VillagerSensorType.NEAREST_WATER_AREA = registerSensor(VillagerSensorType.REGISTRY_KEY_NEAREST_WATER_AREA, VillagerNearbyWaterAreaSensor::new);
        VillagerSensorType.IS_MEAL_TIME = registerSensor(VillagerSensorType.REGISTRY_KEY_IS_MEAL_TIME, VillagerMealTimeSensor::new);
        VillagerSensorType.NEAREST_ENCHANTING_TABLE = registerSensor(VillagerSensorType.REGISTRY_KEY_NEAREST_ENCHANTING_TABLE,
                VillagerNearbyEnchantingTableSensor::new);
        VillagerSensorType.NEAREST_HARVESTABLE_SUGARCANE = registerSensor(VillagerSensorType.REGISTRY_KEY_NEAREST_HARVESTABLE_SUGARCANE,
                VillagerNearbyHarvestableSugarcaneSensor::new);
        VillagerSensorType.CURRENT_HABITAT = registerSensor(VillagerSensorType.REGISTRY_KEY_CURRENT_HABITAT, VillagerHabitatSensor::new);

        // Wolf sensors
        WolfSensorType.OWNER = registerSensor(WolfSensorType.REGISTRY_KEY_OWNER, WolfOwnerSensor::new);
        WolfSensorType.NEARBY_ITEMS = registerSensor(WolfSensorType.REGISTRY_KEY_NEARBY_ITEMS, WolfNearbyItemsSensor::new);
        WolfSensorType.NEARBY_SNIFFABLE_ENTITIES = registerSensor(WolfSensorType.REGISTRY_KEY_NEARBY_SNIFFABLE_ENTITIES, WolfSniffableEntitiesSensor::new);
        WolfSensorType.NEAREST_FENCE_AREA = registerSensor(WolfSensorType.REGISTRY_KEY_NEAREST_FENCE_AREA, WolfFenceAreaSensor::new);
        WolfSensorType.NEARBY_SHEEP = registerSensor(WolfSensorType.REGISTRY_KEY_NEARBY_SHEEP, WolfNearbySheepSensor::new);

        // Cat sensors
        CatSensorType.OWNER = registerSensor(CatSensorType.REGISTRY_KEY_OWNER, CatOwnerSensor::new);
        CatSensorType.NEARBY_ITEMS = registerSensor(CatSensorType.REGISTRY_KEY_NEARBY_ITEMS, CatNearbyItemsSensor::new);

        // Re-freeze registry
        LogUtil.info("Re-freezing sensor type registry...");
        BuiltInRegistries.SENSOR_TYPE.freeze();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <U extends Sensor<?>> SensorType<U> registerSensor(@Nonnull String id, @Nonnull Supplier<U> factory) throws NoSuchMethodException,
            InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<SensorType> constructor = SensorType.class.getDeclaredConstructor(Supplier.class);
        constructor.setAccessible(true);

        SensorType<?> sensor = constructor.newInstance(factory);
        return (SensorType<U>) Registry.register(BuiltInRegistries.SENSOR_TYPE, new ResourceLocation(id), sensor);
    }


}
