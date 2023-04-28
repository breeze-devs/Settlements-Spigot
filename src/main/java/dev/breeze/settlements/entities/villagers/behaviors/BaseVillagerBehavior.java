package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.utils.MessageUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import javax.annotation.Nonnull;
import java.util.Map;

public abstract class BaseVillagerBehavior extends Behavior<Villager> {

    private static final int DEFAULT_START_CONDITION_CHECK_COOLDOWN = TimeUtil.seconds(5);

    @Getter
    private final int maxStartConditionCheckCooldown;

    private int startConditionCheckCooldown;

    public BaseVillagerBehavior(Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState, int runTime) {
        this(requiredMemoryState, runTime, DEFAULT_START_CONDITION_CHECK_COOLDOWN);
    }

    public BaseVillagerBehavior(Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState, int runTime, int maxStartConditionCheckCooldown) {
        super(requiredMemoryState, runTime);

        this.maxStartConditionCheckCooldown = maxStartConditionCheckCooldown;
        this.startConditionCheckCooldown = this.maxStartConditionCheckCooldown;
    }

    @Override
    protected final boolean checkExtraStartConditions(@Nonnull ServerLevel world, @Nonnull Villager villager) {
        if (--this.startConditionCheckCooldown > 0)
            return false;

        this.startConditionCheckCooldown = this.maxStartConditionCheckCooldown;
        return this.checkExtraStartConditionsRateLimited(world, villager);
    }

    protected abstract boolean checkExtraStartConditionsRateLimited(@Nonnull ServerLevel world, @Nonnull Villager villager);

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        MessageUtil.debug("&a[Debug] Villager behavior " + this.getClass().getSimpleName() + " has started");
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        MessageUtil.debug("&c[Debug] Villager behavior " + this.getClass().getSimpleName() + " has stopped");
    }

    /**
     * Returns the cooldown of the behavior, may be negative
     * - if negative, the behavior is either running or waiting for preconditions to be satisfied
     * - check using {@link Behavior.Status} to determine the exact behavior state
     *
     * @return the cooldown of the behavior
     */
    public abstract int getCurrentCooldown();

    @Nonnull
    public ItemStackBuilder getGuiItemBuilder() {
        ItemStackBuilder builder = this.getGuiItemBuilderAbstract();
        builder.appendLore("&7---");

        // Get behavior status
        if (this.getStatus() == Status.RUNNING) {
            builder.appendLore("&aActive");

            // Make item shimmer
            builder.addEnchantment(Enchantment.VANISHING_CURSE, 1);
            builder.hideFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            // Cooling down or preconditions not met
            if (this.getCurrentCooldown() > 0) {
                builder.appendLore("&6Cooling down: %d ticks left".formatted(this.getCurrentCooldown()));
            } else {
                builder.appendLore("&cPreconditions not met");
            }
        }

        return builder;
    }

    @Nonnull
    public abstract ItemStackBuilder getGuiItemBuilderAbstract();

}
