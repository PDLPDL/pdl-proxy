/*
 * Copyright (c) 2021 Playful Digital Learning LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pdlpdl.pdlproxy.minecraft.impl;

import com.pdlpdl.pdlproxy.minecraft.DownstreamServerConnection;
import com.pdlpdl.pdlproxy.minecraft.api.GameProfileAwareInterceptor;
import com.pdlpdl.pdlproxy.minecraft.api.PacketInterceptor;
import com.pdlpdl.pdlproxy.minecraft.api.PacketInterceptorControl;
import com.pdlpdl.pdlproxy.minecraft.api.ProxyDirectPacketControlSupplier;
import com.pdlpdl.pdlproxy.minecraft.api.SessionLoginInterceptor;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectingEvent;
import org.geysermc.mcprotocollib.network.event.session.PacketErrorEvent;
import org.geysermc.mcprotocollib.network.event.session.PacketSendingEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionListener;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundKeepAlivePacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.cookie.clientbound.ClientboundCookieRequestPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundConfigurationAcknowledgedPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundCustomQueryPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundHelloPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginCompressionPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginDisconnectPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginFinishedPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundCustomQueryAnswerPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundHelloPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundKeyPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundLoginAcknowledgedPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * SessionListener for upstream Client connections that forwards packets to the downstream server after initiating the
 * downstream connection on login success.
 */
public class ProxyClientSessionAdapter implements SessionListener, ProxyDirectPacketControlSupplier {

    // NOTE: waiting for state should not be necessary.  It was added during upgrade when configuration state was
    //  introduced into the protocol, and we fought with the protocol library doing some configuration work.
    public static final long WAIT_PROTOCOL_STATE_EXPIRATION = 60_000L;

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(ProxyClientSessionAdapter.class);
    private Logger log = DEFAULT_LOGGER;

    private static final Logger DEFAULT_PACKET_DATA_LOGGER = LoggerFactory.getLogger(ProxyClientSessionAdapter.class.getName() + ".packet-data");
    private Logger packetDataLog = DEFAULT_PACKET_DATA_LOGGER;

    private final MinecraftPacketClassifierUtil minecraftPacketClassifierUtil = new MinecraftPacketClassifierUtil();
    private final MinecraftProtocolStateWaiter upstreamClientSessionMinecraftProtocolStateWaiter;

    /**
     * Function called when an upstream client connects and a connection to the downstream server is needed.
     */
    private final Function<IncomingClientSessionInfo, DownstreamServerConnection> startProxyServerSession;
    private final Session upstreamClientSession;
    private final List<PacketInterceptor> packetInterceptors;

    /**
     * Session Login Interceptor for the owner of this adapter (ProxyServerImpl).
     */
    private final SessionLoginInterceptor sessionLoginInterceptor;

    private final Object sync = new Object();

    private DownstreamServerConnection downstreamServerConnection;
    private boolean shutdownInd;

    private GameProfile upstreamGameProfile;
    private GameProfile downstreamGameProfile;

//========================================
// Constructor
//----------------------------------------

    public ProxyClientSessionAdapter(
        Function<IncomingClientSessionInfo, DownstreamServerConnection> startProxyServerSession,
        Session upstreamClientSession,
        PacketInterceptorControl packetInterceptorControl,
        SessionLoginInterceptor sessionLoginInterceptor
    ) {

        this.startProxyServerSession = startProxyServerSession;
        this.upstreamClientSession = upstreamClientSession;
        this.sessionLoginInterceptor = sessionLoginInterceptor;
        this.upstreamClientSessionMinecraftProtocolStateWaiter = new MinecraftProtocolStateWaiter(this.upstreamClientSession.getPacketProtocol());

        // Install our interceptors.
        PacketInterceptor[] interceptors = new PacketInterceptor[packetInterceptorControl.getInterceptorCount()];
        int cur = 0;
        while (cur < packetInterceptorControl.getInterceptorCount()) {
            interceptors[cur] = packetInterceptorControl.getInterceptor(cur);
            cur++;
        }

        this.packetInterceptors = Collections.unmodifiableList(Arrays.asList(interceptors));
        this.notifyPacketInterceptorsInstalled();
    }

//========================================
// Getters
//----------------------------------------

