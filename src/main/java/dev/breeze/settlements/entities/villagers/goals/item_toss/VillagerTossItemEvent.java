package dev.breeze.settlements.entities.villagers.goals.item_toss;

import dev.breeze.settlements.utils.PersistentUtil;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.UUID;

public final class VillagerTossItemEvent implements Listener {

    public static final String PERSISTENT_KEY = "VILLAGER_TOSSED_ITEM";

    @EventHandler(ignoreCancelled = true)
    public void onHitEntity(EntityDamageByEntityEvent event) {
        String dataString = PersistentUtil.getEntityEntry(event.getDamager(), PersistentDataType.STRING, PERSISTENT_KEY);
        if (dataString == null)
            return;

        // Convert data string to record
        TossItemGoal.TossedItemData data = TossItemGoal.TossedItemData.fromString(dataString);
        if (data == null)
            return;

        // Cancel event if it's at a "friendly" unit
        if (event.getEntity() instanceof Villager || event.getEntity() instanceof IronGolem) {
            event.setCancelled(true);
            return;
        }

        // Cancel event if the wolf is not angry at the villager
        if (event.getEntity() instanceof Wolf wolf && event.getDamager() instanceof Snowball snowball
                && wolf.getTarget() != snowball.getShooter()) {
            event.setCancelled(true);
            return;
        }

        // Otherwise, deal damage
        event.setDamage(data.damage());

        // Calculate knockback
        Vector knockbackDirection = event.getEntity().getLocation().toVector().subtract(event.getDamager().getLocation().toVector()).setY(0.5);
        event.getEntity().setVelocity(knockbackDirection.normalize().multiply(data.knockback()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onHit(ProjectileHitEvent event) {
        String dataString = PersistentUtil.getEntityEntry(event.getEntity(), PersistentDataType.STRING, PERSISTENT_KEY);
        if (dataString == null)
            return;

        // Convert data string to record
        TossItemGoal.TossedItemData data = TossItemGoal.TossedItemData.fromString(dataString);
        if (data == null)
            return;

        // Remove item
        Item item = (Item) Bukkit.getEntity(UUID.fromString(data.itemEntityUuid()));
        if (item == null)
            return;

        // Remove item early
        item.remove();

        // Play particle
        Material material = item.getItemStack().getType();
        if (material.isBlock())
            ParticleUtil.blockBreak(event.getEntity().getLocation(), material, 3, 0.1, 0.1, 0.1, 1);
        else if (material.isItem())
            ParticleUtil.itemBreak(event.getEntity().getLocation(), item.getItemStack(), 3, 0.1, 0.1, 0.1, 0);
    }

}
