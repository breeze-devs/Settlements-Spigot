package dev.breeze.settlements.utils.itemstack;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.meta.SkullMeta;

public final class SkullItemStackBuilder extends ItemStackBuilder {

    public SkullItemStackBuilder() {
        super(Material.PLAYER_HEAD);
    }

    private SkullMeta getSkullMeta() {
        return (SkullMeta) super.itemMeta;
    }

    public SkullItemStackBuilder setSkin(String name) {
        this.getSkullMeta().setOwningPlayer(Bukkit.getOfflinePlayer(name));
        return this;
    }

}
