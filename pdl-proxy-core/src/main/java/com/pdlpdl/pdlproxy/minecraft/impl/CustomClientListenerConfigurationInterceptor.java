package com.pdlpdl.pdlproxy.minecraft.impl;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectingEvent;
import org.geysermc.mcprotocollib.network.event.session.PacketErrorEvent;
import org.geysermc.mcprotocollib.network.event.session.PacketSendingEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionListener;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.ClientListener;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrap the library built-in "ClientListener", which is a packet listener operating "as a client" (i.e. listening for
 *  events from the server).  Just allow configuration state packets to pass through without special handling.
 */
public class CustomClientListenerConfigurationInterceptor implements SessionListener {

    private static final Logger LOG = LoggerFactory.getLogger(CustomClientListenerConfigurationInterceptor.class);

    // NOTE: ClientListener listens "as a client" - so it is listening TO a server (the inverse of the typical
    //  "listener" naming pattern since listeners are designed to be used for any number of functions - not just the
    //  "client" for a "server or visa-versa - and therefore are named after the source of the events).

    // NOTE: At the same time, the packets being named Clientbound... and Serverbound... instead of Server... and
    //  Client... also follows that inversion.
    private final ClientListener wrapped;

    public CustomClientListenerConfigurationInterceptor(ClientListener wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        MinecraftProtocol protocol = session.getPacketProtocol();

        if ((protocol.getInboundState() == ProtocolState.CONFIGURATION) && (packet instanceof ClientboundFinishConfigurationPacket)) {
            // Keeping the outbound state unchanged here.
            LOG.debug("INTERCEPTED client listener finish-config packet handling");

            session.switchInboundState(() -> protocol.setInboundState(ProtocolState.GAME));
        } else {
            // Pass-through to the library so it can handle LOGIN and other bits for us.
            // Re-use the ClientListener logic for everything else
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
