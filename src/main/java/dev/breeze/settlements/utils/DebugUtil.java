package dev.breeze.settlements.utils;

import dev.breeze.settlements.config.files.GeneralConfig;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DebugUtil {

    @Getter
    private static boolean debuggingEnabled = GeneralConfig.getInstance().getDebugEnabled().getValue();

    public static void toggleDebugging() {
        debuggingEnabled = !debuggingEnabled;
        GeneralConfig.getInstance().getDebugEnabled().setValue(debuggingEnabled);
    }

    /**
     * Broadcasts a colored and formatted message in the server, if debug is enabled
     * - this message will only be sent to players with admin permissions
     */
    public static void broadcast(@Nonnull String message, @Nullable HoverEvent hoverEvent, @Nullable ClickEvent clickEvent) {
        if (!debuggingEnabled) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            // TODO: refactor permissions
            if (player.hasPermission("settlements.admin")) {
                // TODO: refactor strings & colors
                MessageUtil.sendActionableMessage(player, "&7[&6S&eDebug&7] &r%s".formatted(message), hoverEvent, clickEvent);
            }
        }

        // Log to console as well
        log(message);
    }

    /**
     * A shorthand method to broadcast an entity to all admin players
     * - the hover action will display a teleport notice along with the hover messages
     * - the click action will suggest to teleport to the entity
     *
     * @param message       The message to be broadcasted
     * @param uuid          The UUID of the entity
     * @param hoverMessages The list of hover messages to be displayed when hovering over the entity
     */
    public static void broadcastEntity(@Nonnull String message, @Nonnull String uuid, @Nonnull List<String> hoverMessages) {
        String hoverMessage = "&e&lClick to teleport to the entity&r\n%s".formatted(String.join("\n", hoverMessages));
        HoverEvent showEntity = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(MessageUtil.translateColorCode(hoverMessage)));
        ClickEvent suggestTeleport = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/minecraft:tp %s".formatted(uuid));

        broadcast(message, showEntity, suggestTeleport);
    }

    public static void log(String format, Object... args) {
        if (!debuggingEnabled) {
            return;
        }

        LogUtil.info(format, args);
    }

}
