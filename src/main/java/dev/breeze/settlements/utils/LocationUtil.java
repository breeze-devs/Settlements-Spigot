package dev.breeze.settlements.utils;

import net.minecraft.core.BlockPos;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class LocationUtil {

    public static EulerAngle convertVectorToEulerAngle(Vector vec) {
        double x = vec.getX();
        double y = vec.getY();
        double z = vec.getZ();

        double xz = Math.sqrt(x * x + z * z);
        double eulX;
        if (x < 0) {
            if (y == 0) {
                eulX = Math.PI * 0.5;
            } else {
                eulX = Math.atan(xz / y) + Math.PI;
            }
        } else {
            eulX = Math.atan(y / xz) + Math.PI * 0.5;
        }

        double eulY;
        if (x == 0)
            eulY = z > 0 ? Math.PI : 0;
        else
            eulY = Math.atan(z / x) + Math.PI * 0.5;

        return new EulerAngle(eulX, eulY, 0);
    }

    public static ArrayList<Location> getLine(Location from, Location to, int amount) {
        from = from.clone();
        to = to.clone();
        Vector v = to.subtract(from).toVector();
        Location loc = from.clone();

        double dx = v.getX() / amount;
        double dy = v.getY() / amount;
        double dz = v.getZ() / amount;

        ArrayList<Location> locations = new ArrayList<>();
        for (int a = 0; a <= amount; a++) {
            locations.add(loc.clone());
            loc.add(dx, dy, dz);
        }
        return locations;
    }

    public static ArrayList<Location> getCircle(Location center, double radius, int amount) {
        center = center.clone();
        World world = center.getWorld();
        double increment = (2 * Math.PI) / amount;
        ArrayList<Location> locations = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            double angle = i * increment;
            double x = center.getX() + (radius * Math.cos(angle));
            double z = center.getZ() + (radius * Math.sin(angle));
            locations.add(new Location(world, x, center.getY(), z));
        }
        return locations;
    }

    public static ArrayList<Location> rotateLocations(Location center, List<Location> points, double yaw, double pitch, double roll, double scale) {
        yaw = Math.toRadians(yaw);
        pitch = Math.toRadians(pitch);
        roll = Math.toRadians(roll);

        ArrayList<Location> list = new ArrayList<>();
        double cp = Math.cos(pitch);
        double sp = Math.sin(pitch);
        double cy = Math.cos(yaw);
        double sy = Math.sin(yaw);
        double cr = Math.cos(roll);
        double sr = Math.sin(roll);
        double x, bx, y, by, z, bz;
        for (Location point : points) {
            x = point.getX() - center.getX();
            bx = x;
            y = point.getY() - center.getY();
            by = y;
            z = point.getZ() - center.getZ();
            bz = z;
            x = ((x * cy - bz * sy) * cr + by * sr) * scale;
            y = ((y * cp + bz * sp) * cr - bx * sr) * scale;
            z = ((z * cp - by * sp) * cy + bx * sy) * scale;
            list.add(new Location(center.getWorld(), (center.getX() + x), (center.getY() + y), (center.getZ() + z)));
        }
        return list;
    }

    /**
     * Calculates the coordinates of a Bézier curve at time t
     * - ref:
     * <a href="https://math.stackexchange.com/questions/4173207/how-to-find-a-bezier-curve-between-two-points-and-two-tangents-without-anchor-po">stack exchange</a>
     *
     * @param t       time
     * @param source  source location (where the curve starts)
     * @param target  destination location (where the curve ends)
     * @param anchor1 anchor location 1 (where the curve curves towards initially)
     * @param anchor2 anchor location 2 (where the curve curves towards later on)
     * @return location on the curve at time t
     */
    public static Location bezierCurve(double t, Location source, Location target, Location anchor1, Location anchor2) {
        double s = 1 - t;
        double x = Math.pow(s, 3) * source.getX() + 3 * Math.pow(s, 2) * t * anchor1.getX()
                + 3 * s * Math.pow(t, 2) * anchor2.getX() + Math.pow(t, 3) * target.getX();
        double y = Math.pow(s, 3) * source.getY() + 3 * Math.pow(s, 2) * t * anchor1.getY()
                + 3 * s * Math.pow(t, 2) * anchor2.getY() + Math.pow(t, 3) * target.getY();
        double z = Math.pow(s, 3) * source.getZ() + 3 * Math.pow(s, 2) * t * anchor1.getZ()
                + 3 * s * Math.pow(t, 2) * anchor2.getZ() + Math.pow(t, 3) * target.getZ();
        return new Location(source.getWorld(), x, y, z);
    }

    @Nonnull
    public static Location fromBlockPos(@Nonnull World world, @Nonnull BlockPos blockPos) {
        return new Location(world, blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

}
