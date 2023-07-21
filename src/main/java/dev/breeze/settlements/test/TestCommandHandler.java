package dev.breeze.settlements.test;

import dev.breeze.settlements.Main;
import dev.breeze.settlements.displays.cakes.CakeDisplay;
import dev.breeze.settlements.displays.cakes.CakeDisplayType;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.utils.KeyUtils;
import dev.breeze.settlements.utils.MessageUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import net.minecraft.core.Rotations;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.VillagerType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.advancement.Advancement;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class TestCommandHandler implements TabExecutor {

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

        String subCommand = args.length > 0 ? args[0].toLowerCase() : "basevillager";
        MessageUtil.sendMessageWithPrefix(p, "Starting test execution...");
        Block target = p.getTargetBlockExact(100);

        switch (subCommand) {
            case "villager" -> {
                if (target == null) {
                    MessageUtil.sendMessageWithPrefix(p, "&cInvalid block target!");
                } else {
                    baseVillager(target, args);
                }
            }
            case "armorstand" -> {
                if (target == null) {
                    MessageUtil.sendMessageWithPrefix(p, "&cInvalid block target!");
                } else if (args.length != 4) {
                    MessageUtil.sendMessageWithPrefix(p, "&cInvalid Arguments!");
                } else {
                    armorstand(p, target, args);
                }
            }
            case "advancement" -> advancement(p, args);
            case "display" -> {
                if (target == null) {
                    MessageUtil.sendMessageWithPrefix(p, "&cInvalid block target!");
                } else {
                    display(p, target, args);
                }
            }
            default -> {
                MessageUtil.sendMessageWithPrefix(p, "&cInvalid testing format!");
                return true;
            }
        }

        MessageUtil.sendMessageWithPrefix(p, "Test execution complete!");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> tabComplete = new ArrayList<>();
        if (!(sender instanceof Player)) {
            return tabComplete;
        }

        if (args.length == 1) {
            tabComplete.add("villager");
            tabComplete.add("armorstand");
            tabComplete.add("advancement");
            tabComplete.add("display");
        }
        return tabComplete.stream().filter(completion -> completion.toLowerCase().contains(args[args.length - 1].toLowerCase())).toList();
    }

    public static void baseVillager(Block target, String[] args) {
        new BaseVillager(target.getLocation().add(0, 1, 0), VillagerType.PLAINS);
    }

    public static void armorstand(Player p, Block target, String[] args) {
        float pitch = Float.parseFloat(args[1]);
        float yaw = Float.parseFloat(args[2]);
        float roll = Float.parseFloat(args[3]);
        MessageUtil.sendMessageWithPrefix(p, "&aPitch: %f, Yaw: %f, Roll: %f", pitch, yaw, roll);

        ServerLevel level = ((CraftWorld) p.getWorld()).getHandle();
        ArmorStand armorStand = new ArmorStand(level, target.getX(), target.getY() + 1, target.getZ());
        armorStand.setNoGravity(true);
        armorStand.setShowArms(true);
        armorStand.setRightArmPose(new Rotations(pitch, yaw, roll));
        armorStand.setItemSlot(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.IRON_AXE).build()));
        level.addFreshEntity(armorStand, CreatureSpawnEvent.SpawnReason.CUSTOM);
    }

    public static void advancement(Player p, String[] args) {
        Bukkit.getUnsafe().loadAdvancement(KeyUtils.newKey(""), "");
        Advancement advancement = Bukkit.getAdvancement(KeyUtils.newKey(""));
    }

    private void display(Player p, Block target, String[] args) {
        // Create the item display
        CakeDisplay cakeDisplay = CakeDisplayType.WHITE.getCakeDisplayFactory().createCakeDisplay();
        cakeDisplay.spawnAll(target.getLocation().add(0, 1, 0));

        // Schedule for removal
        Bukkit.getScheduler().runTaskLater(Main.getPlugin(), cakeDisplay::removeAll, TimeUtil.seconds(5));
    }

}
