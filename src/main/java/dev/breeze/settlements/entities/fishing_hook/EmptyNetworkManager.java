package dev.breeze.settlements.entities.fishing_hook;

import io.netty.channel.*;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

import javax.annotation.Nonnull;
import java.io.Serial;
import java.net.SocketAddress;

public class EmptyNetworkManager extends Connection {

    public EmptyNetworkManager(PacketFlow flag) {
        super(flag);
        this.channel = new EmptyChannel(null);
        this.address = new SocketAddress() {
            @Serial
            private static final long serialVersionUID = 8207338859896320185L;
        };
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void send(@Nonnull Packet packet, PacketSendListener listener) {
        // Do nothing
    }

    public static class EmptyChannel extends AbstractChannel {
        private final ChannelConfig config = new DefaultChannelConfig(this);

        public EmptyChannel(Channel parent) {
            super(parent);
        }

        @Override
        public ChannelConfig config() {
            config.setAutoRead(true);
            return config;
        }

        @Override
        protected void doBeginRead() {
        }

        @Override
        protected void doBind(SocketAddress address) {
        }

        @Override
        protected void doClose() {
        }

        @Override
        protected void doDisconnect() {
        }

        @Override
        protected void doWrite(ChannelOutboundBuffer buffer) {
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        protected boolean isCompatible(EventLoop loop) {
            return true;
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        protected SocketAddress localAddress0() {
            return null;
        }

        @Override
        public ChannelMetadata metadata() {
            return new ChannelMetadata(true);
        }

        @Override
        protected AbstractUnsafe newUnsafe() {
            return null;
        }

        @Override
        protected SocketAddress remoteAddress0() {
            return null;
        }
    }

}

