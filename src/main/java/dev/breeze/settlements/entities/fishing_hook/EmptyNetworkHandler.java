package dev.breeze.settlements.entities.fishing_hook;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import javax.annotation.Nonnull;

/**
 * Used for NPC players that doesn't need to receive packets
 */
public class EmptyNetworkHandler extends ServerGamePacketListenerImpl {

    public EmptyNetworkHandler(MinecraftServer minecraftServer, Connection networkManager, ServerPlayer entityPlayer) {
        super(minecraftServer, networkManager, entityPlayer);
    }

    @Override
    public void send(@Nonnull Packet<?> packet) {
        // Do nothing
    }

}
