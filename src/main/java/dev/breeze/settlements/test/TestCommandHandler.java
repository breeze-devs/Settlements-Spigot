package dev.breeze.settlements.test;

import dev.breeze.settlements.Main;
import dev.breeze.settlements.config.ConfigField;
import dev.breeze.settlements.config.ConfigFileWrapper;
import dev.breeze.settlements.config.ConfigType;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.utils.MessageUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import net.minecraft.core.Rotations;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.VillagerType;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
                try {
                    ItemStack apple = new ItemStackBuilder(Material.APPLE).build();
                    Location loc = new Location(p.getWorld(), 314, -31.4, 3.14);

                    ConfigFileWrapper wrapper = new ConfigFileWrapper("test");
                    // Test primitive objects
                    List<ConfigField<?>> fields = new ArrayList<>(Arrays.asList(
                            new ConfigField<>(wrapper, ConfigType.EMPTY, "primitives", List.of("These are all primitive types", "2nd line of comments"), ""),
                            new ConfigField<>(wrapper, ConfigType.BOOLEAN, "primitives.bool", "default: true", true),
                            new ConfigField<>(wrapper, ConfigType.COLOR, "primitives.color", "default: red", Color.RED),
                            new ConfigField<>(wrapper, ConfigType.DOUBLE, "primitives.double", "default: 3.14", 3.14),
                            new ConfigField<>(wrapper, ConfigType.INT, "primitives.int", "default: 6", 6),
                            new ConfigField<>(wrapper, ConfigType.ITEM_STACK, "primitives.item", "default: apple x1", apple),
                            new ConfigField<>(wrapper, ConfigType.LOCATION, "primitives.location", "default: [314, -31.4, 3.14]", loc),
                            new ConfigField<>(wrapper, ConfigType.LONG, "primitives.long", "default: 314", 314L),
                            new ConfigField<>(wrapper, ConfigType.OFFLINE_PLAYER, "primitives.player", "default: yourself", p),
                            new ConfigField<>(wrapper, ConfigType.STRING, "primitives.string", "default: notch", "notch"),
                            new ConfigField<>(wrapper, ConfigType.VECTOR, "primitives.vector", "default: [314, -31.4, 3.14]", loc.toVector())
                    ));

                    // Test list objects
                    fields.addAll(Arrays.asList(
                            new ConfigField<>(wrapper, ConfigType.BOOLEAN_LIST, "lists.bool", "default: [true, false]", List.of(true, false)),
                            new ConfigField<>(wrapper, ConfigType.BYTE_LIST, "lists.byte", "default: [1B, 3B]", List.of((byte) 1, (byte) 3)),
                            new ConfigField<>(wrapper, ConfigType.DOUBLE_LIST, "lists.double", "default: [3.1, 3.4]", List.of(3.1, 3.4)),
                            new ConfigField<>(wrapper, ConfigType.INT_LIST, "lists.int", "default: [3, 1]", List.of(3, 1)),
                            new ConfigField<>(wrapper, ConfigType.LONG_LIST, "lists.long", "default: [3L, 1L]", List.of(3L, 1L)),
                            new ConfigField<>(wrapper, ConfigType.STRING_LIST, "lists.string", "default: ['hello', 'world']", List.of("hello", "world"))
                    ));

                    // Test indentation
                    fields.addAll(Arrays.asList(
                            new ConfigField<>(wrapper, ConfigType.EMPTY, "indent", "comment1", ""),
                            new ConfigField<>(wrapper, ConfigType.EMPTY, "indent.test", "comment2", "string2"),
                            new ConfigField<>(wrapper, ConfigType.EMPTY, "indent.test.long", "comment3", "string3"),
                            new ConfigField<>(wrapper, ConfigType.STRING, "indent.test.long.string", "comment4", "string4"),
                            new ConfigField<>(wrapper, ConfigType.STRING, "indent.test.long2", "comment5", "string5"),
                            new ConfigField<>(wrapper, ConfigType.STRING, "indent.test2", "comment6", "string6"),
                            new ConfigField<>(wrapper, ConfigType.STRING, "indent2", "comment7", "string7")
                    ));

                    for (ConfigField<?> field : fields) {
                        if (field.getType() == ConfigType.EMPTY) {
                            MessageUtil.sendMessage(p, "%s - EMPTY", field.getName());
                            continue;
                        }
                        MessageUtil.sendMessage(p, "%s - %s", field.getName(), field.getValue().toString());
                    }

                    wrapper.save();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                MessageUtil.sendMessage(p, "&cInvalid testing format!");
                return true;
            }
        }

        MessageUtil.sendMessage(p, "Test execution complete!");
        return true;
    }

}
