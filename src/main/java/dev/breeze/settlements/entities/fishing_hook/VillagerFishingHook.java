package dev.breeze.settlements.entities.fishing_hook;

import dev.breeze.settlements.Main;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.CreatureSpawnEvent;

import javax.annotation.Nonnull;

public class VillagerFishingHook extends FishingHook {

    private final BaseVillager villager;

    private float fishComeFromAngle;
    private int waitCountdown;
    private int fishTravelCountdown;

    public VillagerFishingHook(@Nonnull BaseVillager villager, @Nonnull ServerPlayer fakePlayer, @Nonnull BlockPos waterPos) {
        super(EntityType.FISHING_BOBBER, villager.level());

        this.villager = villager;

        this.fishComeFromAngle = 0;
        this.waitCountdown = Mth.nextInt(this.random, TimeUtil.seconds(5), TimeUtil.seconds(20));
        this.fishTravelCountdown = 0;

        this.setOwner(fakePlayer);

        this.moveTo(waterPos.getCenter());

        // Add to world
        if (!this.level().addFreshEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM)) {
            throw new IllegalStateException("Failed to add custom villager to world");
        }

        // Play sound
        this.level().playSound(null, villager.getX(), villager.getEyeY(), villager.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F);
    }

    @Override
    public void tick() {
        // Don't directly call super tick, because that will remove this entity
        super.baseTick();

        // Safety check
        if (this.villager == null || !this.villager.isAlive()) {
            this.stopFishing();
            return;
        }

        // Do stuff
        float targetY = 0.0F;
        BlockPos pos = this.blockPosition();
        FluidState fluid = this.level().getFluidState(pos);

        if (fluid.is(FluidTags.WATER)) {
            targetY = fluid.getHeight(this.level(), pos);
        }

        boolean isInWater = targetY > 0.0F;

        if (this.currentState == FishHookState.FLYING) {
            // If we hooked an entity, stop fishing
            if (this.hookedIn != null) {
                this.stopFishing();
                return;
            }

            // If we are in water, set state to bobbing
            if (isInWater) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.3, 0.2, 0.3));
                this.currentState = FishHookState.BOBBING;
                return;
            }
        } else if (this.currentState == FishHookState.BOBBING) {
            // Calculate hook motion
            Vec3 velocity = this.getDeltaMovement();
            double deltaVelocityY = this.getY() + velocity.y - (double) pos.getY() - (double) targetY;
            if (Math.abs(deltaVelocityY) < 0.01D) {
                deltaVelocityY += Math.signum(deltaVelocityY) * 0.1D;
            }
            this.setDeltaMovement(velocity.x * 0.9D, velocity.y - deltaVelocityY * this.random.nextDouble() * 0.2, velocity.z * 0.9);

            if (isInWater && !this.level().isClientSide) {
                this.tickFish();
            }
        }

        if (!fluid.is(FluidTags.WATER)) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.03D, 0.0D));
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        this.updateRotation();

        // If we hooked onto a block (didn't hit water)
        if (this.currentState == FishHookState.FLYING && (this.onGround || this.horizontalCollision)) {
            this.setDeltaMovement(Vec3.ZERO);
            this.stopFishing();
        }

        double scale = 0.92D;
        this.setDeltaMovement(this.getDeltaMovement().scale(scale));
        this.reapplyPosition();
    }

    private void tickFish() {
        ServerLevel level = (ServerLevel) this.level();

        // Waiting for fish
        if (--this.waitCountdown > 0) {
            return;
        } else if (this.waitCountdown == 0) {
            // Set where the fish come from
            this.fishComeFromAngle = Mth.nextFloat(this.random, 0, 360);
            this.fishTravelCountdown = Mth.nextInt(this.random, TimeUtil.seconds(1), TimeUtil.seconds(4));
        }

        if (--this.fishTravelCountdown > 0) {
            // Fish is moving towards the hook
            this.fishComeFromAngle += (float) this.random.triangle(0.0D, 9.188D);
            float angle = this.fishComeFromAngle * 0.017453292F;
            float sinAngle = Mth.sin(angle);
            float cosAngle = Mth.cos(angle);
            double fishX = this.getX() + (double) (sinAngle * (float) this.fishTravelCountdown * 0.1F);
            double fishY = Mth.floor(this.getY()) + 1;
            double fishZ = this.getZ() + (double) (cosAngle * (float) this.fishTravelCountdown * 0.1F);
            BlockState fishBlockState = level.getBlockState(new BlockPos(((int) fishX), ((int) fishY) - 1, ((int) fishZ)));
            if (fishBlockState.is(Blocks.WATER)) {
                if (this.random.nextFloat() < 0.15F) {
                    level.sendParticles(ParticleTypes.BUBBLE, fishX, fishY - 0.1, fishZ, 1, sinAngle, 0.1D, cosAngle, 0.0D);
                }

                float f3 = sinAngle * 0.04F;
                float f4 = cosAngle * 0.04F;

                level.sendParticles(ParticleTypes.FISHING, fishX, fishY, fishZ, 0, f4, 0.01D, -f3, 1.0D);
                level.sendParticles(ParticleTypes.FISHING, fishX, fishY, fishZ, 0, -f4, 0.01D, f3, 1.0D);
            }
        } else if (this.fishTravelCountdown == 0) {
            // Fish has bitten the hook
            this.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.25F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
            double d3 = this.getY() + 0.5D;

            level.sendParticles(ParticleTypes.BUBBLE, this.getX(), d3, this.getZ(), (int) (1.0F + this.getBbWidth() * 20.0F), this.getBbWidth(),
                    0.0D, this.getBbWidth(), 0.2);
            level.sendParticles(ParticleTypes.FISHING, this.getX(), d3, this.getZ(), (int) (1.0F + this.getBbWidth() * 20.0F), this.getBbWidth(),
                    0.0D, this.getBbWidth(), 0.2);

            this.reelIn();
        }
    }

    private void reelIn() {
        // Spawn random fish
        double random = RandomUtil.RANDOM.nextDouble();
        AbstractFish fish;
        if (random < 0.6) {
            // 60% chance to spawn cod
            fish = new Cod(EntityType.COD, this.level());
        } else if (random < 0.85) {
            // 25% chance to spawn salmon
            fish = new Salmon(EntityType.SALMON, this.level());
        } else if (random < 0.95) {
            // 10% chance to spawn puffer fish
            fish = new Pufferfish(EntityType.PUFFERFISH, this.level());
        } else {
            // 5% chance to spawn tropical fish
            fish = new TropicalFish(EntityType.TROPICAL_FISH, this.level());
        }

        // Set fish to be flying out of the water at the villager
        fish.setPos(this.getX(), this.getY(), this.getZ());
        double dx = this.villager.getX() - this.getX();
        double dy = this.villager.getY() - this.getY();
        double dz = this.villager.getZ() - this.getZ();
        double scale = 0.3;
        fish.setDeltaMovement(dx * scale, dy * scale + 0.3, dz * scale);

        // Set to auto die after a while
        Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), () -> {
            if (!fish.isAlive())
                return;
            fish.hurt(fish.damageSources().drown(), Float.MAX_VALUE);
        }, TimeUtil.seconds(10));

        // Add entity to world
        this.level().addFreshEntity(fish);

        // Stop fishing since we've fished something up
        this.stopFishing();
    }

    public void stopFishing() {
        // TODO: something?
        // Remove hook entity
        this.discard();
    }

}
