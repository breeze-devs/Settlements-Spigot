package dev.breeze.settlements.test;

import dev.breeze.settlements.Main;
import dev.breeze.settlements.config.files.NitwitPranksConfig;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.utils.MessageUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import net.minecraft.core.Rotations;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.VillagerType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;

import javax.annotation.Nonnull;

public class TestCommandHandler implements CommandExecutor {

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, String[] args) {
        if (!(sender instanceof Player p)) {
            // Console command
            MessageUtil.sendMessage(sender, "Go away evil console!!!");
            return true;
        }

        // Admin-only command
        if (!p.isOp())
            return true;

        int length = args.length;
        MessageUtil.sendMessage(p, "Starting test execution...");
        Block block = p.getTargetBlockExact(100);
        if (block == null) {
            MessageUtil.sendMessage(p, "&cInvalid block target!");
        } else {
            if (length == 0) {
                new BaseVillager(block.getLocation().add(0, 1, 0), VillagerType.PLAINS);
            } else if (length == 4 && args[0].equals("armorstand")) {
                float pitch = Float.parseFloat(args[1]);
                float yaw = Float.parseFloat(args[2]);
                float roll = Float.parseFloat(args[3]);
                MessageUtil.sendMessage(p, "&aPitch: %f, Yaw: %f, Roll: %f", pitch, yaw, roll);

                ServerLevel level = ((CraftWorld) p.getWorld()).getHandle();
                ArmorStand armorStand = new ArmorStand(level, block.getX(), block.getY() + 1, block.getZ());
                armorStand.setNoGravity(true);
                armorStand.setShowArms(true);
                armorStand.setRightArmPose(new Rotations(pitch, yaw, roll));
                armorStand.setItemSlot(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.IRON_AXE).build()));
                level.addFreshEntity(armorStand, CreatureSpawnEvent.SpawnReason.CUSTOM);
            } else if (length == 1 && args[0].equals("Advancement")) {
                Bukkit.getUnsafe().loadAdvancement(NamespacedKey.fromString("", Main.getPlugin()), "");
                Advancement advancement = Bukkit.getAdvancement(NamespacedKey.fromString("", Main.getPlugin()));
            } else if (length == 1 && args[0].equals("config")) {
                int cooldown = NitwitPranksConfig.getInstance().getLaunchFireworkCooldown().getValue();
                double range = NitwitPranksConfig.getInstance().getLaunchFireworkRange().getValue();
                MessageUtil.sendMessage(p, "CD: %d, R: %d", cooldown, ((int) range));
            } else {
                MessageUtil.sendMessage(p, "&cInvalid testing format!");
                return true;
            }
        }

        MessageUtil.sendMessage(p, "Test execution complete!");
        return true;
    }

}
