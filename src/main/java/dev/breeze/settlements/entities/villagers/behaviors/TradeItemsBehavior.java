package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.utils.MessageUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.entity.Entity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public final class TradeItemsBehavior extends InteractAtTargetBehavior {

    private static final ItemStack EMERALD = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.EMERALD).build());

    @Nullable
    private BaseVillager tradeTarget;
    @Nullable
    private Material material;
    private int amount;
    private int initialPrice;

    public TradeItemsBehavior() {
        // Preconditions to this behavior
        // TODO: scan CD: 20s / behavior CD: 20s
        super(Map.of(
                        // We should have a shopping list
                        VillagerMemoryType.SHOPPING_LIST.getMemoryModuleType(), MemoryStatus.VALUE_PRESENT,
                        // There should be sellers we've identified nearby
                        VillagerMemoryType.NEARBY_SELLERS.getMemoryModuleType(), MemoryStatus.VALUE_PRESENT
                ), TimeUtil.seconds(5), 0,
                TimeUtil.seconds(10), Math.pow(3, 2),
                5, 1,
                TimeUtil.seconds(20), TimeUtil.seconds(20));

        this.tradeTarget = null;
        this.material = null;
        this.amount = 0;
        this.initialPrice = 0;
    }

    @Override
    protected boolean scan(ServerLevel level, Villager villager) {
        if (!(villager instanceof BaseVillager baseVillager)) {
            return false;
        }

        // Check inventory fullness
        if (baseVillager.getCustomInventory().isCompletelyFilled()) {
            return false;
        }

        Brain<Villager> brain = villager.getBrain();

        // If there are no items to buy, ignore behavior
        Map<Material, Integer> shoppingList = VillagerMemoryType.SHOPPING_LIST.get(brain);
        if (shoppingList == null || shoppingList.isEmpty()) {
            return false;
        }

        // Get the sellers we've scanned before
        Map<UUID, Material> sellerList = VillagerMemoryType.NEARBY_SELLERS.get(brain);
        if (sellerList == null || sellerList.isEmpty()) {
            return false;
        }

        // Loop through the sellers and pick one to trade with
        Iterator<Map.Entry<UUID, Material>> iterator = sellerList.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Material> entry = iterator.next();
            // Check if we still want the item (we might have bought it already)
            if (!shoppingList.containsKey(entry.getValue())) {
                iterator.remove();
                continue;
            }

            // We want this item, so we'll try to trade with this seller
            Entity sellerEntity = Bukkit.getEntity(entry.getKey());
            if (sellerEntity == null || !(((CraftEntity) sellerEntity).getHandle() instanceof BaseVillager sellerVillager)) {
                iterator.remove();
                continue;
            }

            // Check if the seller is still alive & has stock
            // - the sensor has already checked friendship level
            // - technically and stock too, but someone may have bought out the stock since then
            int stock = sellerVillager.getStock(entry.getValue());
            if (!sellerVillager.isAlive() || stock <= 0) {
                iterator.remove();
                continue;
            }

            this.tradeTarget = sellerVillager;
            this.material = entry.getValue();
            this.amount = Math.min(stock, shoppingList.get(entry.getValue()));
            this.initialPrice = amount * 2; // TODO: evaluate price for item
            break;
        }

        return this.tradeTarget != null;
    }

    @Override
    protected boolean checkExtraCanStillUseConditions(ServerLevel vel, Villager villager, long gameTime) {
        return this.tradeTarget != null && this.tradeTarget.isAlive();
    }

    @Override
    protected void tickExtra(ServerLevel level, Villager villager, long gameTime) {
        if (this.tradeTarget == null) {
            return;
        }
        if (villager instanceof BaseVillager baseVillager) {
            baseVillager.setHeldItem(EMERALD);
        }
        villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.tradeTarget, true));
    }

    @Override
    protected void navigateToTarget(ServerLevel level, Villager villager, long gameTime) {
        if (this.tradeTarget == null) {
            return;
        }
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.tradeTarget, 0.5F, 2));
    }

    @Override
    protected void interactWithTarget(ServerLevel level, Villager villager, long gameTime) {
        if (this.tradeTarget == null || !(villager instanceof BaseVillager baseVillager)) {
            return;
        }

        int price = this.tradeTarget.priceWillingToSellAt(baseVillager, Objects.requireNonNull(this.material), this.amount, this.initialPrice);
        // TODO: haggle if necessary
        // TODO: subtract emeralds

        // TODO: if successful trade, add to inventory
        boolean tradeSuccessful = baseVillager.getEmeraldBalance() >= price;
        if (tradeSuccessful) {
            org.bukkit.inventory.ItemStack purchased = new ItemStackBuilder(this.material).setAmount(this.amount).build();

            // Buyer logic
            baseVillager.getCustomInventory().addItem(purchased);
            baseVillager.setEmeraldBalance(baseVillager.getEmeraldBalance() - price);

            // Seller logic
            this.tradeTarget.getCustomInventory().remove(purchased, this.amount);
            this.tradeTarget.setEmeraldBalance(this.tradeTarget.getEmeraldBalance() + price);

            // Update or remove item from shopping list memory
            HashMap<Material, Integer> shoppingList = VillagerMemoryType.SHOPPING_LIST.get(villager.getBrain());
            if (shoppingList.get(this.material) > this.amount) {
                // Only bought partial amount, update shopping list
                shoppingList.put(this.material, shoppingList.get(this.material) - this.amount);
            } else {
                // Bought enough, remove from shopping list
                shoppingList.remove(this.material);
            }
            VillagerMemoryType.SHOPPING_LIST.set(villager.getBrain(), shoppingList);

            // Remove seller from seller memory
            Map<UUID, Material> sellerList = VillagerMemoryType.NEARBY_SELLERS.get(villager.getBrain());
            sellerList.remove(this.tradeTarget.getUUID());
            VillagerMemoryType.NEARBY_SELLERS.set(villager.getBrain(), sellerList);

            MessageUtil.broadcast("Trade successful: " + this.material + " x" + this.amount + " for " + price + " emeralds");
        }

        // Stop behavior
        this.doStop(level, villager, gameTime);
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.stop(level, villager, gameTime);

        // Reset held item
        if (villager instanceof BaseVillager baseVillager) {
            baseVillager.clearHeldItem();
        }

        // Remove interaction target memory
        villager.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);

        // Reset variables
        this.tradeTarget = null;
        this.material = null;
        this.amount = 0;
        this.initialPrice = 0;
    }

    @Override
    protected boolean hasTarget() {
        return this.tradeTarget != null;
    }

    @Override
    protected boolean isTargetReachable(Villager villager) {
        return this.tradeTarget != null && villager.distanceToSqr(this.tradeTarget) < this.getInteractRangeSquared();
    }

    @Nonnull
    @Override
    public ItemStackBuilder getGuiItemBuilderAbstract() {
        return new ItemStackBuilder(Material.EMERALD)
                .setDisplayName("&eTrade items behavior")
                .setLore("&7Trades items needed with nearby villagers");
    }

}