    public List<PacketInterceptor> getPacketInterceptors() {
        return packetInterceptors;
    }

    public GameProfile getDownstreamGameProfile() {
        return downstreamGameProfile;
    }

//========================================
// Lifecycle
//----------------------------------------

    /**
     * Shutdown the session, closing the Upstream and Downstream sessions (if they are still open) in the process.
     *
     * @param reason description of the reason for the shutdown.
     */
    public void shutdown(String reason) {
        if ((reason == null) || (reason.isEmpty())) {
            reason = "shutting down";
        }

        this.shutdownInd = true;
        DownstreamServerConnection savedDownstreamServerConnection;

        synchronized (this.sync) {
            savedDownstreamServerConnection = this.downstreamServerConnection;
            this.downstreamServerConnection = null;
        }

        if (savedDownstreamServerConnection != null) {
            savedDownstreamServerConnection.disconnect(reason);
        }

        if (this.upstreamClientSession.isConnected()) {
            this.upstreamClientSession.disconnect(reason);
        }

        this.notifyPacketInterceptorsRemoved();
    }


//========================================
// SessionListener
//----------------------------------------

    @Override
    public void packetReceived(Session session, Packet packet) {
        this.log.trace("received packet from client: class={}", packet.getClass().getSimpleName());
        this.handlePacketReceivedFromClient(session, packet);
    }

    @Override
    public void packetSending(PacketSendingEvent event) {
        this.log.trace("sending packet to client: class={}; outbound-state={}", event.getPacket().getClass().getSimpleName(), event.getSession().getPacketProtocol().getOutboundState());

        //
        // Call the Session Login Interceptor now
        //
        if (event.getPacket() instanceof ClientboundLoginFinishedPacket) {
            // Contact the Session Login Interceptor first
            this.sessionLoginInterceptor.onPlayerLoginSuccessSending(event);
        }
    }

    @Override
    public void packetSent(Session session, Packet packet) {
        this.log.trace("sent packet to client: class={}", packet.getClass().getSimpleName());

        if (this.shutdownInd) {
            this.log.debug("have packet-sent after shutdown: race-condition?");
            return;
        }

        //
        // ON client login success (i.e. transition from LOGIN to GAME state)...
        //
        if (packet instanceof ClientboundLoginFinishedPacket) {
            ClientboundLoginFinishedPacket loginSuccessPacket = (ClientboundLoginFinishedPacket) packet;

            this.upstreamGameProfile = loginSuccessPacket.getProfile();
            String username = this.upstreamGameProfile.getName();

            //
            // START the downstream session, to the SERVER (double-check we don't already have the connection).
            //
            this.connectDownstream(session, username, this.upstreamGameProfile);
        }

        this.handlePacketSentToClient(packet);
    }

    @Override
    public void packetError(PacketErrorEvent event) {
        this.log.error("Packet handling error", event.getCause());
    }

    @Override
    public void connected(ConnectedEvent event) {
        this.log.info("Proxy Client Session CONNECTED");
    }

    @Override
    public void disconnecting(DisconnectingEvent event) {
        this.log.info("Proxy Client Session Disconnecting!");
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        this.log.info("Proxy Client Session DISCONNECTED! {}", event.getReason(), event.getCause());

        this.shutdown("proxy client session disconnected");
    }

//========================================
// Downstream Connection Handling
//========================================

