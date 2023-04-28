package dev.breeze.settlements.debug.guis.villager;


import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.guis.CustomInventoryHolder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class VillagerDebugInventoryHolder extends CustomInventoryHolder {

    /**
     * The internal identifier/name of the inventory
     */
    private final String identifier;

    private final BaseVillager villager;

    /**
     * Checks if the given string identifier matches this inventory's
     *
     * @param identifier identifier to test
     * @return true if identifier matches, otherwise false
     */
    public boolean idMatch(String identifier) {
        return this.identifier.equals(identifier);
    }

}
