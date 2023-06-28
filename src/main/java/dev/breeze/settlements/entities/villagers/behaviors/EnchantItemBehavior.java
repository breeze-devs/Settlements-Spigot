package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.config.files.EnchantItemsConfig;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.entities.villagers.sensors.VillagerNearbyEnchantingTableSensor;
import dev.breeze.settlements.utils.LocationUtil;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.SoundUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.itemstack.ItemUtil;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EnchantItemBehavior extends InteractAtTargetBehavior {

    private static final int DISPLAY_PREPARE_TIME = TimeUtil.seconds(5);
    private static final int DISPLAY_ENCHANTED_TIME = TimeUtil.seconds(1);
    private static final int INTERACT_RANGE = 2;
    private static final double PARTICLE_DELTA_Y = 1.25;

    @Nullable
    private BlockPos enchantingTable;
    @Nullable
    private ItemStack itemToEnchant;

    private int animationTick;
    @Deprecated
    private Item displayItem;
    @Nonnull
    private List<Location> circleLocations;

    public EnchantItemBehavior() {
        // Preconditions to this behavior
        super(Map.of(
                        // The villager should have seen a enchanting table nearby
                        VillagerMemoryType.NEAREST_ENCHANTING_TABLE.getMemoryModuleType(), MemoryStatus.VALUE_PRESENT
                ), TimeUtil.seconds(30), 0,
                EnchantItemsConfig.getInstance().getEnchantCooldown().getValue(), Math.pow(INTERACT_RANGE, 2),
                5, 1,
                TimeUtil.seconds(20), DISPLAY_PREPARE_TIME + DISPLAY_ENCHANTED_TIME);

        this.enchantingTable = null;
        this.itemToEnchant = null;

        this.animationTick = 0;
        this.displayItem = null;
        this.circleLocations = Collections.emptyList();
    }

    @Override
    protected boolean scan(@Nonnull ServerLevel level, @Nonnull Villager villager) {
        if (!(villager instanceof BaseVillager baseVillager)) {
            return false;
        }

        BlockPos pos = VillagerMemoryType.NEAREST_ENCHANTING_TABLE.get(villager.getBrain());
        if (pos == null) {
            return false;
        }

        if (!isEnchantingTable(villager, pos)) {
            return false;
        }

        this.enchantingTable = pos;

        // Check if there's an empty slot (to store the enchanted item)
        if (baseVillager.getCustomInventory().isCompletelyFilled()) {
            return false;
        }

        // Find item to enchant
        for (org.bukkit.inventory.ItemStack bukkitItem : baseVillager.getCustomInventory().getItems()) {
            // Ignore items based on criteria:
            // 1. item is null
            // 2. item is already enchanted
            // 3. item cannot be enchanted at the current expertise level
            if (bukkitItem == null || !bukkitItem.getEnchantments().isEmpty() || !canEnchant(baseVillager.getExpertiseLevel(), bukkitItem)) {
                continue;
            }

            // We can enchant current item, decrement count by 1
            baseVillager.getCustomInventory().remove(bukkitItem, 1);
            this.itemToEnchant = new ItemStackBuilder(bukkitItem).setAmount(1).buildNms();
            break;
        }

        // Check if we've found an item to enchant or not
        return this.itemToEnchant != null;
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.start(level, villager, gameTime);
        if (!(villager instanceof BaseVillager baseVillager) || this.enchantingTable == null) {
            return;
        }

        // Disable default walking
        baseVillager.setDefaultWalkTargetDisabled(true);

        // Sample circles
        this.circleLocations = LocationUtil.getCircle(LocationUtil.fromBlockPos(level.getWorld(), this.enchantingTable).add(0.5, 0.1, 0.5), 1.3, 16);
    }

    @Override
    protected boolean checkExtraCanStillUseConditions(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        return this.enchantingTable != null && isEnchantingTable(villager, this.enchantingTable);
    }

    @Override
    protected void tickExtra(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        if (!(villager instanceof BaseVillager baseVillager))
            return;

        // Look at target
        if (this.enchantingTable != null) {
            baseVillager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.enchantingTable));
        }

    }

    @Override
    protected void navigateToTarget(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        if (!(villager instanceof BaseVillager baseVillager)) {
            return;
        }

        if (this.enchantingTable != null) {
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.enchantingTable, 0.5F, 1));
        }

        // Hold item to enchant
        if (this.itemToEnchant != null) {
            baseVillager.setHeldItem(this.itemToEnchant);
        }
    }

    @Override
    protected void interactWithTarget(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        // We are close enough to the enchanting table
        if (!(villager instanceof BaseVillager baseVillager) || this.enchantingTable == null || this.itemToEnchant == null) {
            return;
        }

        // Clear held item
        baseVillager.clearHeldItem();

        // TODO: use item display in 1.20 to spawn a book "item"
        this.animationTick++;
        if (this.animationTick == 1) { // 1 because variable is already incremented
            // TODO: spawn book & spin it slowly
            this.displayItem = level.getWorld().dropItem(LocationUtil.fromBlockPos(level.getWorld(), this.enchantingTable).add(0.5, PARTICLE_DELTA_Y, 0.5),
                    ItemUtil.toBukkit(this.itemToEnchant));
            this.displayItem.setVelocity(new Vector());
            this.displayItem.setGravity(false);
            this.displayItem.setPickupDelay(32767);
            this.displayItem.setTicksLived(6000 - DISPLAY_PREPARE_TIME);
        }
        // +1 here because we want to "process" the enchanted item first in the previous iteration
        else if (this.animationTick == DISPLAY_PREPARE_TIME + 1) {
            // TODO: replace with enchanted item
            this.displayItem = level.getWorld().dropItem(LocationUtil.fromBlockPos(level.getWorld(), this.enchantingTable).add(0.5, PARTICLE_DELTA_Y, 0.5),
                    ItemUtil.toBukkit(this.itemToEnchant));
            this.displayItem.setVelocity(new Vector());
            this.displayItem.setGravity(false);
            this.displayItem.setPickupDelay(32767);
            this.displayItem.setTicksLived(6000 - DISPLAY_ENCHANTED_TIME);

            // Spawn particles
            ParticleUtil.globalParticle(LocationUtil.fromBlockPos(level.getWorld(), this.enchantingTable).add(0.5, PARTICLE_DELTA_Y - 0.1, 0.5),
                    Particle.END_ROD, 20, 0, 0, 0, 0.5);
        } else if (this.animationTick == DISPLAY_PREPARE_TIME + DISPLAY_ENCHANTED_TIME) {
            // TODO: remove item display

            // Don't do anything else & stop behavior
            this.doStop(level, villager, gameTime);
            return;
        }

        // Display particles
        if (this.animationTick < DISPLAY_PREPARE_TIME) {
            // Display enchanting table particles
            ParticleUtil.globalParticle(LocationUtil.fromBlockPos(level.getWorld(), this.enchantingTable).add(0.5, 0.5, 0.5),
                    Particle.ENCHANTMENT_TABLE, 5, 0.1, 0.1, 0.1, 1);

            Location circlePoint1 = this.circleLocations.get(this.animationTick % this.circleLocations.size());
            Location circlePoint2 = this.circleLocations.get((this.animationTick + this.circleLocations.size() / 2) % this.circleLocations.size());
            ParticleUtil.globalParticle(List.of(circlePoint1, circlePoint2), Particle.CRIT_MAGIC, 1, 0, 0, 0, 0);
        }


        // Enchant right after the prepare time
        if (this.animationTick != DISPLAY_PREPARE_TIME) {
            // Otherwise, do nothing
            return;
        }

        // Note: this code is only reachable when animationTick == DISPLAY_PREPARE_TIME
        // - therefore will only be ran once

        // Determine the power of the enchanting table
        int bookshelfCount = getBookshelfCount(level, this.enchantingTable);
        bookshelfCount = Math.min(bookshelfCount, EnchantItemsConfig.getInstance().maxBookshelvesAtLevel(baseVillager.getExpertiseLevel()));

        // Get a random enchantment slot (of the 3 slots in an enchanting table, zero-indexed)
        // - slot 0 has the lowest enchantment cost, 1 medium, and 2 the highest
        // - expert (lv. 5) villagers will ignore slot 0 (i.e. only have medium or higher enchants)
        int slot = RandomUtil.RANDOM.nextInt(baseVillager.getExpertiseLevel() == 5 ? 1 : 0, 3);
        int cost = EnchantmentHelper.getEnchantmentCost(level.getRandom(), slot, bookshelfCount, this.itemToEnchant);

        // Get a random list of enchantments based on the slot & cost
        boolean allowTreasure = EnchantItemsConfig.getInstance().getAllowTreasure().getValue();
        List<EnchantmentInstance> enchantments = EnchantmentHelper.selectEnchantment(level.getRandom(), this.itemToEnchant, cost, allowTreasure);

        // Remove one enchantment if enchanting book (otherwise too OP)
        // - this is done in vanilla as well
        boolean isBook = this.itemToEnchant.is(Items.BOOK);
        if (isBook && enchantments.size() > 1) {
            enchantments.remove(RandomUtil.RANDOM.nextInt(enchantments.size()));
        }

        // Replace the item to an enchanted book if book
        if (isBook) {
            this.itemToEnchant = new ItemStackBuilder(Material.ENCHANTED_BOOK).buildNms();
        }

        // Apply the enchantments to the item
        Map<Enchantment, Integer> enchantmentMap = new HashMap<>();
        for (EnchantmentInstance enchantment : enchantments) {
            enchantmentMap.put(enchantment.enchantment, enchantment.level);
        }
        EnchantmentHelper.setEnchantments(enchantmentMap, this.itemToEnchant);

        // Add enchanted item to inventory
        baseVillager.getCustomInventory().addItem(ItemUtil.toBukkit(this.itemToEnchant));

        // Play enchant sound
        SoundUtil.playSoundPublic(LocationUtil.fromBlockPos(level.getWorld(), this.enchantingTable), Sound.BLOCK_ENCHANTMENT_TABLE_USE,
                1f, RandomUtil.RANDOM.nextFloat() * 0.1F + 0.9F);
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.stop(level, villager, gameTime);
        if (!(villager instanceof BaseVillager baseVillager)) {
            return;
        }

        // Clear held item
        baseVillager.clearHeldItem();

        // Enable default walk target setting
        baseVillager.setDefaultWalkTargetDisabled(false);

        // Reset variables
        this.enchantingTable = null;
        this.itemToEnchant = null;
        this.animationTick = 0;
    }

    @Override
    protected boolean hasTarget() {
        return true;
    }

    @Override
    protected boolean isTargetReachable(Villager villager) {
        if (this.enchantingTable == null) {
            return false;
        }
        return villager.distanceToSqr(this.enchantingTable.getX(), this.enchantingTable.getY(), this.enchantingTable.getZ()) < INTERACT_RANGE;
    }

    @Nonnull
    @Override
    public ItemStackBuilder getGuiItemBuilderAbstract() {
        return new ItemStackBuilder(Material.ENCHANTING_TABLE)
                .setDisplayName("&eEnchant items behavior")
                .setLore(
                        "&7Infrequently enchant an enchantable item in its inventory",
                        "&7Requires a nearby enchanting table (with or without bookshelves)",
                        "&7Max enchanting level depends on villager's expertise and the number of bookshelves"
                );
    }

    private static boolean isEnchantingTable(@Nonnull Villager villager, @Nonnull BlockPos pos) {
        BlockState state = villager.level().getBlockState(pos);
        return VillagerNearbyEnchantingTableSensor.isEnchantingTable(state);
    }

    private static boolean canEnchant(int level, @Nonnull org.bukkit.inventory.ItemStack itemStack) {
        return EnchantItemsConfig.getInstance().canEnchantAtLevel(level).contains(itemStack.getType());
    }

    /**
     * Reference from the {@link net.minecraft.world.inventory.EnchantmentMenu#slotsChanged(Container)} method
     *
     * @return the number of valid bookshelves near this enchanting table
     */
    private static int getBookshelfCount(@Nonnull Level level, @Nonnull BlockPos enchantingTablePos) {
        int count = 0;
        for (BlockPos offset : EnchantmentTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantmentTableBlock.isValidBookShelf(level, enchantingTablePos, offset)) {
                ++count;
            }
        }
        return count;
    }

}

