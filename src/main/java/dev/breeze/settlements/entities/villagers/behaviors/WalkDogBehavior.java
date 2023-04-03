package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.entities.wolves.behaviors.WolfWalkBehavior;
import dev.breeze.settlements.utils.PacketUtil;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nonnull;
import java.util.Map;

public final class WalkDogBehavior extends BaseVillagerBehavior {

    private static final float SPEED_MODIFIER = 0.6F;

    private VillagerWolf cachedWolf;

    public WalkDogBehavior() {
        super(Map.of(
                // The villager should own a wolf
                VillagerMemoryType.OWNED_DOG, MemoryStatus.VALUE_PRESENT,
                // A dog must be present to walk
                VillagerMemoryType.WALK_DOG_TARGET, MemoryStatus.VALUE_PRESENT
        ), WolfWalkBehavior.MAX_WALK_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditionsRateLimited(@Nonnull ServerLevel level, @Nonnull Villager villager) {
        return villager.getBrain().hasMemoryValue(VillagerMemoryType.WALK_DOG_TARGET);
    }

    @Override
    protected boolean canStillUse(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        return villager.getBrain().hasMemoryValue(VillagerMemoryType.WALK_DOG_TARGET);
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.start(level, villager, gameTime);

        // Disable default walk target setting
        if (villager instanceof BaseVillager baseVillager)
            baseVillager.setDefaultWalkTargetDisabled(true);

        // Get wolf to walk
        VillagerWolf villagerWolf = villager.getBrain().getMemory(VillagerMemoryType.WALK_DOG_TARGET).get();
        this.cachedWolf = villagerWolf;

        // Clear relevant memories
        villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);

        // Follow the wolf
        villager.getNavigation().stop();
        villager.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, villagerWolf);
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(villagerWolf, SPEED_MODIFIER, 3));

        // Send leash packet (we don't want to actually leash the wolf)
        ClientboundSetEntityLinkPacket packet = new ClientboundSetEntityLinkPacket(villagerWolf, villager);
        PacketUtil.sendPacketToAllPlayers(packet);
    }

    @Override
    protected void tick(@Nonnull ServerLevel level, Villager villager, long gameTime) {
        PathNavigation navigation = villager.getNavigation();
        if (!navigation.isDone())
            return;

        // Follow the wolf
        VillagerWolf villagerWolf = villager.getBrain().getMemory(VillagerMemoryType.WALK_DOG_TARGET).get();
        villager.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, villagerWolf);
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(villagerWolf, SPEED_MODIFIER, 3));
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.stop(level, villager, gameTime);

        // Enable default walk target setting
        if (villager instanceof BaseVillager baseVillager)
            baseVillager.setDefaultWalkTargetDisabled(false);

        if (this.cachedWolf != null) {
            // Detach leash
            ClientboundSetEntityLinkPacket packet = new ClientboundSetEntityLinkPacket(this.cachedWolf, null);
            PacketUtil.sendPacketToAllPlayers(packet);
        }
    }

}

