package dev.breeze.settlements.entities.villagers.emeralds;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import lombok.Getter;

import javax.annotation.Nonnull;

public class VillagerEmeraldManager {

    // TODO: make config field
    public static final int INITIAL_EMERALD_BALANCE = 640;

    @Nonnull
    private final BaseVillager villager;

    @Getter
    private int emeralds;

    public VillagerEmeraldManager(@Nonnull BaseVillager villager) {
        this.villager = villager;

        // TODO: load emerald balance from memory (if exists; otherwise use initial balance)
        this.emeralds = INITIAL_EMERALD_BALANCE;
    }

    public boolean canAfford(int amount) {
        return this.emeralds >= amount;
    }

    public void depositEmeralds(int amount) {
        this.emeralds += amount;
    }

    public void withdrawEmeralds(int amount) {
        // TODO: do we want to throw an exception if the villager can't afford it?
        // TODO: or do we want to allow the villager to go into debt?
        this.emeralds -= amount;
    }

    public void writeToMemory() {
        // TODO: save emerald balance to memory
    }

}
