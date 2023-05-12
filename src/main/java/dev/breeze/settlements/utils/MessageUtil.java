package dev.breeze.settlements.utils;

import dev.breeze.settlements.Main;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MessageUtil {

    /**
     * Sends a colored and formatted message containing the plugin's name as prefix to the target
     * - color code '&' will be translated
     */
    public static void sendMessageWithPrefix(CommandSender target, String format, Object... args) {
        target.sendMessage(translateColorCode("&7[&6%s&7] &r%s".formatted(Main.getPlugin().getName(), format.formatted(args))));
    }

    /**
     * Sends a colored and formatted message without prefix to the target
     * - color code '&' will be translated
     */
    public static void sendMessageWithoutPrefix(CommandSender target, String format, Object... args) {
        target.sendMessage(translateColorCode(String.format(format, args)));
    }

    public static void sendActionableMessage(@Nonnull CommandSender target, @Nonnull String message,
                                             @Nullable HoverEvent hoverEvent, @Nullable ClickEvent clickEvent) {
        ComponentBuilder messageBuilder = new ComponentBuilder().appendLegacy(translateColorCode(message));
        if (hoverEvent != null) {
            messageBuilder.event(hoverEvent);
        }
        if (clickEvent != null) {
            messageBuilder.event(clickEvent);
        }

        // TODO: if we want to make this bukkit-compatible, we might wanna change this
        target.spigot().sendMessage(messageBuilder.create());
    }


    /**
     * Broadcasts a colored and formatted message in the server
     */
    public static void broadcast(String format, Object... args) {
        Bukkit.broadcastMessage(translateColorCode(format, args));
    }

    public static String translateColorCode(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
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
