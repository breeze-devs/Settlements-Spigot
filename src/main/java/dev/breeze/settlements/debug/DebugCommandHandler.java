package dev.breeze.settlements.debug;

import dev.breeze.settlements.utils.DebugUtil;
import dev.breeze.settlements.utils.MessageUtil;
import dev.breeze.settlements.utils.SoundUtil;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class DebugCommandHandler implements TabExecutor {

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, String[] args) {
        if (!(sender instanceof Player p)) {
            // Console command
            MessageUtil.sendMessageWithPrefix(sender, "Go away evil console!!!");
            return true;
        }

        // TODO: refactor permissions
        // Check if the player has permission to execute the command
        if (!p.hasPermission("settlements.admin")) {
            MessageUtil.sendMessageWithPrefix(sender, "&cYou lack the required permission to use this command!");
        }

        String subCommand = args.length > 0 ? args[0].toLowerCase() : "help";
        switch (subCommand) {
            case "toggle" -> toggleDebug(p);
            case "tool" -> debugTool(p);
            default -> {
                // Help or other
                // TODO: send help message
                MessageUtil.sendMessageWithPrefix(p, "&cInvalid debug format!");
                return true;
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> tabComplete = new ArrayList<>();
        if (!(sender instanceof Player)) {
            return tabComplete;
        }

        if (args.length == 1) {
            tabComplete.add("toggle");
            tabComplete.add("tool");
            tabComplete.add("help");
        }
        return tabComplete.stream().filter(completion -> completion.toLowerCase().contains(args[args.length - 1].toLowerCase())).toList();
    }

    public static void toggleDebug(Player p) {
        DebugUtil.toggleDebugging();
        MessageUtil.sendMessageWithPrefix(p, "%s debugging mode", DebugUtil.isDebuggingEnabled() ? "&aEnabled" : "&cDisabled");
    }

    private static void debugTool(Player p) {
        p.getInventory().addItem(DebugStickEvent.SETTLEMENTS_DEBUG_STICK);
        SoundUtil.playSound(p, Sound.ENTITY_ITEM_PICKUP, 1);
        MessageUtil.sendMessageWithPrefix(p, "&eYou've obtained a debug tool");
    }

}
