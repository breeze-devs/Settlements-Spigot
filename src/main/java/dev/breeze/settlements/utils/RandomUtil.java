package dev.breeze.settlements.utils;

import org.bukkit.Location;

import java.util.*;

public class RandomUtil {

    public static final Random RANDOM = new Random();

    public static Location addRandomOffset(Location location, double dx, double dy, double dz) {
        return location.clone().add(RANDOM.nextDouble() * dx - dx / 2,
                RANDOM.nextDouble() * dy - dy / 2,
                RANDOM.nextDouble() * dz - dz / 2);
    }

    public static String randomString() {
        return String.valueOf(RANDOM.nextDouble() * 100);
    }

    public static <T> T choice(List<T> list) {
        return list.get(RANDOM.nextInt(list.size()));
    }

    public static <T> T choice(T[] list) {
        return list[RANDOM.nextInt(list.length)];
    }

    public static <T> T weightedChoice(Map<T, Integer> weightMap) {
        int max = weightMap.values().stream().mapToInt(Integer::intValue).sum();
        int target = RANDOM.nextInt(max);

        int prefixSum = 0;
        for (Map.Entry<T, Integer> entry : weightMap.entrySet()) {
            prefixSum += entry.getValue();
            if (prefixSum > target)
                return entry.getKey();
        }
        throw new ArithmeticException("Invalid weights! Check if any weight(s) are zero or negative");
    }

    /**
     * Shuffles the given list in-place and returns the list itself
     */
    public static <T> ArrayList<T> shuffle(ArrayList<T> list) {
        Collections.shuffle(list);
        return list;
    }

}