    private void connectDownstream(Session session, String username, GameProfile gameProfile) {
        //
        // START the downstream session, to the SERVER (double-check we don't already have the connection).
        //
        if (this.downstreamServerConnection == null) {
            IncomingClientSessionInfo incomingClientSessionInfo =
                    new IncomingClientSessionInfo(
                            username,
                            this,
                            session,
                            this::handlePacketReceivedFromServer,
                            this::handlePacketSentToServer,
                            this::handlePacketSendingToServer,
                            this::handleDownstreamDisconnect
                    );

            // Inject the Game Profile into the interested interceptors.
            this.processGameProfileAwareInterceptorInjection(gameProfile);

            // TODO: better make this run asynchronously
            // TODO: otherwise delays connecting to the downstream server can timeout the client connection
            DownstreamServerConnection newConnection =  this.startProxyServerSession.apply(incomingClientSessionInfo);

            // Double-check that we didn't shutdown between the check above and now, to minimize the impact of
            //  potential race conditions.
            synchronized (this.sync) {
                if (this.shutdownInd) {
                    newConnection.disconnect("shutdown race");
                } else {
                    this.downstreamServerConnection = newConnection;
                }
            }
        } else {
            this.log.error("INTERNAL ERROR: sent LoginSuccessPacket when downstream server connection is already active");
        }
    }


//========================================
// PacketInterceptorControlSupplier
//----------------------------------------

    @Override
    public ProxyPacketControlImpl getProxyDirectPacketControl() {
        return new ProxyPacketControlImpl(this::directSendPacketToClient, this::directSendPacketToServer);
    }


//========================================
// Packet Event Handling
//----------------------------------------

    /**
     * Given a packet from the downstream Server, forward the packet to the upstream Client, as appropriate.  Also
     * capture important GameProfile data that affects how subsequent data is formatted and needs to be parsed (e.g.
     * chunk data).
     *
     * NOTE: this is where advanced logic may be added that can intercept, add, or remove packets from the flow.
     *
     * @param packet
     */
    private void handlePacketReceivedFromServer(Packet packet) {

        MinecraftProtocol minecraftProtocol = upstreamClientSession.getPacketProtocol();

        //
        // Forward the packet to the client, but only if we should not filter it out.
        //
        this.log.trace("RECEIVED PACKET FROM DOWNSTREAM SERVER {}  (upstream-outbound-state={})", packet.getClass().getSimpleName(), minecraftProtocol.getOutboundState());
        this.packetDataLog.trace("RECEIVED PACKET FROM DOWNSTREAM SERVER {} (upstream-outbound-state={}); data={}", packet.getClass().getSimpleName(), minecraftProtocol.getOutboundState(), packet);

        if (packet instanceof ClientboundLoginFinishedPacket) {
            this.downstreamGameProfile = ((ClientboundLoginFinishedPacket) packet).getProfile();
        }

        if (this.shouldForwardPacketFromServer(packet, minecraftProtocol)) {
            this.log.trace("CHECKING SHOULD FORWARD DOWNSTREAM SERVER PACKET {} (upstream-outbound-state={})", packet.getClass().getSimpleName(), minecraftProtocol.getOutboundState());
            boolean sendOriginal = this.applyReceivedPacketInterceptors(packet, false);
            if (sendOriginal) {
                this.log.trace("FORWARDING PACKET FROM DOWNSTREAM SERVER TO UPSTREAM CLIENT {} (upstream-outbound-state={})", packet.getClass().getSimpleName(), minecraftProtocol.getOutboundState());

                Set<ProtocolState> validStateSet = this.minecraftPacketClassifierUtil.getValidProtocolStateSetForPacket(packet);
                this.upstreamClientSessionMinecraftProtocolStateWaiter.waitForOutboundStateWithTimeout(validStateSet, WAIT_PROTOCOL_STATE_EXPIRATION);

                this.upstreamClientSession.send(packet);
            }
        }
    }

