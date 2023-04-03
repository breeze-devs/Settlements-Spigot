package dev.breeze.settlements.entities.villagers.behaviors;

import com.mojang.authlib.GameProfile;
import dev.breeze.settlements.entities.fishing_hook.EmptyNetworkHandler;
import dev.breeze.settlements.entities.fishing_hook.EmptyNetworkManager;
import dev.breeze.settlements.entities.fishing_hook.VillagerFishingHook;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.utils.PacketUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R2.CraftServer;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.event.entity.CreatureSpawnEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FishingBehavior extends InteractAtTargetBehavior {

    private static final ItemStack FISHING_ROD = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.FISHING_ROD).build());

    /**
     * How long should the villager wait after a successful catch before casting again
     */
    private static final int MAX_RECAST_COOLDOWN = TimeUtil.ticks(10);

    /**
     * How close should the villager be to the water before casting
     */
    private static final int MAX_DISTANCE_FROM_WATER = 5;
    private static final double MAX_DISTANCE_FROM_WATER_SQUARED = Math.pow(MAX_DISTANCE_FROM_WATER, 2);

    @Nullable
    private ServerPlayer fakePlayer;
    @Nullable
    private VillagerFishingHook hook;
    @Nullable
    private BlockPos targetWater;

    private int recastCooldown;

    public FishingBehavior() {
        // Preconditions to this behavior
        super(Map.of(
                        // The villager should have seen water nearby
                        VillagerMemoryType.NEAREST_WATER_AREA, MemoryStatus.VALUE_PRESENT
                ), TimeUtil.seconds(30), 0,
                TimeUtil.minutes(2), MAX_DISTANCE_FROM_WATER_SQUARED,
                5, 5,
                TimeUtil.seconds(20), TimeUtil.minutes(1));

        this.fakePlayer = null;
        this.hook = null;
        this.targetWater = null;

        this.recastCooldown = 0;
    }

    @Override
    protected boolean scan(ServerLevel level, Villager villager) {
        return villager.getBrain().hasMemoryValue(VillagerMemoryType.NEAREST_WATER_AREA);
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.start(level, villager, gameTime);
        if (!(villager instanceof BaseVillager baseVillager))
            return;

        // Disable default walking
        baseVillager.setDefaultWalkTargetDisabled(true);

        this.targetWater = baseVillager.getBrain().getMemory(VillagerMemoryType.NEAREST_WATER_AREA).get();
        this.fakePlayer = createFakePlayer(baseVillager);
    }

    @Override
    protected boolean checkExtraCanStillUseConditions(ServerLevel level, Villager villager, long gameTime) {
        // TODO confirm that water is water
        return true;
    }

    @Override
    protected void tickExtra(ServerLevel level, Villager villager, long gameTime) {
        if (!(villager instanceof BaseVillager baseVillager))
            return;

        // Look at the water
        if (this.targetWater != null)
            baseVillager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.targetWater));

        if (this.fakePlayer != null)
            adjustPlayer(this.fakePlayer, baseVillager);
    }

    @Override
    protected void navigateToTarget(ServerLevel level, Villager villager, long gameTime) {
        if (this.targetWater != null)
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetWater, 0.5F, MAX_DISTANCE_FROM_WATER));
    }

    @Override
    protected void interactWithTarget(ServerLevel level, Villager villager, long gameTime) {
        // We are close enough to the water, start fishing
        if (this.hook != null && this.hook.isAlive()) {
            // Hook is "active", we are fishing
            return;
        }

        // Refresh hook on a cooldown
        if (--this.recastCooldown > 0)
            return;

        this.recastCooldown = MAX_RECAST_COOLDOWN;
        if (villager instanceof BaseVillager baseVillager && this.fakePlayer != null && this.targetWater != null)
            this.hook = new VillagerFishingHook(baseVillager, this.fakePlayer, this.targetWater);
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.stop(level, villager, gameTime);

        // Remove hook
        if (this.hook != null) {
            this.hook.stopFishing();
            this.hook = null;
        }

        // Remove fake player
        if (this.fakePlayer != null) {
            // Send entity removal packet to clients
            ClientboundRemoveEntitiesPacket removalPacket = new ClientboundRemoveEntitiesPacket(IntList.of(this.fakePlayer.getId()));
            PacketUtil.sendPacketToAllPlayers(removalPacket);

            // Remove fake player from world
            this.fakePlayer.remove(Entity.RemovalReason.DISCARDED);
            this.fakePlayer = null;
        }

        this.targetWater = null;
        this.recastCooldown = 0;

        // Enable default walk target setting
        if (villager instanceof BaseVillager baseVillager)
            baseVillager.setDefaultWalkTargetDisabled(false);
    }

    @Override
    protected boolean hasTarget() {
        return true;
    }

    @Override
    protected boolean isTargetReachable(Villager villager) {
        if (this.targetWater == null)
            return false;
        return villager.distanceToSqr(this.targetWater.getX(), this.targetWater.getY(), this.targetWater.getZ()) < MAX_DISTANCE_FROM_WATER_SQUARED;
    }

    public static ServerPlayer createFakePlayer(@Nonnull BaseVillager villager) {
        // Create fake player
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        ServerPlayer fakePlayer = new ServerPlayer(server, ((ServerLevel) villager.level), new GameProfile(UUID.randomUUID(), "FishingRod"));

        // Set connection
        EmptyNetworkManager conn = new EmptyNetworkManager(PacketFlow.CLIENTBOUND);
        fakePlayer.connection = new EmptyNetworkHandler(server, conn, fakePlayer);

        // Add entity
        fakePlayer.setInvisible(true);
        fakePlayer.collides = false;
        fakePlayer.noPhysics = true;
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, FISHING_ROD);
        fakePlayer.setInvulnerable(true);
        adjustPlayer(fakePlayer, villager);
        villager.level.addFreshEntity(fakePlayer, CreatureSpawnEvent.SpawnReason.CUSTOM);

        // Add player to player list
        ClientboundPlayerInfoUpdatePacket infoUpdatePacket = new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                fakePlayer);
        PacketUtil.sendPacketToAllPlayers(infoUpdatePacket);

        // Spawn player
        ClientboundAddPlayerPacket addPlayerPacket = new ClientboundAddPlayerPacket(fakePlayer);
        PacketUtil.sendPacketToAllPlayers(addPlayerPacket);

        // Remove player from player list
        ClientboundPlayerInfoRemovePacket infoRemovePacket = new ClientboundPlayerInfoRemovePacket(List.of(fakePlayer.getUUID()));
        PacketUtil.sendPacketToAllPlayers(infoRemovePacket);
        return fakePlayer;
    }

    private static void adjustPlayer(@Nonnull ServerPlayer fakePlayer, @Nonnull BaseVillager villager) {
        fakePlayer.moveTo(villager.getX(), villager.getY(), villager.getZ(), villager.yBodyRot, villager.getXRot());
    }

}

