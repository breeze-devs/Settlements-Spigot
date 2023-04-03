package dev.breeze.settlements.entities.villagers.inventory;


import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.guis.CustomInventoryHolder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class VillagerInventoryHolder extends CustomInventoryHolder {

    @Getter
    private final BaseVillager villager;

}
