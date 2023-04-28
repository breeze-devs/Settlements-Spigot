package dev.breeze.settlements.entities.villagers.behaviors;

import com.google.common.collect.Sets;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.SoundUtil;
import dev.breeze.settlements.utils.TimeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_19_R2.block.CraftBlock;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;

import javax.annotation.Nonnull;
import java.util.*;

public final class InteractWithFenceGate extends OneShot<Villager> {

    private static final int COOLDOWN_BEFORE_RERUNNING_IN_SAME_NODE = 20;
    private static final double SKIP_CLOSING_IF_FURTHER_THAN = 3.0D;
    private static final double MAX_DISTANCE_TO_HOLD_GATE_OPEN_FOR_OTHER_MOBS = 2.0D;

    private Node currentNode;
    private int cooldown;

    public InteractWithFenceGate() {
        this.currentNode = null;
        this.cooldown = 0;
    }


    @Override
    public boolean trigger(@Nonnull ServerLevel level, Villager villager, long time) {
        // Get current path
        Brain<Villager> brain = villager.getBrain();
        if (!brain.hasMemoryValue(MemoryModuleType.PATH))
            return false;
        Path path = brain.getMemory(MemoryModuleType.PATH).get();

        // Check path
        if (path.notStarted() || path.isDone())
            return false;

        // Check cooldown
        if (--this.cooldown > 0) {
            return false;
        }

        // Increment node if needed
        if (Objects.equals(path.getNextNode(), this.currentNode)) {
            this.cooldown = COOLDOWN_BEFORE_RERUNNING_IN_SAME_NODE;
        }
        this.currentNode = path.getNextNode();

        boolean triggered = false;

        // Check if the previous and next nodes are fence gates
        List<Node> toCheck = new ArrayList<>(Arrays.asList(path.getPreviousNode(), path.getNextNode()));

        // If applicable, add the next-next node to check
        if (path.getNextNodeIndex() + 1 < path.getNodeCount()) {
            toCheck.add(path.getNode(path.getNextNodeIndex() + 1));
        }

        for (Node node : toCheck) {
            if (node == null)
                continue;

            BlockPos pos = node.asBlockPos();
            BlockState state = level.getBlockState(pos);

            // Ignore if not a fence gate
            if (!isFenceGate(state))
                continue;

            // Check if the event is cancelled by another plugin
            boolean toCancel = this.fireEvent(villager, pos);
            if (toCancel)
                return false;

            // Open the fence gate & remember it
            if (!isFenceGateOpen(state)) {
                setFenceGateOpen(villager, level, state, pos, true);
                triggered = true;
            }
            rememberFenceGateToClose(level, villager, pos);
        }

        // Close relevant fence gates
        closeRelevantFenceGates(level, villager, toCheck);
        return triggered;
    }


    /**
     * Fires an entity interact event for when the villager opens a fence gate
     *
     * @return whether the event is cancelled
     */
    private boolean fireEvent(Villager villager, BlockPos position) {
        EntityInteractEvent event = new EntityInteractEvent(villager.getBukkitEntity(), CraftBlock.at(villager.level, position));
        villager.getLevel().getCraftServer().getPluginManager().callEvent(event);
        return event.isCancelled();
    }

    private static void rememberFenceGateToClose(ServerLevel level, Villager villager, BlockPos toClose) {
        GlobalPos globalpos = GlobalPos.of(level.dimension(), toClose);
        Brain<Villager> brain = villager.getBrain();

        // If there are no memory of fence gates, create new memory
        if (!brain.hasMemoryValue(VillagerMemoryType.FENCE_GATE_TO_CLOSE.getMemoryModuleType())) {
            Set<GlobalPos> memory = Sets.newHashSet(globalpos);
            VillagerMemoryType.FENCE_GATE_TO_CLOSE.set(brain, memory);
            return;
        }

        // Otherwise, add to the existing memory
        Set<GlobalPos> memory = VillagerMemoryType.FENCE_GATE_TO_CLOSE.get(brain);
        memory.add(globalpos);
    }

