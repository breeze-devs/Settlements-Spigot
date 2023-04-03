package dev.breeze.settlements.utils;

import dev.breeze.settlements.Main;
import org.bukkit.NamespacedKey;

public class KeyUtils {
    public static NamespacedKey newKey(String key) {
        return new NamespacedKey(Main.getPlugin(), key);
    }
}
