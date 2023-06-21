package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.utils.*;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public final class TradeItemsBehavior extends InteractAtTargetBehavior {

    private static final ItemStack EMERALD = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.EMERALD).build());

    @Nullable
    private BaseVillager seller;
    @Nullable
    private Material material;
    @Nullable
    private ItemStack materialItem;
    private int amount;
    private int sellerTargetPrice; // also initial price
    private int buyerTargetPrice;
    private double currentPrice; // double, rounded when the deal is made

    private float friendship;
    private float sellerMood;
    private float buyerMood;

    // Animation variables
    private boolean tradePerformed;
    private boolean tradeSuccessful;

    private int haggleTicksLeft;
    private int throwEmeraldTicksLeft;
    private int waitUntilThrowProductTicksLeft;

    public TradeItemsBehavior() {
        // Preconditions to this behavior
        super(Map.of(
                        // We should have a shopping list
                        VillagerMemoryType.SHOPPING_LIST.getMemoryModuleType(), MemoryStatus.VALUE_PRESENT,
                        // There should be sellers we've identified nearby
                        VillagerMemoryType.NEARBY_SELLERS.getMemoryModuleType(), MemoryStatus.VALUE_PRESENT
                ), TimeUtil.seconds(30), 0,
                TimeUtil.seconds(20), Math.pow(4, 2),
                5, 1,
                TimeUtil.seconds(20), TimeUtil.seconds(20));

        this.seller = null;
        this.material = null;
        this.materialItem = null;
        this.amount = 0;

        this.sellerTargetPrice = 0;
        this.buyerTargetPrice = 0;
        this.currentPrice = 0.0;

        this.friendship = 0;
        this.sellerMood = 0;
        this.buyerMood = 0;

        this.tradePerformed = false;
        this.tradeSuccessful = false;

        this.haggleTicksLeft = 0;
        this.throwEmeraldTicksLeft = 0;
        this.waitUntilThrowProductTicksLeft = 0;
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

            this.seller = sellerVillager;
            this.material = entry.getValue();
            this.materialItem = CraftItemStack.asNMSCopy(new ItemStackBuilder(this.material).build());

            // Calculate the amount we want to buy using the following criteria:
            // 1. evaluate price & see how many we can afford
            // 2. minimum of #1 and the amount we want to buy
            // 3. minimum of #2 and the amount the seller has in stock
            int selfPriceEvaluation = baseVillager.evaluatePrice(this.material);
            int canAfford = baseVillager.getEmeraldBalance() / selfPriceEvaluation;
            int canBuy = Math.min(canAfford, shoppingList.get(entry.getValue()));
            this.amount = Math.min(stock, canBuy);

            // Check if we can actually buy something
            if (this.amount <= 0) {
                iterator.remove();
                continue;
            }

            this.sellerTargetPrice = this.seller.evaluatePrice(this.material) * this.amount;
            this.buyerTargetPrice = Math.min(selfPriceEvaluation * this.amount, baseVillager.getEmeraldBalance());
            this.currentPrice = this.sellerTargetPrice;

            this.friendship = baseVillager.getFriendshipTowards(sellerVillager);
            this.sellerMood = 1;
            this.buyerMood = 1;

            this.tradePerformed = false;
            this.tradeSuccessful = false;
            // Min 3 seconds, max 8 seconds
            this.haggleTicksLeft = TimeUtil.seconds(3) + RandomUtil.RANDOM.nextInt(TimeUtil.seconds(5));
            this.throwEmeraldTicksLeft = 0;
            this.waitUntilThrowProductTicksLeft = TimeUtil.seconds(1);
            return true;
        }

        return false;
    }

    @Override
    protected boolean checkExtraCanStillUseConditions(ServerLevel vel, Villager villager, long gameTime) {
        return this.seller != null && this.seller.isAlive();
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        super.start(level, villager, gameTime);

        if (this.seller != null) {
            villager.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, this.seller);
            this.seller.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, villager);
        }
    }

    @Override
    protected void tickExtra(ServerLevel level, Villager villager, long gameTime) {
        if (this.seller == null || this.materialItem == null) {
            return;
        }
        if (villager instanceof BaseVillager baseVillager) {
            baseVillager.setHeldItem(EMERALD);
        }
        this.seller.setHeldItem(this.materialItem);

        villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.seller, true));
        this.seller.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(villager, true));
    }

    @Override
    protected void navigateToTarget(ServerLevel level, Villager villager, long gameTime) {
        if (this.seller == null) {
            return;
        }
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.seller, 0.5F, 3));
        this.seller.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(villager, 0.5F, 3));
    }

    @Override
    protected void interactWithTarget(ServerLevel level, Villager villager, long gameTime) {
        if (this.seller == null || this.material == null || !(villager instanceof BaseVillager baseVillager)) {
            return;
        }
        if (!villager.getNavigation().isDone()) {
            villager.getNavigation().stop();
        }
        if (!this.seller.getNavigation().isDone()) {
            this.seller.getNavigation().stop();
        }

        // Calculate trade logic first, then display animations
        if (!this.tradePerformed) {
            this.performTrade(baseVillager);

            // Determine how long throwing the emerald will take based on the deal price
            this.throwEmeraldTicksLeft = Math.min(TimeUtil.seconds(1), 1 + ((int) this.currentPrice) / 4);
        }

        // Display haggle animation
        if (--this.haggleTicksLeft > 0) {
            if (this.haggleTicksLeft % 15 == 0) {
                Villager villagerToHmm = RandomUtil.RANDOM.nextBoolean() ? baseVillager : this.seller;
                boolean approval = RandomUtil.RANDOM.nextBoolean();
                villagerToHmm.playSound(approval ? SoundEvents.VILLAGER_YES : SoundEvents.VILLAGER_NO, 0.3F, 0.75F + RandomUtil.RANDOM.nextFloat() * 0.5F);
                ParticleUtil.globalParticle(LocationUtil.fromNmsEntity(villagerToHmm).add(0, BaseVillager.getActualEyeHeight(), 0),
                        approval ? Particle.VILLAGER_HAPPY : Particle.VILLAGER_ANGRY, approval ? 5 : 2, 0.3, 0.3, 0.3, 0.1);
            }
            return;
        }

        // If not successful trade, display angry animation
        // TODO: add seller to blacklist for a while
        // TODO: decrement friendship
        if (!this.tradeSuccessful) {
            Location sellerLocation = LocationUtil.fromNmsEntity(this.seller).add(0, BaseVillager.getActualEyeHeight(), 0);
            Location buyerLocation = LocationUtil.fromNmsEntity(baseVillager).add(0, BaseVillager.getActualEyeHeight(), 0);
            SoundUtil.playSoundPublic(sellerLocation, Sound.ENTITY_VILLAGER_NO, 0.2F, 0.75F + RandomUtil.RANDOM.nextFloat() * 0.5F);
            SoundUtil.playSoundPublic(buyerLocation, Sound.ENTITY_VILLAGER_NO, 0.2F, 0.75F + RandomUtil.RANDOM.nextFloat() * 0.5F);

            ParticleUtil.globalParticle(sellerLocation, Particle.VILLAGER_ANGRY, 3, 0.3, 0.3, 0.3, 0.1);
            ParticleUtil.globalParticle(buyerLocation, Particle.VILLAGER_ANGRY, 2, 0.3, 0.3, 0.3, 0.1);

            // Stop behavior
            this.doStop(level, villager, gameTime);
            return;
        }

        // Display throw emerald animation
        if (--this.throwEmeraldTicksLeft > 0) {
            throwItemAt(baseVillager, this.seller, Material.EMERALD, RandomUtil.RANDOM.nextBoolean() ? 1 : 2);
            SoundUtil.playSoundPublic(LocationUtil.fromNmsEntity(baseVillager), Sound.ENTITY_ITEM_PICKUP, 0.2F, 2F);
            return;
        }

        // Wait a bit before throwing the product
        if (--this.waitUntilThrowProductTicksLeft > 0) {
            return;
        }

        // Seller throws sold item animation (last animation)
        Location sellerLocation = LocationUtil.fromNmsEntity(this.seller).add(0, BaseVillager.getActualEyeHeight(), 0);
        throwItemAt(this.seller, baseVillager, this.material, this.amount);
        SoundUtil.playSoundPublic(sellerLocation, Sound.ENTITY_ITEM_PICKUP, 0.2F, 2F);

        // Display after-trade effects
        Location buyerLocation = LocationUtil.fromNmsEntity(baseVillager).add(0, BaseVillager.getActualEyeHeight(), 0);
        SoundUtil.playSoundPublic(sellerLocation, Sound.ENTITY_VILLAGER_CELEBRATE, 0.2F, 0.75F + RandomUtil.RANDOM.nextFloat() * 0.5F);
        SoundUtil.playSoundPublic(buyerLocation, Sound.ENTITY_VILLAGER_CELEBRATE, 0.2F, 0.75F + RandomUtil.RANDOM.nextFloat() * 0.5F);

        ParticleUtil.globalParticle(sellerLocation, Particle.VILLAGER_HAPPY, 10, 0.3, 0.3, 0.3, 0.1);
        ParticleUtil.globalParticle(buyerLocation, Particle.VILLAGER_HAPPY, 10, 0.3, 0.3, 0.3, 0.1);

        // Stop behavior
        this.doStop(level, villager, gameTime);
    }

    private void performTrade(@Nonnull BaseVillager baseVillager) {
        // Safety check
        if (this.tradePerformed) {
            return;
        }
        this.tradePerformed = true;

        // Silence the warnings, since we've checked before
        assert this.seller != null;
        assert this.material != null;

        // Determine the price to sell at
        // - the final price will be stored in 'currentPrice' after haggling
        boolean keepHaggling = true;
        while (keepHaggling && this.sellerMood > 0 && this.buyerMood > 0) {
            DebugUtil.broadcastEntity("Haggling: price=%f [b=%d,s=%d], seller mood=%f, buyer mood=%f".formatted(this.currentPrice, this.buyerTargetPrice,
                    this.sellerTargetPrice, this.sellerMood, this.buyerMood), baseVillager.getStringUUID(), Collections.emptyList());
            keepHaggling = false;

            // Check if the buyer wants to haggle
            if (shouldHaggle(this.currentPrice, this.buyerTargetPrice, this.friendship, baseVillager.getEmeraldBalance())) {
                this.currentPrice = getHaggledPrice(this.currentPrice, this.buyerTargetPrice);
                this.sellerMood -= getRandomMoodDecrement(this.friendship);
                keepHaggling = true;
            }

            // Check if the seller wants to haggle as well
            if (shouldHaggle(this.currentPrice, this.buyerTargetPrice, this.friendship, this.seller.getEmeraldBalance())) {
                this.currentPrice = getHaggledPrice(this.currentPrice, this.sellerTargetPrice);
                this.buyerMood -= getRandomMoodDecrement(this.friendship);
                keepHaggling = true;
            }
        }

        // Price is settled, do the trade now
        int dealPrice = Math.toIntExact(Math.round(this.currentPrice));
        this.tradeSuccessful = baseVillager.canAfford(dealPrice);
        if (!this.tradeSuccessful) {
            DebugUtil.broadcastEntity("&cTrade failed (insufficient funds): &e%s &cx&e%d &cfor &e%d &cemeralds"
                            .formatted(this.material.name(), this.amount, dealPrice),
                    baseVillager.getStringUUID(), List.of(
                            "Buyer balance: %d".formatted(baseVillager.getEmeraldBalance()),
                            "Seller balance: %d".formatted(this.seller.getEmeraldBalance()),
                            "Buyer mood: %f".formatted(this.buyerMood),
                            "Seller Mood: %f".formatted(this.sellerMood)
                    ));
            return;
        }

        // Withdraw/deposit emeralds
        baseVillager.withdrawEmeralds(dealPrice);
        this.seller.depositEmeralds(dealPrice);

        // Add/remove item to inventory
        org.bukkit.inventory.ItemStack purchased = new ItemStackBuilder(this.material).setAmount(this.amount).build();
        baseVillager.getCustomInventory().addItem(purchased);
        this.seller.getCustomInventory().remove(purchased, this.amount);

        // Update or remove item from shopping list memory
        HashMap<Material, Integer> shoppingList = VillagerMemoryType.SHOPPING_LIST.get(baseVillager.getBrain());
        if (shoppingList.get(this.material) > this.amount) {
            // Only bought partial amount, update shopping list
            shoppingList.put(this.material, shoppingList.get(this.material) - this.amount);
        } else {
            // Bought enough, remove from shopping list
            shoppingList.remove(this.material);
        }
        VillagerMemoryType.SHOPPING_LIST.set(baseVillager.getBrain(), shoppingList);

        // Remove seller from seller memory
        Map<UUID, Material> sellerList = VillagerMemoryType.NEARBY_SELLERS.get(baseVillager.getBrain());
        sellerList.remove(this.seller.getUUID());
        VillagerMemoryType.NEARBY_SELLERS.set(baseVillager.getBrain(), sellerList);

        DebugUtil.broadcastEntity("&aTrade successful: &e%s &ax&e%d &afor &e%d &aemeralds".formatted(this.material.name(), this.amount, dealPrice),
                baseVillager.getStringUUID(), List.of(
                        "Buyer balance: %d".formatted(baseVillager.getEmeraldBalance()),
                        "Seller balance: %d".formatted(this.seller.getEmeraldBalance()),
                        "Buyer mood: %f".formatted(this.buyerMood),
                        "Seller Mood: %f".formatted(this.sellerMood)
                ));
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
        this.seller = null;
        this.material = null;
        this.materialItem = null;
        this.amount = 0;

        this.sellerTargetPrice = 0;
        this.buyerTargetPrice = 0;
        this.currentPrice = 0.0;

        this.friendship = 0;
        this.sellerMood = 0;
        this.buyerMood = 0;

        this.tradePerformed = false;
        this.tradeSuccessful = false;

        this.haggleTicksLeft = 0;
        this.throwEmeraldTicksLeft = 0;
        this.waitUntilThrowProductTicksLeft = 0;
    }

    @Override
    protected boolean hasTarget() {
        return this.seller != null;
    }

    @Override
    protected boolean isTargetReachable(Villager villager) {
        return this.seller != null && villager.distanceToSqr(this.seller) < this.getInteractRangeSquared();
    }

    @Nonnull
    @Override
    public ItemStackBuilder getGuiItemBuilderAbstract() {
        return new ItemStackBuilder(Material.EMERALD)
                .setDisplayName("&eTrade items behavior")
                .setLore("&7Trades items needed with nearby villagers");
    }

    private static void throwItemAt(@Nonnull BaseVillager thrower, @Nonnull BaseVillager receiver, @Nonnull Material material, int amount) {
        Location headLocation = LocationUtil.fromNmsEntity(thrower).add(0, BaseVillager.getActualEyeHeight(), 0);
        // TODO: 1.20 replace with item display
        Item itemEntity = thrower.getLevel().getWorld().dropItem(headLocation,
                new ItemStackBuilder(material).setAmount(amount).setLore(RandomUtil.randomString()).build());
        itemEntity.setVelocity(LocationUtil.fromNmsEntity(receiver).subtract(headLocation).toVector().multiply(0.1).setY(0.15));
        itemEntity.setPickupDelay(32767);
        itemEntity.setTicksLived(6000 - TimeUtil.ticks(15));
    }

    private static boolean shouldHaggle(double currentPrice, double targetPrice, double friendship, int villagerEmeraldBalance) {
        double delta = Math.abs(currentPrice - targetPrice);
        // If the price difference is insignificant, don't haggle; checks
        // - if the delta price compared to the current price is insignificant (< 15%)
        // - if the delta price is insignificant (< 1%) compared to the villager's emerald balance
        if (Math.abs(delta / currentPrice) < 0.15 && Math.abs(delta / villagerEmeraldBalance) < 0.01) {
            return false;
        }

        // Calculate the haggling chance using the sigmoid function
        double shift = 3;
        double haggleChance = 1 / (1 + Math.exp(shift + 0.9 * friendship - 0.6 * Math.abs(delta)));
        // Roll the dice
        return Math.random() < haggleChance;
    }

    private static double getHaggledPrice(double currentPrice, double targetPrice) {
        double interpolationPoint = 0.7;
        return currentPrice + (interpolationPoint * (targetPrice - currentPrice));
    }

    private static float getRandomMoodDecrement(float friendship) {
        // This multiplier should be small for higher friendship values, and vice versa
        float friendshipMultiplier = 1 / friendship;
        // If friendship is negative, this multiplier will be fixed at 2
        if (friendship < 0) {
            friendshipMultiplier = 2;
        }

        // Generates a random number between 0 and 0.2, multiplied by the friendship multiplier
        return RandomUtil.RANDOM.nextFloat() * 0.2F * friendshipMultiplier;
    }

}