    /**
     * Given a packet sent to the Server (such as those forwarded from the Client), notify listeners as-needed.
     *
     * @param packet
     */
    private void handlePacketSentToServer(Packet packet) {
        MinecraftProtocol minecraftProtocol = upstreamClientSession.getPacketProtocol();

        if (this.shouldForwardPacketFromClient(packet, minecraftProtocol)) {
            this.notifySentPacketInterceptors(packet, false);
        }

        // The CLIENT has finished its configuration; change the downstream client-to-server session to CONFIGURATION state.
        if (packet instanceof ServerboundConfigurationAcknowledgedPacket) {
            // Now set the downstream connection to CONFIGURATION state as we are done forwarding packets in the LOGIN state.
            this.downstreamServerConnection.switchOutboundState(ProtocolState.CONFIGURATION);
        }

        // End of CONFIGURATION for the client; update the downstream server session to GAME state.
        // Doing the update here instead of on the receive-packet handler because we need to allow the
        //  ServerboundFinishConfigurationPacket to send BEFORE changing to GAME state.
        if (packet instanceof ServerboundFinishConfigurationPacket) {
            this.log.debug("Sent ServerboundFinishConfigurationPacket to downstream server; switching outbound to GAME state");
            this.downstreamServerConnection.switchOutboundState(ProtocolState.GAME);
        }
    }

    /**
     * Given an event for a packet about to be sent to the server, process the event.
     */
    private void handlePacketSendingToServer(PacketSendingEvent event) {
    }

    private void handleDownstreamDisconnect(ProxyServerSessionAdapter proxyServerSessionAdapter,
                                            DisconnectedEvent disconnectedEvent) {

        log.info("Downstream disconnect: " + disconnectedEvent.getReason());
        this.shutdown("downstream disconnected: " + disconnectedEvent.getReason());
    }

    /**
     * Given a packet from the upstream Client, forward the packet to the downstream server, as appropriate.
     *
     * NOTE: this is where advanced logic may be added that can intercept, add, or remove packets from the flow.
     *
     * @param packet
     */
    private void handlePacketReceivedFromClient(Session session, Packet packet) {
        //
        // Forward the packet to the server, but ONLY if the client is in the GAME state and the packet is not a
        //  KeepAlive packet.  Also filter out all Login-related packets.
        //

        this.log.debug("RECEIVED PACKET FROM UPSTREAM CLIENT {}", packet.getClass().getSimpleName());

        MinecraftProtocol minecraftProtocol = (MinecraftProtocol) upstreamClientSession.getPacketProtocol();

        if (packet instanceof ServerboundKeyPacket) {
            this.log.debug("HAVE KEY PACKET FROM UPSTREAM CLIENT");
        }

        if (this.shouldForwardPacketFromClient(packet, minecraftProtocol)) {
            this.log.debug("SHOULD FORWARD PACKET FROM UPSTREAM CLIENT {}", packet.getClass().getSimpleName());

            boolean sendOriginal = this.applyReceivedPacketInterceptors(packet, true);
            if (sendOriginal) {
                this.log.debug("FORWARDING PACKET FROM UPSTREAM CLIENT TO DOWNSTREAM SERVER {}", packet.getClass().getSimpleName());

                Set<ProtocolState> validStateSet = this.minecraftPacketClassifierUtil.getValidProtocolStateSetForPacket(packet);
                this.downstreamServerConnection.waitForState(validStateSet, WAIT_PROTOCOL_STATE_EXPIRATION);
                this.downstreamServerConnection.send(packet);
            } else {
                this.log.debug("DROPPING PACKET FROM UPSTREAM CLIENT TO DOWNSTREAM SERVER {}", packet.getClass().getSimpleName());
            }
        } else {
            this.log.debug("DROPPING PACKET FROM UPSTREAM CLIENT {}", packet.getClass().getSimpleName());
        }
    }

    /**
     * Given a packet from the sent to the Client (such as those forwarded from the Server), notify listeners as-needed.
     *
     * @param packet
     */
    private void handlePacketSentToClient(Packet packet) {
        this.log.debug("PACKET SENT TO CLIENT {}", packet.getClass().getSimpleName());
        if (this.packetDataLog.isTraceEnabled()) {
            this.packetDataLog.trace("SENT PACKET DATA = {}", packet);
        }

        MinecraftProtocol minecraftProtocol = (MinecraftProtocol) upstreamClientSession.getPacketProtocol();

        if (this.shouldForwardPacketFromServer(packet, minecraftProtocol)) {
            this.notifySentPacketInterceptors(packet, true);
        }
    }

//========================================
// Interceptors
//----------------------------------------

