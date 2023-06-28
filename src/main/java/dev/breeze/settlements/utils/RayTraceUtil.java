package dev.breeze.settlements.utils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class RayTraceUtil {

    /**
     * Mostly copied from paper
     */
    @Nullable
    public static RayTraceResult getTargetEntity(Entity source, int maxDistance) {
        if (maxDistance < 1 || maxDistance > 120) {
            throw new IllegalArgumentException("maxDistance must be between 1-120");
        }

        Vec3 start = source.getEyePosition(1.0F);
        Vec3 direction = source.getLookAngle();
        Vec3 end = start.add(direction.x * maxDistance, direction.y * maxDistance, direction.z * maxDistance);

        List<Entity> entityList = source.level().getEntities(source, source.getBoundingBox()
                .expandTowards(direction.x * maxDistance, direction.y * maxDistance, direction.z * maxDistance)
                .inflate(1.0D, 1.0D, 1.0D), EntitySelector.NO_SPECTATORS.and(Entity::isPickable));

        double distance = 0.0D;
        RayTraceResult result = null;

        for (Entity entity : entityList) {
            final double inflationAmount = entity.getPickRadius();
            AABB aabb = entity.getBoundingBox().inflate(inflationAmount, inflationAmount, inflationAmount);
            Optional<Vec3> rayTraceResult = aabb.clip(start, end);

            if (rayTraceResult.isPresent()) {
                Vec3 rayTrace = rayTraceResult.get();
                double distanceTo = start.distanceToSqr(rayTrace);
                if (distanceTo < distance || distance == 0.0D) {
                    result = new RayTraceResult(entity, distanceTo);
                    distance = distanceTo;
                }
            }
        }

        return result;
    }

    public record RayTraceResult(Entity entity, double distance) {
    }

}
