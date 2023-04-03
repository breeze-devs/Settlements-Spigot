package dev.breeze.settlements.utils.particle;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ParticleUtil {

    public static void globalParticle(Location location, Particle particle, int count, double dx, double dy, double dz, double speed) {
        location.getWorld().spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, dx, dy, dz, speed);
    }

    public static void globalParticle(List<Location> locations, Particle particle, int count, double dx, double dy, double dz, double speed) {
        for (Location loc : locations)
            loc.getWorld().spawnParticle(particle, loc.getX(), loc.getY(), loc.getZ(), count, dx, dy, dz, speed);
    }

    public static void playerParticle(Player player, Location location, Particle particle, int count, double dx, double dy, double dz, double speed) {
        player.spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, dx, dy, dz, speed);
    }

    public static void playerParticle(Player player, List<Location> locations, Particle particle, int count, double dx, double dy, double dz, double speed) {
        for (Location loc : locations)
            player.spawnParticle(particle, loc.getX(), loc.getY(), loc.getZ(), count, dx, dy, dz, speed);
    }

    public static void coloredDust(Location location, int count, double dx, double dy, double dz, double speed, float size, Color color) {
        DustOptions options = new DustOptions(color, size);
        location.getWorld().spawnParticle(Particle.REDSTONE, location.getX(), location.getY(), location.getZ(), count, dx, dy, dz, speed, options);
    }

    public static void blockBreak(Location location, Material blockMaterial, int count, double dx, double dy, double dz, double speed) {
        location.getWorld().spawnParticle(Particle.BLOCK_CRACK, location.getX(), location.getY(), location.getZ(), count, dx, dy, dz, speed, blockMaterial.createBlockData());
    }

    public static void itemBreak(Location location, ItemStack itemStack, int count, double dx, double dy, double dz, double speed) {
        location.getWorld().spawnParticle(Particle.ITEM_CRACK, location.getX(), location.getY(), location.getZ(), count, dx, dy, dz, speed, itemStack);
    }

}
