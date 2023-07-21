package dev.breeze.settlements.entities.villagers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import dev.breeze.settlements.entities.villagers.behaviors.*;
import dev.breeze.settlements.entities.villagers.behaviors.farmer.HarvestSugarcaneBehavior;
import dev.breeze.settlements.entities.villagers.behaviors.farmer.MakeCakeBehavior;
import dev.breeze.settlements.entities.villagers.behaviors.fletcher.CollectArrowsBehavior;
import dev.breeze.settlements.entities.villagers.behaviors.fletcher.MakeTippedArrowsBehavior;
import dev.breeze.settlements.entities.villagers.behaviors.habitat.desert.DrinkWaterBehavior;
import dev.breeze.settlements.entities.villagers.behaviors.pranks.LaunchFireworkBehavior;
import dev.breeze.settlements.entities.villagers.behaviors.pranks.RingBellBehavior;
import dev.breeze.settlements.entities.villagers.behaviors.pranks.RunAroundBehavior;
import dev.breeze.settlements.entities.villagers.behaviors.pranks.ThrowSnowballBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.raid.Raid;

import java.util.*;

/**
 * Mostly copied from VillagerGoalPackages class
 * - custom behaviors are added
 */
public final class CustomVillagerBehaviorPackages {

    private static final float STROLL_SPEED_MODIFIER = 0.4F;

    /**
     * Villagers with the following professions can open or close fence gates
     */
    private static final Set<VillagerProfession> CAN_OPEN_FENCE_GATE = Set.of(VillagerProfession.BUTCHER, VillagerProfession.FARMER,
            VillagerProfession.FLETCHER, VillagerProfession.LEATHERWORKER, VillagerProfession.SHEPHERD);

    /**
     * Core behaviors
     */
    public static BehaviorContainer getCorePackage(VillagerProfession profession, float speed) {
        List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> coreBehaviors = new ArrayList<>();

        // Add default behaviors
        coreBehaviors.addAll(List.of(
                Pair.of(0, new Swim(0.8F)),
                Pair.of(0, InteractWithDoor.create()),
                Pair.of(0, new LookAtTargetSink(45, 90)),
                Pair.of(0, new VillagerPanicTrigger()),
                Pair.of(0, WakeUp.create()),
                Pair.of(0, ReactToBell.create()),
                Pair.of(0, SetRaidStatus.create()),
                Pair.of(0, ValidateNearbyPoi.create(profession.heldJobSite(), MemoryModuleType.JOB_SITE)),
                Pair.of(0, ValidateNearbyPoi.create(profession.acquirableJobSite(), MemoryModuleType.POTENTIAL_JOB_SITE)),
                Pair.of(1, new MoveToTargetSink()),
                Pair.of(2, PoiCompetitorScan.create()),
                Pair.of(3, new LookAndFollowTradingPlayerSink(speed)),
                Pair.of(5, GoToWantedItem.create(speed, false, 4)),
                Pair.of(6, AcquirePoi.create(profession.acquirableJobSite(), MemoryModuleType.JOB_SITE, MemoryModuleType.POTENTIAL_JOB_SITE, true,
                        Optional.empty())),
                Pair.of(7, new GoToPotentialJobSite(speed)),
                Pair.of(8, YieldJobSite.create(speed)),
                Pair.of(10, AcquirePoi.create((poiType) -> poiType.is(PoiTypes.HOME), MemoryModuleType.HOME, false, Optional.of((byte) 14))),
                Pair.of(10, AcquirePoi.create((poiType) -> poiType.is(PoiTypes.MEETING), MemoryModuleType.MEETING_POINT, true, Optional.of((byte) 14))),
                Pair.of(10, AssignProfessionFromJobSite.create()),
                Pair.of(10, ResetProfession.create())
        ));

        // Add custom behaviors
        List<BaseVillagerBehavior> customBehaviors = new ArrayList<>();

        // Open fence gate behavior (not added to GUI)
        if (CAN_OPEN_FENCE_GATE.contains(profession)) {
            coreBehaviors.add(Pair.of(0, new InteractWithFenceGate()));
        }

        // Eat meals behavior
        addBehavior(new EatAtMealTimeBehavior(), coreBehaviors, 4, customBehaviors);

        // Drink water behavior
        addBehavior(new DrinkWaterBehavior(), coreBehaviors, 4, customBehaviors);

        // Scan for pets behavior (not added to GUI)
        coreBehaviors.add(Pair.of(20, ScanForPetsBehaviorController.scanForPets()));

        // Return behaviors
        return new BehaviorContainer(ImmutableList.copyOf(coreBehaviors), customBehaviors);
    }

