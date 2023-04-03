package dev.breeze.settlements.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LogUtil {

    private static Logger logger;

    public static void init(JavaPlugin plugin) {
        logger = plugin.getLogger();
        info("Hello (happy) world!");
    }

    public static void info(String format, Object... args) {
        logger.log(Level.INFO, String.format(format, args));
    }

    public static void warning(String format, Object... args) {
        logger.log(Level.WARNING, String.format(format, args));
    }

    public static void severe(String format, Object... args) {
        logger.log(Level.SEVERE, String.format(format, args));
    }

    public static void exception(Exception exception, String format, Object... args) {
        logger.log(Level.SEVERE, String.format(format, args), exception);
    }

}