    /**
     * Inject the GameProfile into injectors with the GameProfileAwareInterceptor interface.
     *
     * @param gameProfile game profile to inject.
     */
    private void processGameProfileAwareInterceptorInjection(GameProfile gameProfile) {
        for (PacketInterceptor onePacketInterceptor : this.packetInterceptors) {
            if (onePacketInterceptor instanceof GameProfileAwareInterceptor) {
                ((GameProfileAwareInterceptor) onePacketInterceptor).injectGameProfile(gameProfile);
            }
        }
    }

    private void notifyPacketInterceptorsInstalled() {
        for (PacketInterceptor onePacketInterceptor : this.packetInterceptors) {
            onePacketInterceptor.onInterceptorInstalled(this.upstreamClientSession);
        }
    }

    private void notifyPacketInterceptorsRemoved() {
        for (PacketInterceptor onePacketInterceptor : this.packetInterceptors) {
            onePacketInterceptor.onInterceptorRemoved(this.upstreamClientSession);
        }
    }

    private void directSendPacketToClient(Packet packet) {
        if (this.upstreamClientSession.isConnected()) {
            this.log.debug("SENDING DIRECT PACKET TO UPSTREAM CLIENT {}", packet.getClass().getSimpleName());

            this.upstreamClientSession.send(packet);
        } else {
            this.log.info("Dropping direct client-bound packet; session is closed: packet-class={}", packet.getClass().getSimpleName());
        }
    }

    private void directSendPacketToServer(Packet packet) {
        if (this.downstreamServerConnection != null)  {
            this.downstreamServerConnection.send(packet);
        } else {
            this.log.info("Dropping direct server-bound packet; session is closed: packet-class={}", packet.getClass().getSimpleName());
        }
    }

    private boolean applyReceivedPacketInterceptors(Packet packet, boolean isClient) {
        ProxyPacketControlImpl proxyPacketControl = this.getProxyDirectPacketControl();

        for (PacketInterceptor onePacketInterceptor : this.packetInterceptors) {
            if (isClient) {
                onePacketInterceptor.onClientPacketReceived(packet, proxyPacketControl);
            } else {
                onePacketInterceptor.onServerPacketReceived(packet, proxyPacketControl);
            }
        }

        //
        // Inject any client-bound packets added by the interceptor
        //
        List<Packet> clientBoundInjectedPackets = proxyPacketControl.getAddedClientBoundPackets();
        for (Packet clientBoundPacket : clientBoundInjectedPackets) {
            this.log.debug("SENDING INTERCEPTOR INJECTED PACKET TO UPSTREAM CLIENT {}", packet.getClass().getSimpleName());

            Set<ProtocolState> validProtocolStateSet = this.minecraftPacketClassifierUtil.getValidProtocolStateSetForPacket(clientBoundPacket);
            this.upstreamClientSessionMinecraftProtocolStateWaiter.waitForOutboundStateWithTimeout(validProtocolStateSet, WAIT_PROTOCOL_STATE_EXPIRATION);

            this.upstreamClientSession.send(clientBoundPacket);
        }


        //
        // Inject any server-bound packets added by the interceptor
        //
        List<Packet> serverBoundInjectedPackets = proxyPacketControl.getAddedServerBoundPackets();
        for (Packet serverBoundPacket : serverBoundInjectedPackets) {
            this.log.debug("SENDING INTERCEPTOR INJECTED PACKET TO DOWNSTREAM SERVER {}", packet.getClass().getSimpleName());

            Set<ProtocolState> validProtocolStateSet = this.minecraftPacketClassifierUtil.getValidProtocolStateSetForPacket(serverBoundPacket);
            this.downstreamServerConnection.waitForState(validProtocolStateSet, WAIT_PROTOCOL_STATE_EXPIRATION);
            this.downstreamServerConnection.send(serverBoundPacket);
        }


        return ! proxyPacketControl.isDropPacket();
    }

