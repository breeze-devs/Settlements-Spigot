package dev.breeze.settlements.entities.villagers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import dev.breeze.settlements.config.files.InternalTradingConfig;
import dev.breeze.settlements.config.files.WolfFetchItemConfig;
import dev.breeze.settlements.entities.villagers.behaviors.BaseVillagerBehavior;
import dev.breeze.settlements.entities.villagers.inventory.VillagerInventory;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemory;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.entities.villagers.navigation.VillagerNavigation;
import dev.breeze.settlements.entities.villagers.sensors.BaseVillagerSensor;
import dev.breeze.settlements.entities.villagers.sensors.VillagerSensor;
import dev.breeze.settlements.entities.villagers.sensors.VillagerSensorType;
import dev.breeze.settlements.utils.DebugUtil;
import dev.breeze.settlements.utils.LogUtil;
import dev.breeze.settlements.utils.StringUtil;
import dev.breeze.settlements.utils.VillagerUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Predicate;

public class BaseVillager extends Villager {

    public static final String ENTITY_TYPE = "settlements_villager";
    private static final String INVENTORY_NBT_TAG = "custom_inventory";

    // TODO: perhaps associate this with personality
    public static final double MIN_FRIENDSHIP_TO_TRADE = InternalTradingConfig.getInstance().getMinFriendshipToTrade().getValue();

    private static final Map<VillagerProfession, Material> PROFESSION_MATERIAL_MAP = Map.ofEntries(
            Map.entry(VillagerProfession.NONE, Material.BARRIER),
            Map.entry(VillagerProfession.ARMORER, Material.BLAST_FURNACE),
            Map.entry(VillagerProfession.BUTCHER, Material.SMOKER),
            Map.entry(VillagerProfession.CARTOGRAPHER, Material.CARTOGRAPHY_TABLE),
            Map.entry(VillagerProfession.CLERIC, Material.BREWING_STAND),
            Map.entry(VillagerProfession.FARMER, Material.COMPOSTER),
            Map.entry(VillagerProfession.FISHERMAN, Material.BARREL),
            Map.entry(VillagerProfession.FLETCHER, Material.FLETCHING_TABLE),
            Map.entry(VillagerProfession.LEATHERWORKER, Material.CAULDRON),
            Map.entry(VillagerProfession.LIBRARIAN, Material.LECTERN),
            Map.entry(VillagerProfession.MASON, Material.STONECUTTER),
            Map.entry(VillagerProfession.NITWIT, Material.POTATO),
            Map.entry(VillagerProfession.SHEPHERD, Material.LOOM),
            Map.entry(VillagerProfession.TOOLSMITH, Material.SMITHING_TABLE),
            Map.entry(VillagerProfession.WEAPONSMITH, Material.GRINDSTONE)
    );

    @Getter
    private final Map<Activity, List<BaseVillagerBehavior>> activityBehaviorListMap = new HashMap<>();

    @Getter
    @Nonnull
    private VillagerInventory customInventory;

    @Getter
    @Setter
    private boolean defaultWalkTargetDisabled;

    /**
     * Constructor called when Minecraft tries to load the entity
     */
    public BaseVillager(@Nonnull EntityType<? extends Villager> entityType, @Nonnull Level level) {
        super(EntityType.VILLAGER, level);
        this.init();
    }

    /**
     * Constructor to spawn the villager in manually
     */
    public BaseVillager(@Nonnull Location location, @Nonnull VillagerType villagertype) {
        super(EntityType.VILLAGER, ((CraftWorld) location.getWorld()).getHandle(), villagertype);
        this.setPos(location.getX(), location.getY(), location.getZ());
        if (!this.level.addFreshEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM)) {
            throw new IllegalStateException("Failed to add custom villager to world");
        }

