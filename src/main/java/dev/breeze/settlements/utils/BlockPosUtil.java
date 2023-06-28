package dev.breeze.settlements.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

public class BlockPosUtil {

    @Nonnull
    public static Vec3i vec3ToVec3i(@Nonnull Vec3 vec3) {
        return new Vec3i(((int) vec3.x), ((int) vec3.y), ((int) vec3.z));
    }

    @Nonnull
    public static Vec3 vec3iToVec3(@Nonnull Vec3i vec3i) {
        return new Vec3(vec3i.getX(), vec3i.getY(), vec3i.getZ());
    }

    @Nonnull
    public static BlockPos fromVec3(@Nonnull Vec3 vec3) {
        return new BlockPos(vec3ToVec3i(vec3));
    }

}
