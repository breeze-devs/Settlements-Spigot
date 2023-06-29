package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.utils.BlockPosUtil;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class CustomSetWalkTargetFromBlockMemory {

    public static OneShot<Villager> create(MemoryModuleType<GlobalPos> destination, float speed, int completionRange, int maxDistance, int maxRunTime) {
        return BehaviorBuilder.create((context) -> context.group(context.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE),
                context.absent(MemoryModuleType.WALK_TARGET),
                context.present(destination)).apply(context, (cantReachWalkTargetSince, walkTarget, destinationResult) -> (world, entity, time) -> {

            // Added custom clause of disabling automatic setting of walk target
            if (entity instanceof BaseVillager baseVillager && baseVillager.isDefaultWalkTargetDisabled())
                return false;

            // Code copied from SetWalkTargetFromBlockMemory class
            GlobalPos globalPos = context.get(destinationResult);
            Optional<Long> optional = context.tryGet(cantReachWalkTargetSince);
            if (globalPos.dimension() == world.dimension() && (optional.isEmpty() || world.getGameTime() - optional.get() <= (long) maxRunTime)) {
                if (globalPos.pos().distManhattan(entity.blockPosition()) > maxDistance) {
                    Vec3 vec3 = null;
                    int l = 0;
                    int m = 1000;

                    while (vec3 == null || BlockPosUtil.fromVec3(vec3).distManhattan(entity.blockPosition()) > maxDistance) {
                        vec3 = DefaultRandomPos.getPosTowards(entity, 15, 7, Vec3.atBottomCenterOf(globalPos.pos()), (float) Math.PI / 2F);
                        ++l;
                        if (l == m) {
                            entity.releasePoi(destination);
                            destinationResult.erase();
                            cantReachWalkTargetSince.set(time);
                            return true;
                        }
                    }

                    walkTarget.set(new WalkTarget(vec3, speed, completionRange));
                } else if (globalPos.pos().distManhattan(entity.blockPosition()) > completionRange) {
                    walkTarget.set(new WalkTarget(globalPos.pos(), speed, completionRange));
                }
            } else {
                entity.releasePoi(destination);
                destinationResult.erase();
                cantReachWalkTargetSince.set(time);
            }

            return true;
        }));
    }

}
