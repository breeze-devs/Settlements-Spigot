package dev.breeze.settlements.utils.itemstack;

import org.bukkit.Material;
import org.bukkit.inventory.meta.FireworkMeta;

public final class FireworkItemStackBuilder extends ItemStackBuilder {

    public FireworkItemStackBuilder() {
        super(Material.FIREWORK_ROCKET);
    }

    private FireworkMeta getFireworkMeta() {
        return (FireworkMeta) super.itemMeta;
    }

    public FireworkItemStackBuilder setFlightDuration(int duration) {
        this.getFireworkMeta().setPower(duration);
        return this;
    }

}
