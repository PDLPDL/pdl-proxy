package com.pdlpdl.pdlproxy.minecraft.impl;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectingEvent;
import org.geysermc.mcprotocollib.network.event.session.PacketErrorEvent;
import org.geysermc.mcprotocollib.network.event.session.PacketSendingEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionListener;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.ServerListener;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundLoginAcknowledgedPacket;

/**
 * Wrap the library built-in "ServerListener", which is a packet listener operating "as a server" (i.e. listening for
 *  events from the client).  Just allow configuration state packets to pass through without special handling.
 */
public class CustomServerListenerConfigurationInterceptor implements SessionListener {

    private final ServerListener wrapped;

    public CustomServerListenerConfigurationInterceptor() {
        // Don't need the "networkCodec" - that's the configuration we are stopping the library from sending.
        this.wrapped = new ServerListener(null);
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        //
        // As the server, the ServerboundLoginAcknowledgedPacket was received, indicating the client is ready for CONFIGURATION state
        //  Immediately set the outbound to the Client to CONFIGURATION state.  Should we now change the downstream to expect
        //  CONFIGURATION from the downstream server?  How will the downstream server know to change to configuration state?
        //
        MinecraftProtocol protocol = session.getPacketProtocol();
        if ((protocol.getInboundState() == ProtocolState.LOGIN) && ((packet instanceof ServerboundLoginAcknowledgedPacket))) {
            // The real client has said, "I'm ready for configuration".  So, prepare ourselves to receive CONFIGURATION.
            session.switchInboundState(() -> protocol.setInboundState(ProtocolState.CONFIGURATION));

            // We aren't going to send anything other than CONFIGURATION to the upstream client here either, so go
            //  ahead and switch that state as well.
            protocol.setOutboundState(ProtocolState.CONFIGURATION);
        } else {
            this.wrapped.packetReceived(session, packet);
        }
    }

    @Override
    public void packetSending(PacketSendingEvent event) {
        this.wrapped.packetSending(event);
    }

    @Override
    public void packetSent(Session session, Packet packet) {
        this.wrapped.packetSent(session, packet);
    }

    @Override
    public void packetError(PacketErrorEvent event) {
        this.wrapped.packetError(event);
    }

    @Override
    public void connected(ConnectedEvent event) {
        this.wrapped.connected(event);
    }

    @Override
    public void disconnecting(DisconnectingEvent event) {
        this.wrapped.disconnecting(event);
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        this.wrapped.disconnected(event);
    }
}