    public static void closeRelevantFenceGates(ServerLevel level, Villager villager, @Nonnull List<Node> nodes) {
        Brain<Villager> brain = villager.getBrain();

        // If there are no memory, ignore
        if (!brain.hasMemoryValue(VillagerMemoryType.FENCE_GATE_TO_CLOSE.getMemoryModuleType()))
            return;

        // Loop through each remembered position
        Set<GlobalPos> memory = VillagerMemoryType.FENCE_GATE_TO_CLOSE.get(brain);
        Iterator<GlobalPos> iterator = memory.iterator();
        while (iterator.hasNext()) {
            GlobalPos pos = iterator.next();
            BlockPos blockPos = pos.pos();

            // Don't close the gate if the nodes are "current"
            if (nodes.stream().anyMatch((node) -> node.asBlockPos().equals(blockPos))) {
                continue;
            }

            if (isFenceGateTooFarAway(level, villager, pos)) {
                iterator.remove();
                continue;
            }

            BlockState state = level.getBlockState(blockPos);
            if (!isFenceGate(state)) {
                iterator.remove();
                continue;
            }

            if (!isFenceGateOpen(state)) {
                iterator.remove();
                continue;
            }

            if (areOtherMobsComingThroughFenceGate(villager, blockPos, brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES))) {
                iterator.remove();
                continue;
            }

            // Close the fence gate
            setFenceGateOpen(villager, level, state, blockPos, false);
            iterator.remove();
        }
    }

    private static boolean areOtherMobsComingThroughFenceGate(Villager villager, BlockPos pos, Optional<List<LivingEntity>> otherMobs) {
        if (otherMobs.isEmpty())
            return false;

        List<LivingEntity> otherMobsList = otherMobs.get();
        for (LivingEntity otherMob : otherMobsList) {
            // Ignore all non-villager mobs coming through gates
            if (otherMob.getType() != EntityType.VILLAGER)
                continue;

            // Ignore all mobs far away from the gates
            if (!pos.closerToCenterThan(otherMob.position(), MAX_DISTANCE_TO_HOLD_GATE_OPEN_FOR_OTHER_MOBS))
                continue;

            // Check if any mob is coming through the gates
            // - short circuit if there is any
            if (isMobComingThroughFenceGate(otherMob.getBrain(), pos))
                return true;
        }
        return false;
    }

    private static boolean isMobComingThroughFenceGate(Brain<?> brain, BlockPos pos) {
        // If mob is not path finding, ignore
        if (!brain.hasMemoryValue(MemoryModuleType.PATH))
            return false;

        Path pathEntity = brain.getMemory(MemoryModuleType.PATH).get();

        // If mob's pathing is done, ignore
        if (pathEntity.isDone())
            return false;

        // Try to get path nodes
        Node previousNode = pathEntity.getPreviousNode();
        if (previousNode == null)
            return false;

        Node nextNode = pathEntity.getNextNode();
        return pos.equals(previousNode.asBlockPos()) || pos.equals(nextNode.asBlockPos());
    }

    private static boolean isFenceGateTooFarAway(ServerLevel world, LivingEntity entity, GlobalPos gatePosition) {
        return gatePosition.dimension() != world.dimension() || !gatePosition.pos().closerToCenterThan(entity.position(), SKIP_CLOSING_IF_FURTHER_THAN);
    }

    /*
     * Fence gate methods
     */
    private static boolean isFenceGate(BlockState state) {
        return state.is(BlockTags.FENCE_GATES, stateBase -> stateBase.getBlock() instanceof FenceGateBlock);
    }

    private static boolean isFenceGateOpen(BlockState blockState) {
        return blockState.getValue(FenceGateBlock.OPEN);
    }

    private static void setFenceGateOpen(@Nonnull Entity entity, Level level, BlockState state, BlockPos pos, boolean toOpen) {
        // Ignore if fence gate is already opened/closed
        if (isFenceGateOpen(state) == toOpen)
            return;

        // Change state
        level.setBlock(pos, state.setValue(FenceGateBlock.OPEN, toOpen), 10);

        // Play sound
        Location location = new Location(level.getWorld(), pos.getX(), pos.getY(), pos.getZ());
        Sound sound = toOpen ? Sound.BLOCK_FENCE_GATE_OPEN : Sound.BLOCK_FENCE_GATE_CLOSE;
        SoundUtil.playSoundPublic(location, sound, 1F, RandomUtil.RANDOM.nextFloat() * 0.1F + 0.9F);

        // Fire event
        level.gameEvent(entity, toOpen ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);

        if (!toOpen)
            return;

        // If open, apply slowness to all entities nearby briefly
        // - this is to prevent animals from walking out of the fence gate
        for (Animal nearby : level.getEntitiesOfClass(Animal.class, entity.getBoundingBox().inflate(7, 4, 7))) {
            if (nearby == null || !nearby.isAlive() || nearby instanceof Wolf)
                continue;
            nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TimeUtil.seconds(5), 20, false, false),
                    entity, EntityPotionEffectEvent.Cause.PLUGIN);
            nearby.getNavigation().stop();
        }
    }

}
