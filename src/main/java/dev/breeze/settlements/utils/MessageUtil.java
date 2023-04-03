package dev.breeze.settlements.utils;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageUtil {

    /**
     * Sends a colored and formatted message to the target
     * - color code '&' will be translated
     */
    public static void sendMessage(CommandSender target, String format, Object... args) {
        target.sendMessage(translateColorCode(String.format(format, args)));
    }

    /**
     * Broadcasts a colored and formatted message in the server
     */
    public static void broadcast(String format, Object... args) {
        Bukkit.broadcastMessage(translateColorCode(format, args));
    }

    public static String translateColorCode(String format, Object... args) {
        return ChatColor.translateAlternateColorCodes('&', String.format(format, args));
    }

    public static String stripColor(String format, Object... args) {
        return ChatColor.stripColor(String.format(format, args));
    }

    public static void sendActionbar(Player p, String format, Object... args) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(translateColorCode(format, args)));
    }

    public static void sendTitle(Player p, String title, String subtitle) {
        sendTitle(p, title, subtitle, 0, 20, 0);
    }

    public static void sendTitle(Player p, String title, String subtitle, int fadeInTicks, int durationTicks, int fadeOutTicks) {
        p.sendTitle(title == null ? "" : translateColorCode(title),
                subtitle == null ? "" : translateColorCode(subtitle), fadeInTicks, durationTicks, fadeOutTicks);
    }

}