    /**
     * Work activity behaviors
     */
    public static BehaviorContainer getWorkPackage(VillagerProfession profession, float speed) {
        ArrayList<Pair<? extends BehaviorControl<? super Villager>, Integer>> workChoiceBehaviors = new ArrayList<>();

        // Add default behaviors
        workChoiceBehaviors.addAll(List.of(
                Pair.of(profession == VillagerProfession.FARMER ? new WorkAtComposter() : new WorkAtPoi(), 7),
                Pair.of(StrollAroundPoi.create(MemoryModuleType.JOB_SITE, STROLL_SPEED_MODIFIER, 4), 2),
                Pair.of(StrollToPoi.create(MemoryModuleType.JOB_SITE, STROLL_SPEED_MODIFIER, 1, 10), 5),
                Pair.of(StrollToPoiList.create(MemoryModuleType.SECONDARY_JOB_SITE, speed, 1, 6, MemoryModuleType.JOB_SITE), 5),
                Pair.of(new HarvestFarmland(), profession == VillagerProfession.FARMER ? 2 : 5),
                Pair.of(new UseBonemeal(), profession == VillagerProfession.FARMER ? 4 : 7)
        ));

        /*
         * Assign custom work behaviors based on profession
         */

        // Map of { behavior => weight of behavior }
        Map<BaseVillagerBehavior, Integer> customBehaviorWeightMap = new HashMap<>();
        int customGoalWeight = 10;

        if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) {
            // Unreachable code, because villager does not have job site
        } else if (profession == VillagerProfession.ARMORER) {
            customBehaviorWeightMap.put(new RepairIronGolemBehavior(), customGoalWeight);
        } else if (profession == VillagerProfession.BUTCHER) {
            customBehaviorWeightMap.put(new TameWolfBehavior(), customGoalWeight);
            customBehaviorWeightMap.put(new BreedAnimalsBehavior(Set.of(EntityType.COW, EntityType.PIG, EntityType.RABBIT)), customGoalWeight);
            customBehaviorWeightMap.put(new ButcherAnimalsBehavior(Map.of(
                    EntityType.COW, 3,
                    EntityType.SHEEP, 5,
                    EntityType.CHICKEN, 3,
                    EntityType.PIG, 2,
                    EntityType.RABBIT, 2
            )), customGoalWeight);
        } else if (profession == VillagerProfession.CARTOGRAPHER) {
            // TODO: add behavior
        } else if (profession == VillagerProfession.CLERIC) {
            // TODO: add behavior
        } else if (profession == VillagerProfession.FARMER) {
            customBehaviorWeightMap.put(new HarvestSugarcaneBehavior(), customGoalWeight);
            customBehaviorWeightMap.put(new TameWolfBehavior(), customGoalWeight);
            customBehaviorWeightMap.put(new TameCatBehavior(), customGoalWeight);
            customBehaviorWeightMap.put(new BreedAnimalsBehavior(Set.of(EntityType.CHICKEN)), customGoalWeight);
            customBehaviorWeightMap.put(new MakeCakeBehavior(), customGoalWeight);
        } else if (profession == VillagerProfession.FISHERMAN) {
            customBehaviorWeightMap.put(new TameCatBehavior(), customGoalWeight);
            customBehaviorWeightMap.put(new FishingBehavior(), customGoalWeight);
        } else if (profession == VillagerProfession.FLETCHER) {
            // TODO: pluck feather from chicken
            customBehaviorWeightMap.put(new CollectArrowsBehavior(), customGoalWeight);
            customBehaviorWeightMap.put(new MakeTippedArrowsBehavior(), customGoalWeight);
            customBehaviorWeightMap.put(new BreedAnimalsBehavior(Set.of(EntityType.CHICKEN)), customGoalWeight);
        } else if (profession == VillagerProfession.LEATHERWORKER) {
            customBehaviorWeightMap.put(new TameWolfBehavior(), customGoalWeight);
            customBehaviorWeightMap.put(new BreedAnimalsBehavior(Set.of(EntityType.COW)), customGoalWeight);
        } else if (profession == VillagerProfession.LIBRARIAN) {
            customBehaviorWeightMap.put(new EnchantItemBehavior(), customGoalWeight);
        } else if (profession == VillagerProfession.MASON) {
            // TODO: add behavior
        } else if (profession == VillagerProfession.SHEPHERD) {
            customBehaviorWeightMap.put(new TameWolfBehavior(), customGoalWeight);
            customBehaviorWeightMap.put(new ShearSheepBehavior(), customGoalWeight);
            customBehaviorWeightMap.put(new BreedAnimalsBehavior(Set.of(EntityType.SHEEP)), customGoalWeight);
        } else if (profession == VillagerProfession.TOOLSMITH) {
            customBehaviorWeightMap.put(new RepairIronGolemBehavior(), customGoalWeight);
        } else if (profession == VillagerProfession.WEAPONSMITH) {
            customBehaviorWeightMap.put(new RepairIronGolemBehavior(), customGoalWeight);
        }

