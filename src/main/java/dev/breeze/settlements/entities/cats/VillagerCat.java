package dev.breeze.settlements.entities.cats;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import dev.breeze.settlements.entities.cats.behaviors.CatFetchOrEatBehavior;
import dev.breeze.settlements.entities.cats.behaviors.CatFollowOwnerBehaviorController;
import dev.breeze.settlements.entities.cats.behaviors.CatSleepBehavior;
import dev.breeze.settlements.entities.cats.goals.CatFollowOwnerGoal;
import dev.breeze.settlements.entities.cats.goals.CatLookLockGoal;
import dev.breeze.settlements.entities.cats.goals.CatMovementLockGoal;
import dev.breeze.settlements.entities.cats.goals.CatSitWhenOrderedToGoal;
import dev.breeze.settlements.entities.cats.memories.CatMemoryType;
import dev.breeze.settlements.entities.cats.sensors.CatSensorType;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.utils.DebugUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.UpdateActivityFromSchedule;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.entity.schedule.ScheduleBuilder;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class VillagerCat extends Cat {

    public static final String ENTITY_TYPE = "settlements_cat";

    /**
     * Are all behaviors (including owner-related ones) registered successfully?
     */
    private boolean behaviorsRegisteredSuccessfully;

    @Getter
    @Setter
    private boolean stopFollowOwner;

    @Getter
    @Setter
    private boolean lookLocked;
    @Getter
    @Setter
    private boolean movementLocked;

    /**
     * Constructor called when Minecraft tries to load the entity
     */
    public VillagerCat(@Nonnull EntityType<? extends Wolf> entityType, @Nonnull Level level) {
        super(EntityType.CAT, level);
        this.init();
    }

    /**
     * Constructor to spawn the cat in via plugin
     */
    public VillagerCat(@Nonnull Location location) {
        super(EntityType.CAT, ((CraftWorld) location.getWorld()).getHandle());
        this.setPos(location.getX(), location.getY(), location.getZ());
        if (!this.level().addFreshEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM)) {
            throw new IllegalStateException("Failed to add custom cat to world");
        }

        this.init();
    }

    private void init() {
        // Use the same navigation as wolves to ignore fences
//        this.navigation = new VillagerWolf.WolfNavigation(this, this.level);

        // Configure pathfinder goals
        this.initGoals();

        // If not "tamed" already
        if (this.getOwnerUUID() == null) {
            // Set cat to be tamed by a "null" UID
            this.setTame(true);
            this.setOwnerUUID(null);
            this.setCollarColor(DyeColor.WHITE);
        }

        // Set step height to 1.5 (able to cross fences)
        this.setMaxUpStep(1.5F);

        this.behaviorsRegisteredSuccessfully = false;

        this.stopFollowOwner = false;
        this.lookLocked = false;
        this.movementLocked = false;
    }

    /**
     * Called before world load to build the entity type
     */
    public static EntityType.Builder<Entity> getEntityTypeBuilder() {
        return EntityType.Builder.of(VillagerCat::new, MobCategory.CREATURE)
                .sized(0.6F, 0.7F)
                .clientTrackingRange(8);
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        DebugUtil.log("Loading custom cat (%s)", this.getUUID().toString());

        // TODO: restore any NBT data if needed
    }

    @Override
    public boolean save(@Nonnull CompoundTag nbt) {
        DebugUtil.log("Saving custom cat (%s)", this.getUUID().toString());
        return super.save(nbt);
    }

    /**
     * Saves villager data to the NBT tag
     */
    @Override
    public void addAdditionalSaveData(@Nonnull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);

        // IMPORTANT: save as custom ID to persist this entity
        nbt.putString("id", "minecraft:" + ENTITY_TYPE);
        nbt.putString("Plugin", "Settlements");
    }

    private void initGoals() {
        // Remove selected default goals
        this.goalSelector.removeAllGoals((goal -> goal instanceof SitWhenOrderedToGoal || goal instanceof FollowOwnerGoal));
        // Add replacement goals
        this.goalSelector.addGoal(1, new CatSitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(6, new CatFollowOwnerGoal(this, 1.0D, 10.0F, 8.0F, false));

        // Add look-lock goal (prevent cat from looking away in other goals)
        this.goalSelector.addGoal(1, new CatLookLockGoal(this));
        // Add movement-lock goal (prevent cat from moving in other goals)
        this.goalSelector.addGoal(1, new CatMovementLockGoal(this));

        // Add target to all fishes (helps the villager kill fish)
        List.of(Cod.class, Salmon.class, Pufferfish.class, TropicalFish.class)
                .forEach(clazz -> this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, clazz, true)));
    }

    /*
     * Brain-related methods
     */
    @Override
    @Nonnull
    @SuppressWarnings("unchecked")
    public Brain<Cat> getBrain() {
        return (Brain<Cat>) super.getBrain();
    }

    @Override
    @Nonnull
    protected Brain.Provider<Cat> brainProvider() {
        ImmutableList<MemoryModuleType<?>> memoryTypes = new ImmutableList.Builder<MemoryModuleType<?>>()
                .add(CatMemoryType.NEARBY_ITEMS)
                .build();
        ImmutableList<SensorType<? extends Sensor<Cat>>> sensorTypes = new ImmutableList.Builder<SensorType<? extends Sensor<Cat>>>()
                .add(CatSensorType.OWNER)
                .add(CatSensorType.NEARBY_ITEMS)
                .build();
        return Brain.provider(memoryTypes, sensorTypes);
    }

    private void refreshBrain(@Nonnull ServerLevel level) {
        Brain<Cat> brain = this.getBrain();

        brain.stopAll(level, this);
        this.brain = brain.copyWithoutBehaviors();
        this.registerBrainGoals(this.getBrain());
    }

    @Override
    @Nonnull
    protected Brain<?> makeBrain(@Nonnull Dynamic<?> dynamic) {
        Brain<Cat> brain = this.brainProvider().makeBrain(dynamic);

        // Note: owner might not have loaded now, so this registration might fail
        // - which is acceptable because we have the owner sensor as a fallback
        this.registerBrainGoals(brain);

        return brain;
    }

    @Override
    public void tick() {
        super.tick();

        // TODO: edit this?
        this.getBrain().tick((ServerLevel) this.level(), this);
    }

    /**
     * Register behaviors for a tamed VillagerCat
     * - if the owner is null or dead, the cat will not have any brain behaviors
     */
    private void registerBrainGoals(Brain<Cat> brain) {
        if (this.getOwner() == null || !this.getOwner().isAlive()) {
            DebugUtil.log("Skipping initialization of cat (%s) brain because owner is not available", this.getUUID().toString());
            return;
        }

        DebugUtil.log("Initializing brain for cat (%s)", this.getUUID().toString());

        // Set activity schedule
        // - cat is active from dusk (13000) to dawn (22000) & when villagers are working (2000-9000)
        Schedule catSchedule = new ScheduleBuilder(new Schedule())
                .changeActivityAt(2000, Activity.WORK) // villager work
                .changeActivityAt(9000, Activity.REST) // villager meet
                .changeActivityAt(13000, Activity.PLAY) // villager rest
                .changeActivityAt(22000, Activity.REST) // villager rest
                .build();
        brain.setSchedule(catSchedule);

        // Register behaviors
        brain.addActivity(Activity.CORE, new ImmutableList.Builder<Pair<Integer, BehaviorControl<? super Cat>>>()
                .build());
        brain.addActivity(Activity.WORK, new ImmutableList.Builder<Pair<Integer, BehaviorControl<? super Cat>>>()
                .add(Pair.of(2, new CatFetchOrEatBehavior()))
                .add(Pair.of(5, CatFollowOwnerBehaviorController.startFollowOwner()))
                .add(Pair.of(99, UpdateActivityFromSchedule.create()))
                .build());
        brain.addActivity(Activity.PLAY, new ImmutableList.Builder<Pair<Integer, BehaviorControl<? super Cat>>>()
                .add(Pair.of(2, new CatFetchOrEatBehavior()))
                .add(Pair.of(5, CatFollowOwnerBehaviorController.stopFollowOwner()))
                .add(Pair.of(99, UpdateActivityFromSchedule.create()))
                .build());
        brain.addActivity(Activity.REST, new ImmutableList.Builder<Pair<Integer, BehaviorControl<? super Cat>>>()
                .add(Pair.of(1, new CatSleepBehavior()))
//                .add(Pair.of(2, new CatWalkToBedBehavior()))
                .add(Pair.of(5, CatFollowOwnerBehaviorController.stopFollowOwner()))
                .add(Pair.of(99, UpdateActivityFromSchedule.create()))
                .build());

        // Set important activities
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.REST);
        brain.setActiveActivityIfPossible(Activity.REST);

        // Set flag
        this.behaviorsRegisteredSuccessfully = true;
    }

    @Override
    public boolean isTame() {
        return true;
    }

    @Override
    @Nullable
    public BaseVillager getOwner() {
        // Return null if no owner
        if (this.getOwnerUUID() == null)
            return null;

        // Try to get owner entity
        Entity entity = this.level().getMinecraftWorld().getEntity(this.getOwnerUUID());
        if (!(entity instanceof BaseVillager villager))
            return null;

        return villager;
    }

    public void tameByVillager(@Nonnull BaseVillager villager, CatVariant variant) {
        this.setOwnerUUID(villager.getUUID());
        this.setCollarColor(DyeColor.LIME);
        this.setVariant(variant);

        AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null)
            maxHealth.setBaseValue(20.0D);

        this.setHealth(this.getMaxHealth());
    }

    public void onOwnerSensorTick() {
        // Check if we've already registered the behaviors
        if (this.behaviorsRegisteredSuccessfully) {
            return;
        }

        // Check base condition
        if (this.getOwnerUUID() == null) {
            return;
        }

        DebugUtil.log("Owner (%s) detected, refreshing cat (%s) brain goals", this.getOwnerUUID().toString(), this.getUUID().toString());
        this.refreshBrain(this.level().getMinecraftWorld());
    }

    /**
     * Returns a short description of this villager cat, primarily used in hover messages
     */
    public List<String> getHoverDescription() {
        return List.of(
                "Entity type: %s".formatted(ENTITY_TYPE),
                "Owned by: %s".formatted(this.getOwner() == null ? "N/A" : this.getOwnerUUID().toString()),
                "UUID: %s".formatted(this.getStringUUID())
        );
    }

}
