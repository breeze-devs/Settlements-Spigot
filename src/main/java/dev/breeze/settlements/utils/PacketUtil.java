package dev.breeze.settlements.utils;

import net.minecraft.network.protocol.Packet;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class PacketUtil {

    public static void sendPacketToAllPlayers(Packet<?> packet) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            ((CraftPlayer) online).getHandle().connection.send(packet);
        }
    }

}