        this.init();
    }

    private void init() {
        // Configure navigation controller
        VillagerNavigation navigation = new VillagerNavigation(this, this.level);
        navigation.setCanOpenDoors(true);
        navigation.setCanFloat(true);
        this.navigation = navigation;

        // this.initPathfinderGoals();
        this.refreshBrain(this.level.getMinecraftWorld());

        // Configure extra data
        this.customInventory = new VillagerInventory(this, VillagerInventory.DEFAULT_INVENTORY_ROWS);

        // Initialize miscellaneous variables
        this.defaultWalkTargetDisabled = false;
    }

    /**
     * Called before world load to build the entity type
     */
    public static EntityType.Builder<Entity> getEntityTypeBuilder() {
        return EntityType.Builder.of(BaseVillager::new, MobCategory.MISC)
                .sized(0.6F, 1.95F)
                .clientTrackingRange(10);
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        DebugUtil.log("Loading custom villager (%s)", this.getUUID().toString());

        // Attempt to load saved inventory data
        if (nbt.contains(INVENTORY_NBT_TAG, Tag.TAG_COMPOUND)) {
            this.customInventory = VillagerInventory.fromNbt(this, nbt.getCompound(INVENTORY_NBT_TAG));
        }

        // Load custom memories to brain
        VillagerMemoryType.load(nbt, this);
    }

    /**
     * Saves villager data to the NBT tag
     */
    @Override
    public void addAdditionalSaveData(@Nonnull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);

        // IMPORTANT: save as custom ID to persist this entity
        nbt.putString("id", "minecraft:" + ENTITY_TYPE);
        nbt.putString("plugin", "Settlements");

        // Save inventory data
        nbt.put(INVENTORY_NBT_TAG, this.customInventory.toNbtTag());

        // Save custom memories
        VillagerMemoryType.save(nbt, this);
    }

    @Override
    public boolean save(@Nonnull CompoundTag nbt) {
        DebugUtil.log("Saving custom villager (%s)", this.getUUID().toString());
        return super.save(nbt);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected @Nonnull Brain.Provider<Villager> brainProvider() {
        try {
            // cB = private static final ImmutableList<MemoryModuleType<?>>
            final ImmutableList<MemoryModuleType<?>> DEFAULT_MEMORY_TYPES = (ImmutableList<MemoryModuleType<?>>) FieldUtils.readStaticField(Villager.class,
                    "cB", true);
            // cC = private static final ImmutableList<SensorType<? extends Sensor<? super Villager>>>
            final ImmutableList<SensorType<Sensor<Villager>>> DEFAULT_SENSOR_TYPES = (ImmutableList<SensorType<Sensor<Villager>>>)
                    FieldUtils.readStaticField(Villager.class, "cC", true);

            // Add custom memories
            ImmutableList.Builder<MemoryModuleType<?>> customMemoryTypes = new ImmutableList.Builder<MemoryModuleType<?>>()
                    .addAll(DEFAULT_MEMORY_TYPES);
            for (VillagerMemory<?> memory : VillagerMemoryType.ALL_MEMORIES) {
                customMemoryTypes.add(memory.getMemoryModuleType());
            }

            // Add custom sensors
            ImmutableList.Builder<SensorType<? extends Sensor<Villager>>> customSensorTypes =
                    new ImmutableList.Builder<SensorType<? extends Sensor<Villager>>>()
                            .addAll(DEFAULT_SENSOR_TYPES);
            for (VillagerSensor<? extends BaseVillagerSensor> sensor : VillagerSensorType.ALL_SENSORS) {
                customSensorTypes.add(sensor.getSensorType());
            }

            return Brain.provider(customMemoryTypes.build(), customSensorTypes.build());
        } catch (IllegalAccessException e) {
            LogUtil.exception(e, "Encountered exception when creating custom villager brain!");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void refreshBrain(@NotNull ServerLevel level) {
        Brain<Villager> brain = this.getBrain();

        brain.stopAll(level, this);
        this.brain = brain.copyWithoutBehaviors();
        this.registerBrainGoals(this.getBrain());
    }

    private void addActivity(Brain<Villager> brain, Activity activity, CustomVillagerBehaviorPackages.BehaviorContainer container) {
        brain.addActivity(activity, container.behaviors());
        this.activityBehaviorListMap.put(activity, container.customBehaviors());
    }

    private void addActivityWithConditions(Brain<Villager> brain, Activity activity, CustomVillagerBehaviorPackages.BehaviorContainer container,
                                           Set<Pair<MemoryModuleType<?>, MemoryStatus>> conditions) {
        brain.addActivityWithConditions(activity, container.behaviors(), conditions);
        this.activityBehaviorListMap.put(activity, container.customBehaviors());
    }

    /**
     * Core components copied from parent class
     */
    private void registerBrainGoals(Brain<Villager> brain) {
        VillagerProfession profession = this.getVillagerData().getProfession();

        // Register activities & behaviors
        this.addActivity(brain, Activity.CORE, CustomVillagerBehaviorPackages.getCorePackage(profession, 0.5F));
        this.addActivity(brain, Activity.IDLE, CustomVillagerBehaviorPackages.getIdlePackage(profession, 0.5F));

        if (this.isBaby()) {
            // If baby, register PLAY activities
            brain.addActivity(Activity.PLAY, CustomVillagerBehaviorPackages.getPlayPackage(0.5F));
        } else {
            // Otherwise, register WORK activities if job site is present
            this.addActivityWithConditions(brain, Activity.WORK, CustomVillagerBehaviorPackages.getWorkPackage(profession, 0.5F),
                    ImmutableSet.of(Pair.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT)));
        }

        // Register meet activities if meeting point is present
        this.addActivityWithConditions(brain, Activity.MEET, CustomVillagerBehaviorPackages.getMeetPackage(profession, 0.5F),
                Set.of(Pair.of(MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT)));

        // Register other activities
        brain.addActivity(Activity.REST, CustomVillagerBehaviorPackages.getRestPackage(profession, 0.5F));
        brain.addActivity(Activity.PANIC, CustomVillagerBehaviorPackages.getPanicPackage(profession, 0.5F));
        brain.addActivity(Activity.PRE_RAID, CustomVillagerBehaviorPackages.getPreRaidPackage(profession, 0.5F));
        brain.addActivity(Activity.RAID, CustomVillagerBehaviorPackages.getRaidPackage(profession, 0.5F));
        brain.addActivity(Activity.HIDE, CustomVillagerBehaviorPackages.getHidePackage(profession, 0.5F));

        // Set schedule
        if (this.isBaby()) {
            brain.setSchedule(Schedule.VILLAGER_BABY);
        } else {
            brain.setSchedule(Schedule.VILLAGER_DEFAULT);
        }

        // Configure activities
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.setActiveActivityIfPossible(Activity.IDLE);
        brain.updateActivityFromSchedule(this.level.getDayTime(), this.level.getGameTime());
    }

    /*
     * Interaction methods
     */

    /**
     * Returns a predicate that determines whether the wolves that this villager owns should pick up an item or not
     * - only professions that can own wolves will be supported
     */
    public Predicate<ItemEntity> getFetchableItemsPredicate() {
        VillagerProfession profession = this.getProfession();
        return itemEntity -> {
            if (itemEntity == null)
                return false;
            ItemStack item = itemEntity.getItem();
            return WolfFetchItemConfig.getInstance().wantsItem(profession, CraftItemStack.asBukkitCopy(item).getType());
        };
    }

    /**
     * Returns a predicate that determines whether the villager want an item when trading with another villager
     */
    public Predicate<ItemEntity> getTradeItemsPredicate() {
        VillagerProfession profession = this.getProfession();
        return itemEntity -> {
            if (itemEntity == null)
                return false;
            ItemStack item = itemEntity.getItem();
            if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) {
                // TODO: do they want anything? potentially food?
            } else if (profession == VillagerProfession.ARMORER) {

            } else if (profession == VillagerProfession.BUTCHER) {

            } else if (profession == VillagerProfession.CARTOGRAPHER) {

            } else if (profession == VillagerProfession.CLERIC) {

            } else if (profession == VillagerProfession.FARMER) {

            } else if (profession == VillagerProfession.FISHERMAN) {

            } else if (profession == VillagerProfession.FLETCHER) {

            } else if (profession == VillagerProfession.LEATHERWORKER) {

            } else if (profession == VillagerProfession.LIBRARIAN) {

            } else if (profession == VillagerProfession.MASON) {

            } else if (profession == VillagerProfession.SHEPHERD) {

            } else if (profession == VillagerProfession.TOOLSMITH) {

            } else if (profession == VillagerProfession.WEAPONSMITH) {

            }

            // If no early returns, return false
            return false;
        };
    }

    /**
     * Receive an item from another entity
     * - e.g. from a tamed wolf fetching an item
     *
     * @return whether the receiving is successful
     */
    public boolean receiveItem(@Nonnull ItemEntity item) {
        if (!item.isAlive())
            return false;

        // Add item to inventory
        if (this.getCustomInventory().addItem(CraftItemStack.asBukkitCopy(item.getItem())).isEmpty()) {
            // Display item pick-up animation
            this.take(item, item.getItem().getCount());

            // Remove the item entity
            item.remove(RemovalReason.DISCARDED);

            // Item is accepted
            return true;
        } else {
            // Inventory is full, don't pick up
            return false;
        }
    }

    /*
     * Villager friendship methods
     */
    public float getFriendshipTowards(@Nonnull BaseVillager villager) {
        // TODO: implement friendship system
        // TODO: potential range = [-1, 1] where + is friendly and - is hostile?
        return 0;
    }

    /**
     * Calculates the price modifier towards another villager based on their friendship
     * - this number will be multiplied to the price of the trade offer
     *
     * @param buyer the buying villager
     * @return the price modifier towards the buyer villager
     */
    public float getPriceModifierTowardsVillager(@Nonnull BaseVillager buyer) {
        // TODO: check friendship towards the buyer & return appropriate price modifier
        return 1f;
    }

    /*
     * Emerald balance shortcut methods
     */
    public int getEmeraldBalance() {
        return Objects.requireNonNull(VillagerMemoryType.EMERALD_BALANCE.get(this.getBrain()));
    }

    public void setEmeraldBalance(int amount) {
        VillagerMemoryType.EMERALD_BALANCE.set(this.getBrain(), amount);
    }

    public boolean canAfford(int amount) {
        return this.getEmeraldBalance() >= amount;
    }

    public void depositEmeralds(int amount) {
        this.setEmeraldBalance(this.getEmeraldBalance() + amount);
    }

    public void withdrawEmeralds(int amount) {
        // TODO: do we want to throw an exception if the villager can't afford it?
        // TODO: or do we want to allow the villager to go into debt?
        this.setEmeraldBalance(this.getEmeraldBalance() - amount);
    }

    /*
     * Internal trading methods
     */
    public int getStock(@Nonnull Material material) {
        // TODO: check if the villager is actually selling the material, not just present in the inventory
        return this.getCustomInventory().count(material);
    }

    public int evaluatePrice(@Nonnull Material material) {
        // TODO: change to actual price evaluation
        return 3;
    }

    /*
     * Misc methods
     */
    public VillagerProfession getProfession() {
        return this.getVillagerData().getProfession();
    }

    public org.bukkit.inventory.ItemStack getProfessionGuiItem() {
        VillagerProfession profession = this.getProfession();
        return new ItemStackBuilder(PROFESSION_MATERIAL_MAP.get(profession))
                .setDisplayName("&e&lProfession")
                .setLore(
                        "&f%s".formatted(profession == VillagerProfession.NONE ? "Unemployed" : StringUtil.toTitleCase(profession.name())),
                        "&7Expertise: %s".formatted(VillagerUtil.getExpertiseName(this.getExpertiseLevel(), true))
                )
                .build();
    }

    public int getExpertiseLevel() {
        return this.getVillagerData().getLevel();
    }

    /**
     * Returns the type of the villager based on the biome it spawned in
     */
    public VillagerType getVillagerBiomeType() {
        return this.getVillagerData().getType();
    }

    /**
     * Returns a short description of this villager, primarily used in hover messages
     */
    public List<String> getHoverDescription() {
        String professionDetails = this.getProfession().name();
        if (this.getProfession() == VillagerProfession.NONE) {
            professionDetails = "unemployed";
        }
        professionDetails = "%s %s".formatted(VillagerUtil.getExpertiseName(this.getExpertiseLevel(), false), professionDetails);

        return List.of(
                "Entity type: %s".formatted(ENTITY_TYPE),
                "Profession: %s".formatted(StringUtil.toTitleCase(professionDetails)),
                "UUID: %s".formatted(this.getStringUUID())
        );
    }

    /**
     * Sets the item that the villager is holding
     *
     * @param item the item to hold
     */
    public void setHeldItem(ItemStack item) {
        this.setItemSlot(EquipmentSlot.MAINHAND, item);
        this.setDropChance(EquipmentSlot.MAINHAND, 0f);
    }

    /**
     * Removes the item that the villager is holding
     */
    public void clearHeldItem() {
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    public static double getActualEyeHeight() {
        return 1.5;
    }

}
