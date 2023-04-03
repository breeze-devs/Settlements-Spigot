package dev.breeze.settlements.entities.villagers.events;

import dev.breeze.settlements.Main;
import dev.breeze.settlements.utils.LocationUtil;
import dev.breeze.settlements.utils.SafeRunnable;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerReplenishTradeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class VillagerRestockEvent implements Listener {

    private static final HashMap<UUID, Long> cooldown = new HashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onHitEntity(VillagerReplenishTradeEvent event) {
        AbstractVillager villager = event.getEntity();

        // Villager restocks multiple items at once, limit this event to only trigger once
        UUID id = villager.getUniqueId();
        long now = System.currentTimeMillis();
        if (cooldown.containsKey(id) && cooldown.get(id) > now)
            return;
        cooldown.put(id, now + 500);

        // Display particles
        displayRestockParticles(villager);
    }

    private void displayRestockParticles(AbstractVillager villager) {
        final double dy = 0.03;
        final int particlesPerTick = 3;

        new SafeRunnable() {
            int elapsed = 0;

            @Override
            public void safeRun() {
                if (elapsed > 20) {
                    this.cancel();
                    return;
                }

                List<Location> circle = LocationUtil.getCircle(villager.getLocation(), 0.5, 16);
                for (int a = 0; a < particlesPerTick; a++) {
                    int index = (elapsed * particlesPerTick + a);
                    Location location = circle.get(index % circle.size()).clone().add(0, index * dy, 0);
                    ParticleUtil.globalParticle(location, Particle.COMPOSTER, 1, 0, 0, 0, 0);
                }
                elapsed++;
            }
        }.runTaskTimer(Main.getPlugin(), 0L, 1L);
    }

}
