package dev.breeze.settlements.debug;

import dev.breeze.settlements.utils.MessageUtil;
import dev.breeze.settlements.utils.SoundUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
            MessageUtil.sendMessage(sender, "Go away evil console!!!");
            return true;
        }

        if (!p.isOp()) {
            // Admin-only command
            MessageUtil.sendMessage(sender, "You must be an operator to use this command!");
        }

        String subCommand = args.length > 0 ? args[0].toLowerCase() : "help";
        switch (subCommand) {
            case "tool" -> debugTool(p);
            default -> {
                // Help or other
                // TODO: send help message
                MessageUtil.sendMessage(p, "&cInvalid debug format!");
                return true;
            }
        }

        MessageUtil.sendMessage(p, "Test execution complete!");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> tabComplete = new ArrayList<>();
        if (!(sender instanceof Player)) {
            return tabComplete;
        }

        if (args.length == 1) {
            tabComplete.add("tool");
            tabComplete.add("help");
        }
        return tabComplete.stream().filter(completion -> completion.toLowerCase().contains(args[args.length - 1].toLowerCase())).toList();
    }

    private static void debugTool(Player p) {
        ItemStack debugStick = new ItemStackBuilder(Material.DEBUG_STICK)
                .setDisplayName("&a&lSettlements &f&lDebug Stick")
                .setLore("&eRight click &fan entity added by this plugin to view debug information",
                        "&7oh by the way, this also works as a regular debug stick")
                .build();
        p.getInventory().addItem(debugStick);
        SoundUtil.playSound(p, Sound.ENTITY_ITEM_PICKUP, 1);
        MessageUtil.sendMessage(p, "&eYou've obtained a debug tool");
    }

}
