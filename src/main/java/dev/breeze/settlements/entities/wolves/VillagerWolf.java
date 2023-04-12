package dev.breeze.settlements.entities.wolves;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.wolves.behaviors.WolfChaseSheepBehavior;
import dev.breeze.settlements.entities.wolves.behaviors.WolfFetchItemBehavior;
import dev.breeze.settlements.entities.wolves.behaviors.WolfSitBehaviorController;
import dev.breeze.settlements.entities.wolves.behaviors.WolfWalkBehavior;
import dev.breeze.settlements.entities.wolves.goals.WolfFollowOwnerGoal;
import dev.breeze.settlements.entities.wolves.goals.WolfLookLockGoal;
import dev.breeze.settlements.entities.wolves.goals.WolfMovementLockGoal;
import dev.breeze.settlements.entities.wolves.goals.WolfSitWhenOrderedToGoal;
import dev.breeze.settlements.entities.wolves.memories.WolfMemoryType;
import dev.breeze.settlements.entities.wolves.sensors.WolfSensorType;
import dev.breeze.settlements.utils.LogUtil;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.UpdateActivityFromSchedule;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.entity.schedule.ScheduleBuilder;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class VillagerWolf extends Wolf {

    public static final String ENTITY_TYPE = "settlements_wolf";

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
    public VillagerWolf(@Nonnull EntityType<? extends Wolf> entityType, @Nonnull Level level) {
        super(EntityType.WOLF, level);
        this.init();
    }

    /**
     * Constructor to spawn the villager in via plugin
     */
    public VillagerWolf(@Nonnull Location location) {
        super(EntityType.WOLF, ((CraftWorld) location.getWorld()).getHandle());
        this.setPos(location.getX(), location.getY(), location.getZ());
        if (!this.level.addFreshEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM)) {
            throw new IllegalStateException("Failed to add custom wolf to world");
        }

        this.init();
    }

    private void init() {
        // TODO: improve navigation to ignore fences
        this.navigation = new WolfNavigation(this, this.level);

        // Configure pathfinder goals
        this.initGoals();

        // If not "tamed" already
        if (this.getOwnerUUID() == null) {
            // Set wolf to be tamed by a "null" UID
            this.setTame(true);
            this.setOwnerUUID(null);
            this.setCollarColor(DyeColor.WHITE);
        }

        // Set step height to 1.5 (able to cross fences)
        this.maxUpStep = 1.5F;

        this.behaviorsRegisteredSuccessfully = false;
        this.stopFollowOwner = false;
        this.lookLocked = false;
        this.movementLocked = false;
    }

    /**
     * Called before world load to build the entity type
     */
    public static EntityType.Builder<Entity> getEntityTypeBuilder() {
        return EntityType.Builder.of(VillagerWolf::new, MobCategory.CREATURE)
                .sized(0.6F, 0.85F)
                .clientTrackingRange(10);
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        LogUtil.info("Loading custom wolf...");
        WolfMemoryType.load(nbt, this);
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

        // Save custom memories
        WolfMemoryType.save(nbt, this);
    }

    private void initGoals() {
        // Remove selected default goals
        this.goalSelector.removeAllGoals((goal -> goal instanceof SitWhenOrderedToGoal || goal instanceof FollowOwnerGoal));
        // Add replacement goals
        this.goalSelector.addGoal(2, new WolfSitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(6, new WolfFollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));

        // Add look-lock goal (prevent wolf from looking away in other goals)
        this.goalSelector.addGoal(1, new WolfLookLockGoal(this));
        // Add movement-lock goal (prevent wolf from moving in other goals)
        this.goalSelector.addGoal(1, new WolfMovementLockGoal(this));

        // Add target to all mobs that are hostile to villagers
        List.of(Zombie.class, Pillager.class, Vindicator.class, Vex.class, Witch.class, Evoker.class, Illusioner.class, Ravager.class)
                .forEach(clazz -> this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, clazz, true)));
    }

    /*
     * Brain-related methods
     */
    @Override
    @Nonnull
    public Brain<Wolf> getBrain() {
        return (Brain<Wolf>) super.getBrain();
    }

    @Override
    @Nonnull
    protected Brain.Provider<Wolf> brainProvider() {
        ImmutableList<MemoryModuleType<?>> memoryTypes = new ImmutableList.Builder<MemoryModuleType<?>>()
                .add(WolfMemoryType.NEARBY_ITEMS)
                .add(WolfMemoryType.NEARBY_SNIFFABLE_ENTITIES)
                .add(WolfMemoryType.RECENTLY_SNIFFED_ENTITIES)
                .add(WolfMemoryType.NEAREST_FENCE_AREA)
                .add(WolfMemoryType.NEARBY_SHEEP)
                .build();
        ImmutableList<SensorType<? extends Sensor<Wolf>>> sensorTypes = new ImmutableList.Builder<SensorType<? extends Sensor<Wolf>>>()
                .add(WolfSensorType.OWNER)
                .add(WolfSensorType.NEARBY_ITEMS)
                .add(WolfSensorType.NEARBY_SNIFFABLE_ENTITIES)
                .add(WolfSensorType.NEAREST_FENCE_AREA)
                .add(WolfSensorType.NEARBY_SHEEP)
                .build();
        return Brain.provider(memoryTypes, sensorTypes);
    }

    private void refreshBrain(@Nonnull ServerLevel level) {
        Brain<Wolf> brain = this.getBrain();

        brain.stopAll(level, this);
        this.brain = brain.copyWithoutBehaviors();
        this.registerBrainGoals(this.getBrain());
    }

    @Override
    @Nonnull
    protected Brain<?> makeBrain(@Nonnull Dynamic<?> dynamic) {
        Brain<Wolf> brain = this.brainProvider().makeBrain(dynamic);

        // Note: owner might not have loaded now, so this registration might fail
        // - which is acceptable because we have the owner sensor as a fallback
        this.registerBrainGoals(brain);

        return brain;
    }

    @Override
    public void tick() {
        super.tick();

        // TODO: edit this?
        this.getBrain().tick((ServerLevel) this.level, this);
    }

    /**
     * Register behaviors for a tamed VillagerWolf
     * - if the owner is null or dead, the wolf will not have any brain behaviors
     */
    private void registerBrainGoals(Brain<Wolf> brain) {
        if (this.getOwner() == null || !this.getOwner().isAlive()) {
            LogUtil.warning("Skipping registration of brain goals because owner is not available");
            return;
        }

        LogUtil.info("Registering wolf brain goals");

        // Set activity schedule
        // TODO: do we need to refine this?
        Schedule wolfSchedule = new ScheduleBuilder(new Schedule())
                .changeActivityAt(10, Activity.IDLE) // villager idle
                .changeActivityAt(2000, Activity.WORK) // villager work
                .changeActivityAt(9000, Activity.IDLE) // villager meet
                .changeActivityAt(11000, Activity.PLAY) // villager idle
                .changeActivityAt(12000, Activity.REST) // villager rest
                .build();
        brain.setSchedule(wolfSchedule);

        // Core behaviors
        brain.addActivity(Activity.CORE, new ImmutableList.Builder<Pair<Integer, BehaviorControl<? super Wolf>>>()
                .build());

        // Idle behaviors
        brain.addActivity(Activity.IDLE, new ImmutableList.Builder<Pair<Integer, BehaviorControl<? super Wolf>>>()
                .add(Pair.of(2, WolfSitBehaviorController.stand()))
                .add(Pair.of(99, UpdateActivityFromSchedule.create()))
                .build());

        // Work behaviors
        List<Pair<? extends BehaviorControl<? super Wolf>, Integer>> workBehaviors = new ArrayList<>();
        workBehaviors.add(Pair.of(new WolfFetchItemBehavior(this.getOwner().getFetchableItemsPredicate()), 10));
        if (this.getOwner().getProfession() == VillagerProfession.SHEPHERD)
            workBehaviors.add(Pair.of(new WolfChaseSheepBehavior(), 10));

        brain.addActivity(Activity.WORK, new ImmutableList.Builder<Pair<Integer, BehaviorControl<? super Wolf>>>()
                .add(Pair.of(2, WolfSitBehaviorController.stand()))
                .add(Pair.of(3, new RunOne<>(workBehaviors)))
                .add(Pair.of(99, UpdateActivityFromSchedule.create()))
                .build());

        // Play behaviors
        brain.addActivity(Activity.PLAY, new ImmutableList.Builder<Pair<Integer, BehaviorControl<? super Wolf>>>()
                .add(Pair.of(2, WolfSitBehaviorController.stand()))
                .add(Pair.of(3, new WolfWalkBehavior()))
                .add(Pair.of(99, UpdateActivityFromSchedule.create()))
                .build());

        // Rest behaviors
        brain.addActivity(Activity.REST, new ImmutableList.Builder<Pair<Integer, BehaviorControl<? super Wolf>>>()
                .add(Pair.of(2, WolfSitBehaviorController.sit()))
                .add(Pair.of(99, UpdateActivityFromSchedule.create()))
                .build());

        // Set important activities
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.setActiveActivityIfPossible(Activity.IDLE);

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
        Entity entity = this.level.getMinecraftWorld().getEntity(this.getOwnerUUID());
        if (!(entity instanceof BaseVillager villager))
            return null;

        return villager;
    }

    public void tameByVillager(@Nonnull BaseVillager villager) {
        this.setOwnerUUID(villager.getUUID());
        this.setCollarColor(DyeColor.LIME);
    }

    public void onOwnerSensorTick() {
        // Check if we've already registered the behaviors
        if (this.behaviorsRegisteredSuccessfully) {
            return;
        }

        LogUtil.info("Owner detected, refreshing wolf brain goals!");
        this.refreshBrain(this.level.getMinecraftWorld());
    }


    /*
     * Custom navigation for wolves to step over fences
     */
    public static class WolfNavigation extends GroundPathNavigation {

        public WolfNavigation(Mob entity, Level world) {
            super(entity, world);
        }

        @Override
        @Nonnull
        protected PathFinder createPathFinder(int range) {
            this.nodeEvaluator = new WolfNodeEvaluator();
            return new PathFinder(this.nodeEvaluator, range);
        }

    }

    /*
     * Custom node evaluator for wolves to step over fences
     */
    private static class WolfNodeEvaluator extends WalkNodeEvaluator {

        // Copied from parent because it's private
        private final Object2BooleanMap<AABB> collisionCache = new Object2BooleanOpenHashMap<>();

        @Override
        public void done() {
            this.collisionCache.clear();
            super.done();
        }

        /**
         * Mostly copied over from the parent class
         * - with some minor refactoring
         * - and one additional block to allow "passage" through fence gates
         */
        @Override
        @Nullable
        protected Node findAcceptedNode(int x, int y, int z, int maxYStep, double prevFeetY, @Nonnull Direction direction,
                                        @Nonnull BlockPathTypes prevNodeType) {
            Node node = null;
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
            double d = this.getFloorLevel(mutableBlockPos.set(x, y, z));
            if (d - prevFeetY > this.getMobJumpHeight()) {
                return null;
            }
            BlockPathTypes currNodeType = this.getCachedBlockType(this.mob, x, y, z);
            float penalty = this.mob.getPathfindingMalus(currNodeType);
            double e = (double) this.mob.getBbWidth() / 2.0D;
            if (penalty >= 0.0F) {
                node = this.getNodeAndUpdateCostToMax(x, y, z, currNodeType, penalty);
            }

            if (doesBlockHavePartialCollision(prevNodeType) && node != null && node.costMalus >= 0.0F && !this.canReachWithoutCollision(node)) {
                node = null;
            }

            if (currNodeType != BlockPathTypes.WALKABLE && (!this.isAmphibious() || currNodeType != BlockPathTypes.WATER)) {
                if ((node == null || node.costMalus < 0.0F) && maxYStep > 0 && (currNodeType != BlockPathTypes.FENCE || this.canWalkOverFences()) && currNodeType != BlockPathTypes.UNPASSABLE_RAIL && currNodeType != BlockPathTypes.TRAPDOOR && currNodeType != BlockPathTypes.POWDER_SNOW) {
                    node = this.findAcceptedNode(x, y + 1, z, maxYStep - 1, prevFeetY, direction, prevNodeType);
                    if (node != null && (node.type == BlockPathTypes.OPEN || node.type == BlockPathTypes.WALKABLE) && this.mob.getBbWidth() < 1.0F) {
                        double g = (double) (x - direction.getStepX()) + 0.5D;
                        double h = (double) (z - direction.getStepZ()) + 0.5D;
                        AABB aABB = new AABB(g - e, this.getFloorLevel(mutableBlockPos.set(g, y + 1, h)) + 0.001D, h - e, g + e,
                                (double) this.mob.getBbHeight() + this.getFloorLevel(mutableBlockPos.set(node.x, node.y, node.z)) - 0.002D, h + e);
                        if (this.hasCollisions(aABB)) {
                            node = null;
                        }
                    }
                }

                if (!this.isAmphibious() && currNodeType == BlockPathTypes.WATER && !this.canFloat()) {
                    if (this.getCachedBlockType(this.mob, x, y - 1, z) != BlockPathTypes.WATER) {
                        return node;
                    }

                    while (y > this.mob.level.getMinBuildHeight()) {
                        --y;
                        currNodeType = this.getCachedBlockType(this.mob, x, y, z);
                        if (currNodeType != BlockPathTypes.WATER) {
                            return node;
                        }

                        node = this.getNodeAndUpdateCostToMax(x, y, z, currNodeType, this.mob.getPathfindingMalus(currNodeType));
                    }
                }

                if (currNodeType == BlockPathTypes.OPEN) {
                    int i = 0;
                    int j = y;

                    while (currNodeType == BlockPathTypes.OPEN) {
                        --y;
                        if (y < this.mob.level.getMinBuildHeight()) {
                            return this.getBlockedNode(x, j, z);
                        }

                        if (i++ >= this.mob.getMaxFallDistance()) {
                            return this.getBlockedNode(x, y, z);
                        }

                        currNodeType = this.getCachedBlockType(this.mob, x, y, z);
                        penalty = this.mob.getPathfindingMalus(currNodeType);
                        if (currNodeType != BlockPathTypes.OPEN && penalty >= 0.0F) {
                            node = this.getNodeAndUpdateCostToMax(x, y, z, currNodeType, penalty);
                            break;
                        }

                        if (penalty < 0.0F) {
                            return this.getBlockedNode(x, y, z);
                        }
                    }
                }

                // >> Custom code begin -- recognize fences as walkable
                // - aka allows the wolf to pathfind on top of fences
                if (node == null && currNodeType == BlockPathTypes.FENCE) {
                    node = this.getNode(x, y + 1, z);
                    node.type = BlockPathTypes.FENCE;
                    node.costMalus = 2;
                }
                // << Custom code ends

                if (doesBlockHavePartialCollision(currNodeType) && node == null) {
                    node = this.getNode(x, y, z);
                    node.closed = true;
                    node.type = currNodeType;
                    node.costMalus = currNodeType.getMalus();
                }

            }
            return node;
        }

        /**
         * Copied from the parent class
         */
        private double getMobJumpHeight() {
            return Math.max(1.125D, this.mob.maxUpStep);
        }

        /**
         * Copied from the parent class
         */
        private Node getNodeAndUpdateCostToMax(int x, int y, int z, BlockPathTypes type, float penalty) {
            Node node = this.getNode(x, y, z);
            node.type = type;
            node.costMalus = Math.max(node.costMalus, penalty);
            return node;
        }

        /**
         * Copied from the parent class
         */
        private static boolean doesBlockHavePartialCollision(BlockPathTypes nodeType) {
            return nodeType == BlockPathTypes.FENCE || nodeType == BlockPathTypes.DOOR_WOOD_CLOSED || nodeType == BlockPathTypes.DOOR_IRON_CLOSED;
        }

        /**
         * Copied from the parent class
         */
        private boolean canReachWithoutCollision(Node node) {
            AABB aABB = this.mob.getBoundingBox();
            Vec3 vec3 = new Vec3((double) node.x - this.mob.getX() + aABB.getXsize() / 2.0D,
                    (double) node.y - this.mob.getY() + aABB.getYsize() / 2.0D,
                    (double) node.z - this.mob.getZ() + aABB.getZsize() / 2.0D);
            int i = Mth.ceil(vec3.length() / aABB.getSize());
            vec3 = vec3.scale(1.0F / (float) i);

            for (int j = 1; j <= i; ++j) {
                aABB = aABB.move(vec3);
                if (this.hasCollisions(aABB)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Copied from the parent class
         */
        private boolean hasCollisions(AABB box) {
            return this.collisionCache.computeIfAbsent(box, (box2) -> !this.level.noCollision(this.mob, box));
        }

        /**
         * Copied from the parent class
         */
        private Node getBlockedNode(int x, int y, int z) {
            Node node = this.getNode(x, y, z);
            node.type = BlockPathTypes.BLOCKED;
            node.costMalus = -1.0F;
            return node;
        }

    }

}