    private void notifySentPacketInterceptors(Packet packet, boolean isClient) {
        for (PacketInterceptor onePacketInterceptor : this.packetInterceptors) {
            if (isClient) {
                onePacketInterceptor.onPacketSentToClient(packet);
            } else {
                onePacketInterceptor.onPacketSentToServer(packet);
            }
        }
    }

//========================================
// Packet Filtering Logic
//----------------------------------------

    private boolean shouldForwardPacketFromServer(Packet packet, MinecraftProtocol minecraftProtocol) {
        log.trace("SHOULD FORWARD PACKET FROM SERVER: upstream-client-is-connected={}; inbound-state={} (want GAME/CONFIGURATION); is-login-packet={}; is-config-packet={}; is-clientbound-keepalive-packet={}; is-passthrough-state={}; packet={}",
            this.upstreamClientSession.isConnected(),
            minecraftProtocol.getInboundState(),
            this.isLoginPacket(packet),
            this.minecraftPacketClassifierUtil.isConfigurationPacket(packet),
            (packet instanceof ClientboundKeepAlivePacket),
            this.isPassthroughState(minecraftProtocol.getInboundState()),
            packet.getClass().getSimpleName()
        );

        return (
                    (this.upstreamClientSession.isConnected()) &&
                    (this.isPassthroughState(minecraftProtocol.getInboundState())) &&
                    (!(this.isLoginPacket(packet))) &&
                    (!(packet instanceof ClientboundKeepAlivePacket))
        );
    }

    private boolean shouldForwardPacketFromClient(Packet packet, MinecraftProtocol minecraftProtocol) {
        log.trace("SHOULD FORWARD PACKET FROM CLIENT: downstream-is-connected={}; outbound-state={} (want GAME/CONFIGURATION); is-login-packet={}; is-config-packet={}; is-serverbound-keepalive-packet={}; is-passthrough-state={}; packet={}",
            (this.downstreamServerConnection != null),
            minecraftProtocol.getOutboundState(),
            this.isLoginPacket(packet),
            this.minecraftPacketClassifierUtil.isConfigurationPacket(packet),
            (packet instanceof ServerboundKeepAlivePacket),
            this.isPassthroughState(minecraftProtocol.getOutboundState()),
            packet.getClass().getSimpleName()
        );

        return (
                    (this.downstreamServerConnection != null) &&
                    (this.isPassthroughState(minecraftProtocol.getOutboundState())) &&
                    (!this.isLoginPacket(packet)) &&
                    (!(packet instanceof ServerboundKeepAlivePacket))
        );
    }


    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    private boolean isPassthroughState(ProtocolState protocolState) {
        switch (protocolState) {
            case GAME:
            case CONFIGURATION:
                return true;
        }

        return false;
    }

    /**
     * Determine whether the packet is a login-related packet.
     *
     * @param packet the packet to check
     * @return true => the packet is related to login; false => otherwise.
     */
    private boolean isLoginPacket(Packet packet) {
        return (
                        // Despite its name, not part of the login handshake and is sent during the GAME state.  It needs to be forwarded.
                        //  (packet instanceof ClientboundLoginPacket) ||
                        (packet instanceof ClientboundLoginDisconnectPacket) ||
                        (packet instanceof ClientboundHelloPacket) ||
                        (packet instanceof ClientboundLoginFinishedPacket) ||
                        (packet instanceof ClientboundLoginCompressionPacket) ||
                        (packet instanceof ClientboundCustomQueryPacket) ||
                        (packet instanceof ClientboundCookieRequestPacket) ||
                        (packet instanceof ServerboundHelloPacket) ||
                        (packet instanceof ServerboundKeyPacket) ||
                        (packet instanceof ServerboundCustomQueryAnswerPacket) ||
                        (packet instanceof ServerboundLoginAcknowledgedPacket)
        );
    }
}
