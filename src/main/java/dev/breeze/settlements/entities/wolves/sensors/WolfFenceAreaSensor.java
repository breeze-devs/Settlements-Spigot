package dev.breeze.settlements.entities.wolves.sensors;

import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.entities.wolves.memories.WolfMemoryType;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.TimeUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

public class WolfFenceAreaSensor extends Sensor<Wolf> {

    /**
     * How far away to scan for fence areas horizontally
     */
    private static final int RANGE_HORIZONTAL = 20;

    /**
     * How far away to scan for fence areas vertically
     */
    private static final int RANGE_VERTICAL = 4;

    /**
     * The minimum number of fences to be considered a "fenced area"
     */
    private static final int MIN_FENCE_COUNT = 16;
    /**
     * The maximum number of fences that can be detected in one area
     */
    private static final int MAX_FENCE_DEPTH = 100;

    /**
     * How often will the wolf scan for nearby fence areas
     * - should be infrequent as this operation may be expensive
     */
    private static final int SENSE_COOLDOWN = TimeUtil.minutes(3);

    private final Set<BlockPos> visited;

    public WolfFenceAreaSensor() {
        super(SENSE_COOLDOWN);
        this.visited = new HashSet<>();
    }

    @Override
    @Nonnull
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(WolfMemoryType.NEAREST_FENCE_AREA);
    }

    @Override
    protected void doTick(@Nonnull ServerLevel world, @Nonnull Wolf wolf) {
        // Type cast checking
        if (!(wolf instanceof VillagerWolf villagerWolf))
            return;

        Brain<Wolf> brain = villagerWolf.getBrain();

        // Only scan when working
        if (!brain.isActive(Activity.WORK))
            return;

        // Clear visited set
        this.visited.clear();

        // Scan for nearest fence area
        Optional<BlockPos> area = BlockPos.findClosestMatch(villagerWolf.blockPosition(), RANGE_HORIZONTAL, RANGE_VERTICAL, (pos) -> {
            // If visited, ignore
            if (this.visited.contains(pos))
                return false;

            // Check block type
            BlockState state = world.getBlockState(pos);
            if (!this.isFenceOrFenceGate(state))
                return false;

            // Try to get a fenced area
            FenceArea fenceArea = this.getConnectedFenceArea(world, pos);
            if (fenceArea == null)
                return false;

            brain.setMemory(WolfMemoryType.NEAREST_FENCE_AREA, fenceArea);
            return true;
        });

        // If no area is found, erase memory
        if (area.isEmpty())
            brain.eraseMemory(WolfMemoryType.NEAREST_FENCE_AREA);
    }


    /**
     * Performs a depth-first search to find all adjacent blocks with fences
     *
     * @return FenceArea if there is a connected fence area within limits, otherwise null
     */
    @Nullable
    private FenceArea getConnectedFenceArea(@Nonnull ServerLevel world, @Nonnull BlockPos start) {
        Set<BlockPos> fencedArea = new HashSet<>();
        BlockPos gatePosition = null;
        Stack<BlockPos> stack = new Stack<>();
        stack.push(start);

        int depth = 0;
        while (!stack.isEmpty()) {
            // Check maximum DFS count
            if (depth++ > MAX_FENCE_DEPTH) {
                depth = -1;
                break;
            }

            // Pop current node
            BlockPos currentPos = stack.pop();

            // If we've visited it, ignore
            if (this.visited.contains(currentPos))
                continue;

            // Mark as visited
            this.visited.add(currentPos);

            // If current is fence or fence gate, add to list
            BlockState currentState = world.getBlockState(currentPos);
            if (this.isFenceGate(currentState)) {
                if (gatePosition == null)
                    gatePosition = currentPos;
                fencedArea.add(currentPos);
            } else if (this.isFence(currentState)) {
                fencedArea.add(currentPos);
            } else {
                // Current block is not fence or fence gate, continue to next
                continue;
            }

            // Check for neighboring blocks
            BlockPos[] neighbors = new BlockPos[]{currentPos.north(), currentPos.east(), currentPos.south(), currentPos.west()};
            for (BlockPos neighbor : neighbors) {
                // Ignore if visited
                if (this.visited.contains(neighbor))
                    continue;
                // Ignore if not fence or fence gate
                if (!isFenceOrFenceGate(world.getBlockState(neighbor)))
                    continue;
                // Add to stack to visit
                stack.push(neighbor);
            }
        }

        // If no fence gate or reached max depth, return null
        if (gatePosition == null || depth == -1)
            return null;

        // Check if fence is too small
        if (fencedArea.size() < MIN_FENCE_COUNT)
            return null;

        try {
            return new FenceArea(fencedArea, gatePosition);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean isFenceOrFenceGate(BlockState state) {
        return this.isFence(state) || this.isFenceGate(state);
    }

    private boolean isFence(BlockState state) {
        return state.is(BlockTags.FENCES, stateBase -> stateBase.getBlock() instanceof FenceBlock);
    }

    private boolean isFenceGate(BlockState state) {
        return state.is(BlockTags.FENCE_GATES, stateBase -> stateBase.getBlock() instanceof FenceGateBlock);
    }

    @Getter
    public static final class FenceArea {

        @Nonnull
        private final Set<BlockPos> fences; // and fence gates
        @Nonnull
        private final BlockPos gatePosition;

        private int xMin, xMax;
        private final int y;
        private int zMin, zMax;

        public FenceArea(@Nonnull Set<BlockPos> fences, @Nonnull BlockPos gatePosition) throws IllegalArgumentException {
            this.fences = fences;
            this.gatePosition = gatePosition;
            this.y = this.gatePosition.getY();

            // Calculate min/max of X and Z
            this.xMin = this.zMin = Integer.MAX_VALUE;
            this.xMax = this.zMax = Integer.MIN_VALUE;
            for (BlockPos pos : this.fences) {
                this.xMin = Math.min(this.xMin, pos.getX());
                this.xMax = Math.max(this.xMax, pos.getX());
                this.zMin = Math.min(this.zMin, pos.getZ());
                this.zMax = Math.max(this.zMax, pos.getZ());
            }

            if (this.xMin == this.xMax || this.zMin == this.zMax) {
                throw new IllegalArgumentException("Connected fences does not form a cohesive area!");
            }
        }

        @Nonnull
        public static FenceArea fromNbt(CompoundTag tag) {
            Set<BlockPos> fences = new HashSet<>();
            ListTag fenceList = tag.getList("fences", Tag.TAG_LONG);
            for (Tag fenceTag : fenceList) {
                long posLong = ((LongTag) fenceTag).getAsLong();
                fences.add(BlockPos.of(posLong));
            }

            BlockPos gatePos = BlockPos.of(tag.getLong("gate_position"));
            return new FenceArea(fences, gatePos);
        }

        /**
         * Returns the absolute center of the fence area
         * - might not be inside the fence zone if irregularly shaped
         */
        @Nonnull
        public BlockPos getCenter() {
            return new BlockPos((this.xMin + this.xMax) / 2, this.y, (this.zMin + this.zMax) / 2);
        }

        @Nullable
        public BlockPos getAnyPositionInside() {
            return this.getAnyPositionInside(0);
        }

        @Nullable
        private BlockPos getAnyPositionInside(int attempts) {
            if (attempts > 10)
                return null;

            // Generate a random point within bounds
            int randomX = RandomUtil.RANDOM.nextInt(this.xMin + 1, this.xMax);
            int randomZ = RandomUtil.RANDOM.nextInt(this.zMin + 1, this.zMax);

            // Ray trace in the +X direction (any direction works)
            int dx = 0;
            int fenceCount = 0;
            while (randomX + dx <= this.xMax) {
                if (this.fences.contains(new BlockPos(randomX + dx, this.y, randomZ)))
                    fenceCount++;
                dx++;
            }

            // If intersected fence count is even, then we are outside the fence
            // - we'll attempt to try again until the max attempts is reached
            return fenceCount % 2 == 0 ? this.getAnyPositionInside(attempts + 1) : new BlockPos(randomX, this.y, randomZ);
        }

        @Nonnull
        public CompoundTag toNbtTag() {
            CompoundTag nbtTag = new CompoundTag();

            // Store gate position
            nbtTag.putLong("gate_position", this.gatePosition.asLong());

            // Store fence positions
            ListTag fenceList = new ListTag();
            for (BlockPos pos : this.fences) {
                fenceList.add(LongTag.valueOf(pos.asLong()));
            }
            nbtTag.put("fences", fenceList);

            return nbtTag;
        }

    }

}