        // Add custom behaviors
        List<BaseVillagerBehavior> customBehaviors = new ArrayList<>();
        for (Map.Entry<BaseVillagerBehavior, Integer> entry : customBehaviorWeightMap.entrySet()) {
            addChoiceBehavior(entry.getKey(), workChoiceBehaviors, entry.getValue(), customBehaviors);
        }

        ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> behaviors = ImmutableList.of(
                getMinimalLookBehavior(),
                Pair.of(2, CustomSetWalkTargetFromBlockMemory.create(MemoryModuleType.JOB_SITE, speed, 9, 100, 1200)),
                Pair.of(3, new GiveGiftToHero(100)),
                Pair.of(5, new RunOne<>(workChoiceBehaviors)),
                Pair.of(10, new ShowTradesToPlayer(400, 1600)),
                Pair.of(10, SetLookAndInteract.create(EntityType.PLAYER, 4)),
                Pair.of(99, UpdateActivityFromSchedule.create())
        );

        return new BehaviorContainer(behaviors, customBehaviors);
    }

    /**
     * Play activity behaviors
     * - only used in baby villagers
     */
    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getPlayPackage(float speed) {
        return ImmutableList.of(
                getFullLookBehavior(),
                Pair.of(0, new MoveToTargetSink(80, 120)),
                Pair.of(5, PlayTagWithOtherKids.create()),
                Pair.of(5, new RunOne<>(
                        ImmutableMap.of(MemoryModuleType.VISIBLE_VILLAGER_BABIES, MemoryStatus.VALUE_ABSENT),
                        ImmutableList.of(
                                Pair.of(InteractWith.of(EntityType.VILLAGER, 8, MemoryModuleType.INTERACTION_TARGET, speed, 2), 2),
                                Pair.of(InteractWith.of(EntityType.CAT, 8, MemoryModuleType.INTERACTION_TARGET, speed, 2), 1),
                                Pair.of(VillageBoundRandomStroll.create(speed), 1),
                                Pair.of(SetWalkTargetFromLookTarget.create(speed, 2), 1),
                                Pair.of(new JumpOnBed(speed), 2),
                                Pair.of(new DoNothing(20, 40), 2)
                        ))),
                Pair.of(99, UpdateActivityFromSchedule.create())
        );
    }

    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getRestPackage(VillagerProfession profession, float speed) {
        return ImmutableList.of(
                getMinimalLookBehavior(),
                Pair.of(2, CustomSetWalkTargetFromBlockMemory.create(MemoryModuleType.HOME, speed, 1, 150, 1200)),
                Pair.of(3, ValidateNearbyPoi.create((poiType) -> poiType.is(PoiTypes.HOME), MemoryModuleType.HOME)),
                Pair.of(3, new SleepInBed()),
                Pair.of(5, new RunOne<>(
                        ImmutableMap.of(MemoryModuleType.HOME, MemoryStatus.VALUE_ABSENT),
                        ImmutableList.of(
                                Pair.of(SetClosestHomeAsWalkTarget.create(speed), 1),
                                Pair.of(InsideBrownianWalk.create(speed), 4),
                                Pair.of(GoToClosestVillage.create(speed, 4), 2),
                                Pair.of(new DoNothing(20, 40), 2)
                        ))),
                Pair.of(99, UpdateActivityFromSchedule.create())
        );
    }

    public static BehaviorContainer getMeetPackage(VillagerProfession profession, float speed) {
        ArrayList<Pair<? extends BehaviorControl<? super Villager>, Integer>> customMeetChoiceBehaviors = new ArrayList<>();

        // Custom behavior container
        List<BaseVillagerBehavior> customBehaviors = new ArrayList<>();

        // Internal trade behavior (higher weight)
        addChoiceBehavior(new TradeItemsBehavior(), customMeetChoiceBehaviors, 5, customBehaviors);

        // Feed wolf behavior
        if (profession == VillagerProfession.BUTCHER) {
            addChoiceBehavior(new FeedWolfBehavior(), customMeetChoiceBehaviors, 1, customBehaviors);
        }

        // Tame wolf behavior
        if (profession == VillagerProfession.SHEPHERD || profession == VillagerProfession.FARMER || profession == VillagerProfession.LEATHERWORKER
                || profession == VillagerProfession.BUTCHER) {
            addChoiceBehavior(new TameWolfBehavior(), customMeetChoiceBehaviors, 1, customBehaviors);
        }

        // Tame cat behavior
        if (profession == VillagerProfession.FARMER || profession == VillagerProfession.FISHERMAN) {
            addChoiceBehavior(new TameCatBehavior(), customMeetChoiceBehaviors, 1, customBehaviors);
        }

        // Throw healing potion behavior
        if (profession == VillagerProfession.CLERIC) {
            addChoiceBehavior(new ThrowHealingPotionBehavior(), customMeetChoiceBehaviors, 1, customBehaviors);
        }

        // Nitwit behaviors
        if (profession == VillagerProfession.NITWIT) {
            addChoiceBehavior(new RingBellBehavior(), customMeetChoiceBehaviors, 1, customBehaviors);
            addChoiceBehavior(new LaunchFireworkBehavior(), customMeetChoiceBehaviors, 1, customBehaviors);
        }


        // Default behaviors
        ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> behaviors = ImmutableList.of(
                getFullLookBehavior(),
                Pair.of(1, new RunOne<>(customMeetChoiceBehaviors)),
                Pair.of(2, TriggerGate.triggerOneShuffled(ImmutableList.of(
                        Pair.of(StrollAroundPoi.create(MemoryModuleType.MEETING_POINT, STROLL_SPEED_MODIFIER, 40), 2),
                        Pair.of(SocializeAtBell.create(), 2)
                ))),
                Pair.of(2, CustomSetWalkTargetFromBlockMemory.create(MemoryModuleType.MEETING_POINT, speed, 6, 100, 200)),
                Pair.of(3, new GiveGiftToHero(100)),
                Pair.of(3, ValidateNearbyPoi.create((poiType) -> poiType.is(PoiTypes.MEETING), MemoryModuleType.MEETING_POINT)),
                Pair.of(3, new GateBehavior<>(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET), GateBehavior.OrderPolicy.ORDERED,
                        GateBehavior.RunningPolicy.RUN_ONE, ImmutableList.of(Pair.of(new TradeWithVillager(), 1)))),
                Pair.of(10, new ShowTradesToPlayer(400, 1600)),
                Pair.of(10, SetLookAndInteract.create(EntityType.PLAYER, 4)),
                Pair.of(99, UpdateActivityFromSchedule.create())
        );

        return new BehaviorContainer(behaviors, customBehaviors);
    }

    public static BehaviorContainer getIdlePackage(VillagerProfession profession, float speed) {
        List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> idleBehaviors = new ArrayList<>();
        List<BaseVillagerBehavior> customBehaviors = new ArrayList<>();

        idleBehaviors.addAll(List.of(
                getFullLookBehavior(),
                Pair.of(3, new GiveGiftToHero(100)),
                Pair.of(3, SetLookAndInteract.create(EntityType.PLAYER, 4)),
                Pair.of(3, new ShowTradesToPlayer(400, 1600)),
                Pair.of(3, new GateBehavior<>(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET), GateBehavior.OrderPolicy.ORDERED,
                        GateBehavior.RunningPolicy.RUN_ONE, ImmutableList.of(Pair.of(new TradeWithVillager(), 1)))),
                Pair.of(3, new GateBehavior<>(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.BREED_TARGET), GateBehavior.OrderPolicy.ORDERED,
                        GateBehavior.RunningPolicy.RUN_ONE, ImmutableList.of(Pair.of(new VillagerMakeLove(), 1)))),
                Pair.of(99, UpdateActivityFromSchedule.create())
        ));

        // Default behaviors that will be randomly chosen to run one
        ArrayList<Pair<? extends BehaviorControl<? super Villager>, Integer>> idleChoiceBehaviors = new ArrayList<>();
        idleChoiceBehaviors.addAll(List.of(
                Pair.of(InteractWith.of(EntityType.VILLAGER, 8, MemoryModuleType.INTERACTION_TARGET, speed, 2), 2),
                Pair.of(InteractWith.of(EntityType.VILLAGER, 8, AgeableMob::canBreed, AgeableMob::canBreed, MemoryModuleType.BREED_TARGET, speed, 2), 1),
                Pair.of(InteractWith.of(EntityType.CAT, 8, MemoryModuleType.INTERACTION_TARGET, speed, 2), 1),
                Pair.of(VillageBoundRandomStroll.create(speed), 1),
                Pair.of(SetWalkTargetFromLookTarget.create(speed, 2), 1),
                Pair.of(new JumpOnBed(speed), 1),
                Pair.of(new DoNothing(30, 60), 1)
        ));

        /*
         * Add custom behaviors
         */
        // Custom wolf-related behaviors
        if (profession == VillagerProfession.SHEPHERD || profession == VillagerProfession.FARMER || profession == VillagerProfession.LEATHERWORKER
                || profession == VillagerProfession.BUTCHER) {
            // Add parallel-running behaviors
            addBehavior(new WalkDogBehavior(), idleBehaviors, 1, customBehaviors);

            // Add choice behaviors
            for (BaseVillagerBehavior behavior : List.of(
                    new TameWolfBehavior(),
                    new WashWolfBehavior()
            )) {
                addChoiceBehavior(behavior, idleChoiceBehaviors, 10, customBehaviors);
            }
        }

        // Tame cat behavior (cat should be resting now, so no other behaviors)
        if (profession == VillagerProfession.FARMER || profession == VillagerProfession.FISHERMAN) {
            addChoiceBehavior(new TameCatBehavior(), idleChoiceBehaviors, 10, customBehaviors);
        }

        // Nitwit behaviors
        if (profession == VillagerProfession.NITWIT) {
            // Add parallel-running behaviors
            addBehavior(new LaunchFireworkBehavior(), idleBehaviors, 1, customBehaviors);
            addBehavior(new ThrowSnowballBehavior(), idleBehaviors, 1, customBehaviors);
            addBehavior(new RunAroundBehavior(), idleBehaviors, 1, customBehaviors);
        }

        // Add choice behaviors to Minecraft behaviors
        idleBehaviors.add(Pair.of(2, new RunOne<>(idleChoiceBehaviors)));

        return new BehaviorContainer(ImmutableList.copyOf(idleBehaviors), customBehaviors);
    }

    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getPanicPackage(VillagerProfession profession, float speed) {
        float panicSpeed = speed * 1.5F;
        return ImmutableList.of(
                getMinimalLookBehavior(),
                Pair.of(0, VillagerCalmDown.create()),
                Pair.of(1, SetWalkTargetAwayFrom.entity(MemoryModuleType.NEAREST_HOSTILE, panicSpeed, 6, false)),
                Pair.of(1, SetWalkTargetAwayFrom.entity(MemoryModuleType.HURT_BY_ENTITY, panicSpeed, 6, false)),
                Pair.of(3, VillageBoundRandomStroll.create(panicSpeed, 2, 2))
        );
    }

    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getPreRaidPackage(VillagerProfession profession, float speed) {
        float panicSpeed = speed * 1.5F;
        return ImmutableList.of(
                getMinimalLookBehavior(),
                Pair.of(0, RingBell.create()),
                Pair.of(0, TriggerGate.triggerOneShuffled(ImmutableList.of(
                        Pair.of(CustomSetWalkTargetFromBlockMemory.create(MemoryModuleType.MEETING_POINT, panicSpeed, 2, 150, 200), 6),
                        Pair.of(VillageBoundRandomStroll.create(panicSpeed), 2)
                ))),
                Pair.of(99, ResetRaidStatus.create())
        );
    }

    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getRaidPackage(VillagerProfession profession, float speed) {
        return ImmutableList.of(
                getMinimalLookBehavior(),
                Pair.of(0, BehaviorBuilder.sequence(
                        BehaviorBuilder.triggerIf(CustomVillagerBehaviorPackages::raidWon),
                        TriggerGate.triggerOneShuffled(ImmutableList.of(
                                Pair.of(MoveToSkySeeingSpot.create(speed), 5),
                                Pair.of(VillageBoundRandomStroll.create(speed * 1.1F), 2)
                        ))
                )),
                Pair.of(0, new CelebrateVillagersSurvivedRaid(600, 600)),
                Pair.of(2, BehaviorBuilder.sequence(
                        BehaviorBuilder.triggerIf(CustomVillagerBehaviorPackages::hasActiveRaid),
                        LocateHidingPlace.create(24, speed * 1.4F, 1)
                )),
                Pair.of(99, ResetRaidStatus.create())
        );
    }

    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getHidePackage(VillagerProfession profession, float speed) {
        return ImmutableList.of(
                getMinimalLookBehavior(),
                Pair.of(0, SetHiddenState.create(15, 3)),
                Pair.of(1, LocateHidingPlace.create(32, speed * 1.25F, 2))
        );
    }

    /**
     * Usually used when the villager is "busy"
     */
    private static Pair<Integer, BehaviorControl<LivingEntity>> getMinimalLookBehavior() {
        return Pair.of(5, new RunOne<>(ImmutableList.of(
                Pair.of(SetEntityLookTarget.create(EntityType.VILLAGER, 8.0F), 2),
                Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 8.0F), 2),
                Pair.of(new DoNothing(30, 60), 8)
        )));
    }

    /**
     * Usually used when the villager is "free"
     */
    private static Pair<Integer, BehaviorControl<LivingEntity>> getFullLookBehavior() {
        return Pair.of(5, new RunOne<>(ImmutableList.of(
                Pair.of(SetEntityLookTarget.create(EntityType.CAT, 8.0F), 8),
                Pair.of(SetEntityLookTarget.create(EntityType.VILLAGER, 8.0F), 2),
                Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 8.0F), 2),
                Pair.of(SetEntityLookTarget.create(MobCategory.CREATURE, 8.0F), 1),
                Pair.of(SetEntityLookTarget.create(MobCategory.WATER_CREATURE, 8.0F), 1),
                Pair.of(SetEntityLookTarget.create(MobCategory.AXOLOTLS, 8.0F), 1),
                Pair.of(SetEntityLookTarget.create(MobCategory.UNDERGROUND_WATER_CREATURE, 8.0F), 1),
                Pair.of(SetEntityLookTarget.create(MobCategory.WATER_AMBIENT, 8.0F), 1),
                Pair.of(SetEntityLookTarget.create(MobCategory.MONSTER, 8.0F), 1),
                Pair.of(new DoNothing(30, 60), 2)
        )));
    }

    private static boolean hasActiveRaid(ServerLevel world, LivingEntity entity) {
        Raid raid = world.getRaidAt(entity.blockPosition());
        return raid != null && raid.isActive() && !raid.isVictory() && !raid.isLoss();
    }

    private static boolean raidWon(ServerLevel world, LivingEntity entity) {
        Raid raid = world.getRaidAt(entity.blockPosition());
        return raid != null && raid.isVictory();
    }

    /*
     * Utility methods
     */
    private static void addBehavior(BaseVillagerBehavior behavior,
                                    List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> minecraftBehaviors, int weight,
                                    List<BaseVillagerBehavior> settlementBehaviors) {
        minecraftBehaviors.add(Pair.of(weight, behavior));
        settlementBehaviors.add(behavior);
    }

    private static void addChoiceBehavior(BaseVillagerBehavior behavior,
                                          List<Pair<? extends BehaviorControl<? super Villager>, Integer>> choiceBehaviors, int weight,
                                          List<BaseVillagerBehavior> settlementBehaviors) {
        choiceBehaviors.add(Pair.of(behavior, weight));
        settlementBehaviors.add(behavior);
    }

    /**
     * Record used as return data structure
     *
     * @param behaviors       minecraft behaviors to be registered
     * @param customBehaviors custom behaviors (must be same instance)
     */
    public record BehaviorContainer(ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> behaviors,
                                    List<BaseVillagerBehavior> customBehaviors) {
    }

}
