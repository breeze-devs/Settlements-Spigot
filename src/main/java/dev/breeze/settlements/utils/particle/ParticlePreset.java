package dev.breeze.settlements.utils.particle;

import dev.breeze.settlements.utils.LocationUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.List;

public class ParticlePreset {

    public static void displayLine(Location from, Location to, int sampleCount, Particle particle,
                                   int count, double dx, double dy, double dz, double speed) {
        List<Location> line = LocationUtil.getLine(from, to, sampleCount);
        ParticleUtil.globalParticle(line, particle, count, dx, dy, dz, speed);
    }

    public static void displayLinePrivate(Player player, Location from, Location to, int sampleCount, Particle particle,
                                          int count, double dx, double dy, double dz, double speed) {
        List<Location> line = LocationUtil.getLine(from, to, sampleCount);
        ParticleUtil.playerParticle(player, line, particle, count, dx, dy, dz, speed);
    }

    public static void displayCircle(Location center, double radius, int sampleCount, Particle particle,
                                     int count, double dx, double dy, double dz, double speed) {
        List<Location> circle = LocationUtil.getCircle(center, radius, sampleCount);
        ParticleUtil.globalParticle(circle, particle, count, dx, dy, dz, speed);
    }

    public static void displayCirclePrivate(Player player, Location center, double radius, int sampleCount, Particle particle,
                                            int count, double dx, double dy, double dz, double speed) {
        List<Location> circle = LocationUtil.getCircle(center, radius, sampleCount);
        ParticleUtil.playerParticle(player, circle, particle, count, dx, dy, dz, speed);
    }

}
