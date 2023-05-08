package dev.breeze.settlements.entities.villagers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import dev.breeze.settlements.entities.villagers.behaviors.*;
import dev.breeze.settlements.entities.villagers.behaviors.farmer.HarvestSugarcaneBehavior;
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
        BaseVillagerBehavior temp;

        if (CAN_OPEN_FENCE_GATE.contains(profession)) {
            coreBehaviors.add(Pair.of(0, new InteractWithFenceGate()));
        }

        // Eat meals behavior
        temp = new EatAtMealTimeBehavior();
        coreBehaviors.add(Pair.of(4, temp));
        customBehaviors.add(temp);

        // Drink water behavior
        temp = new DrinkWaterBehavior();
        coreBehaviors.add(Pair.of(4, temp));
        customBehaviors.add(temp);

        // Scan for pets behavior
        coreBehaviors.add(Pair.of(20, ScanForPetsBehaviorController.scanForPets()));

        // TODO: refactor this into other activities
        if (profession == VillagerProfession.NITWIT) {
            ArrayList<Pair<? extends BehaviorControl<? super Villager>, Integer>> nitwitBehaviors = new ArrayList<>();
            nitwitBehaviors.add(Pair.of(new LaunchFireworkBehavior(), 1));
            nitwitBehaviors.add(Pair.of(new ThrowSnowballBehavior(), 1));
            nitwitBehaviors.add(Pair.of(new RingBellBehavior(), 1));
            nitwitBehaviors.add(Pair.of(new RunAroundBehavior(), 1));

            // Add to core behaviors
            coreBehaviors.add(Pair.of(30, new RunOne<>(nitwitBehaviors)));
        }

        // Return behaviors
        return new BehaviorContainer(ImmutableList.copyOf(coreBehaviors), customBehaviors);
    }

    /**
     * Work activity behaviors
     */
    public static BehaviorContainer getWorkPackage(VillagerProfession profession, float speed) {
        ArrayList<Pair<? extends BehaviorControl<? super Villager>, Integer>> workBehaviors = new ArrayList<>();

        // Add default behaviors
        workBehaviors.addAll(List.of(
                Pair.of(profession == VillagerProfession.FARMER ? new WorkAtComposter() : new WorkAtPoi(), 7),
                Pair.of(StrollAroundPoi.create(MemoryModuleType.JOB_SITE, STROLL_SPEED_MODIFIER, 4), 2),
                Pair.of(StrollToPoi.create(MemoryModuleType.JOB_SITE, STROLL_SPEED_MODIFIER, 1, 10), 5),
                Pair.of(StrollToPoiList.create(MemoryModuleType.SECONDARY_JOB_SITE, speed, 1, 6, MemoryModuleType.JOB_SITE), 5),
                Pair.of(new HarvestFarmland(), profession == VillagerProfession.FARMER ? 2 : 5),
                Pair.of(new UseBonemeal(), profession == VillagerProfession.FARMER ? 4 : 7)
        ));

        // Assign custom work behaviors based on profession
        List<BaseVillagerBehavior> customBehaviors = new ArrayList<>();
        BaseVillagerBehavior temp;

        int customGoalWeight = 10;
        if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) {
            // Unreachable code, because villager does not have job site
        } else if (profession == VillagerProfession.ARMORER) {
            temp = new RepairIronGolemBehavior();
            workBehaviors.add(Pair.of(temp, customGoalWeight));
            customBehaviors.add(temp);
        } else if (profession == VillagerProfession.BUTCHER) {
            temp = new TameWolfBehavior();
            workBehaviors.add(Pair.of(temp, customGoalWeight));
            customBehaviors.add(temp);

            temp = new BreedAnimalsBehavior(Set.of(EntityType.COW, EntityType.PIG, EntityType.RABBIT));
            workBehaviors.add(Pair.of(temp, customGoalWeight));
            customBehaviors.add(temp);

            temp = new ButcherAnimalsBehavior(Map.of(
                    EntityType.COW, 3,
                    EntityType.SHEEP, 5,
                    EntityType.CHICKEN, 3,
                    EntityType.PIG, 2,
                    EntityType.RABBIT, 2
            ));
            workBehaviors.add(Pair.of(temp, customGoalWeight));
            customBehaviors.add(temp);
        } else if (profession == VillagerProfession.CARTOGRAPHER) {

        } else if (profession == VillagerProfession.CLERIC) {

        } else if (profession == VillagerProfession.FARMER) {
            temp = new HarvestSugarcaneBehavior();
            workBehaviors.add(Pair.of(temp, customGoalWeight));
            customBehaviors.add(temp);

            temp = new TameWolfBehavior();
            workBehaviors.add(Pair.of(temp, customGoalWeight));
            customBehaviors.add(temp);

            temp = new TameCatBehavior();
            workBehaviors.add(Pair.of(temp, customGoalWeight));
            customBehaviors.add(temp);

            temp = new BreedAnimalsBehavior(Set.of(EntityType.CHICKEN));
            workBehaviors.add(Pair.of(temp, customGoalWeight));
            customBehaviors.add(temp);
        } else if (profession == VillagerProfession.FISHERMAN) {
            temp = new TameCatBehavior();
            workBehaviors.add(Pair.of(temp, customGoalWeight));
            customBehaviors.add(temp);

            temp = new FishingBehavior();
            workBehaviors.add(Pair.of(temp, customGoalWeight));
            customBehaviors.add(temp);
        } else if (profession == VillagerProfession.FLETCHER) {
            // TODO: pluck feather from chicken
            temp = new BreedAnimalsBehavior(Set.of(EntityType.CHICKEN));
            workBehaviors.add(Pair.of(temp, customGoalWeight));
            customBehaviors.add(temp);
        } else if (profession == VillagerProfession.LEATHERWORKER) {
            temp = new TameWolfBehavior();
            workBehaviors.add(Pair.of(temp, customGoalWeight));
            customBehaviors.add(temp);

            temp = new BreedAnimalsBehavior(Set.of(EntityType.COW));
            workBehaviors.add(Pair.of(temp, customGoalWeight));
            customBehaviors.add(temp);
        } else if (profession == VillagerProfession.LIBRARIAN) {
            temp = new EnchantItemBehavior();
            workBehaviors.add(Pair.of(temp, customGoalWeight));
            customBehaviors.add(temp);
        } else if (profession == VillagerProfession.MASON) {

        } else if (profession == VillagerProfession.SHEPHERD) {
            temp = new TameWolfBehavior();
            workBehaviors.add(Pair.of(temp, customGoalWeight));
            customBehaviors.add(temp);

            temp = new ShearSheepBehavior();
            workBehaviors.add(Pair.of(temp, customGoalWeight));
            customBehaviors.add(temp);

            temp = new BreedAnimalsBehavior(Set.of(EntityType.SHEEP));
            workBehaviors.add(Pair.of(temp, customGoalWeight));
            customBehaviors.add(temp);
        } else if (profession == VillagerProfession.TOOLSMITH) {
            temp = new RepairIronGolemBehavior();
            workBehaviors.add(Pair.of(temp, customGoalWeight));
            customBehaviors.add(temp);
        } else if (profession == VillagerProfession.WEAPONSMITH) {
            temp = new RepairIronGolemBehavior();
            workBehaviors.add(Pair.of(temp, customGoalWeight));
            customBehaviors.add(temp);
        }

        ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> behaviors = ImmutableList.of(
                getMinimalLookBehavior(),
                Pair.of(2, CustomSetWalkTargetFromBlockMemory.create(MemoryModuleType.JOB_SITE, speed, 9, 100, 1200)),
                Pair.of(3, new GiveGiftToHero(100)),
                Pair.of(5, new RunOne<>(workBehaviors)),
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
        ArrayList<Pair<? extends BehaviorControl<? super Villager>, Integer>> customMeetBehaviors = new ArrayList<>();

        // Add custom behaviors
        List<BaseVillagerBehavior> customBehaviors = new ArrayList<>();
        BaseVillagerBehavior temp;

        // Feed wolf behavior
        if (profession == VillagerProfession.BUTCHER) {
            temp = new FeedWolfBehavior();
            customMeetBehaviors.add(Pair.of(temp, 1));
            customBehaviors.add(temp);
        }

        // Tame wolf behavior
        if (profession == VillagerProfession.SHEPHERD || profession == VillagerProfession.FARMER || profession == VillagerProfession.LEATHERWORKER
                || profession == VillagerProfession.BUTCHER) {
            temp = new TameWolfBehavior();
            customMeetBehaviors.add(Pair.of(temp, 1));
            customBehaviors.add(temp);
        }

        // Tame cat behavior
        if (profession == VillagerProfession.FARMER || profession == VillagerProfession.FISHERMAN) {
            temp = new TameCatBehavior();
            customMeetBehaviors.add(Pair.of(temp, 1));
            customBehaviors.add(temp);
        }

        // Throw healing potion behavior
        if (profession == VillagerProfession.CLERIC) {
            temp = new ThrowHealingPotionBehavior();
            customMeetBehaviors.add(Pair.of(temp, 1));
            customBehaviors.add(temp);
        }

        // Default behaviors
        ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> behaviors = ImmutableList.of(
                getFullLookBehavior(),
                Pair.of(1, new RunOne<>(customMeetBehaviors)),
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

        // Add custom behaviors
        BaseVillagerBehavior temp;

        // Custom wolf-related behaviors
        if (profession == VillagerProfession.SHEPHERD || profession == VillagerProfession.FARMER || profession == VillagerProfession.LEATHERWORKER
                || profession == VillagerProfession.BUTCHER) {
            // Add parallel-running behaviors
            temp = new WalkDogBehavior();
            idleBehaviors.add(Pair.of(1, temp));
            customBehaviors.add(temp);

            // Add choice behaviors
            for (BaseVillagerBehavior behavior : List.of(
                    new TameWolfBehavior(),
                    new WashWolfBehavior()
            )) {
                idleChoiceBehaviors.add(Pair.of(behavior, 10));
                customBehaviors.add(behavior);
            }
        }

        // Tame cat behavior (cat should be resting now, so no other behaviors)
        if (profession == VillagerProfession.FARMER || profession == VillagerProfession.FISHERMAN) {
            temp = new TameCatBehavior();
            idleChoiceBehaviors.add(Pair.of(temp, 10));
            customBehaviors.add(temp);
        }

        // Add choice behaviors
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
