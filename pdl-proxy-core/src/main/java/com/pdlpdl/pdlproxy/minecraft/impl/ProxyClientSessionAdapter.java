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

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundCustomQueryPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundHelloPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundLoginCompressionPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundLoginDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundCustomQueryPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundHelloPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundKeyPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectingEvent;
import com.github.steveice10.packetlib.event.session.PacketErrorEvent;
import com.github.steveice10.packetlib.event.session.PacketSendingEvent;
import com.github.steveice10.packetlib.event.session.SessionListener;
import com.github.steveice10.packetlib.packet.Packet;
import com.pdlpdl.pdlproxy.minecraft.DownstreamServerConnection;
import com.pdlpdl.pdlproxy.minecraft.api.PacketInterceptor;
import com.pdlpdl.pdlproxy.minecraft.api.PacketInterceptorControl;
import com.pdlpdl.pdlproxy.minecraft.api.ProxyDirectPacketControlSupplier;
import com.pdlpdl.pdlproxy.minecraft.api.SessionLoginInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * SessionListener for upstream Client connections that forwards packets to the downstream server after initiating the
 * downstream connection on login success.
 */
public class ProxyClientSessionAdapter implements SessionListener, ProxyDirectPacketControlSupplier {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(ProxyClientSessionAdapter.class);
    private Logger log = DEFAULT_LOGGER;

    private static final Logger DEFAULT_PACKET_DATA_LOGGER = LoggerFactory.getLogger(ProxyClientSessionAdapter.class.getName() + ".packet-data");
    private Logger packetDataLog = DEFAULT_PACKET_DATA_LOGGER;


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

    private GameProfile downstreamGameProfile;

//========================================
// Constructor
//----------------------------------------

    public ProxyClientSessionAdapter(
            Function<IncomingClientSessionInfo, DownstreamServerConnection> startProxyServerSession,
            Session upstreamClientSession,
            PacketInterceptorControl packetInterceptorControl,
            SessionLoginInterceptor sessionLoginInterceptor) {

        this.startProxyServerSession = startProxyServerSession;
        this.upstreamClientSession = upstreamClientSession;
        this.sessionLoginInterceptor = sessionLoginInterceptor;

        //
        // Initialize Packet Interceptors Array.  Result is an unmodifiable list.  Note that it is feasible the list
        //  could be mutable - just make sure to use thread-safe operations if changing it as we could get inbound
        //  messages concurrently from client and server.
        //
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
        this.log.trace("received packet from client: class={}", packet.getClass().getName());
        this.handlePacketReceivedFromClient(session, packet);
    }

    @Override
    public void packetSending(PacketSendingEvent event) {
        this.log.trace("sending packet to client: class={}", event.getPacket().getClass().getName());

        //
        // Call the Session Login Interceptor now
        //
        if (event.getPacket() instanceof ClientboundGameProfilePacket) {
            // Contact the Session Login Interceptor first
            this.sessionLoginInterceptor.onPlayerLoginSuccessSending(event);
        }
    }

    @Override
    public void packetSent(Session session, Packet packet) {
        this.log.trace("sent packet to client: class={}", packet.getClass().getName());

        if (this.shutdownInd) {
            this.log.debug("have packet-sent after shutdown: race-condition?");
            return;
        }

        //
        // ON client login success (i.e. transition from LOGIN to GAME state)...
        //
        if (packet instanceof ClientboundGameProfilePacket) {
            ClientboundGameProfilePacket loginSuccessPacket = (ClientboundGameProfilePacket) packet;

            GameProfile gameProfile = loginSuccessPacket.getProfile();
            String username = gameProfile.getName();

            //
            // START the downstream session, to the SERVER (double-check we don't already have the connection).
            //
            this.connectDownstream(session, username);
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

    private void connectDownstream(Session session, String username) {
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
                            this::handleDownstreamDisconnect
                    );

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
        MinecraftProtocol minecraftProtocol = (MinecraftProtocol) upstreamClientSession.getPacketProtocol();

        //
        // Forward the packet to the client, but ONLY if the client is in the GAME state (i.e. not still logging in) and
        //  the packet is not a Server KeepAlive packet (our hollow server handles its own keep-alive packets).  Also
        //  filter out all Login-related packets.
        //

        this.log.debug("HAVE PACKET {}", packet.getClass().getSimpleName());
        this.packetDataLog.debug("HAVE PACKET {}", packet.getClass().getSimpleName());
        if (this.packetDataLog.isTraceEnabled()) {
            this.packetDataLog.trace("RECEIVED PACKET DATA = {}", packet);
        }

        if (packet instanceof ClientboundGameProfilePacket) {
            this.downstreamGameProfile = ((ClientboundGameProfilePacket) packet).getProfile();
        }

        if (this.shouldForwardPacketFromServer(packet, minecraftProtocol)) {
            this.log.debug("SHOULD FORWARD PACKET {}", packet.getClass().getSimpleName());
            boolean sendOriginal = this.applyReceivedPacketInterceptors(packet, false);
            if (sendOriginal) {
                this.log.debug("FORWARDING PACKET {}", packet.getClass().getSimpleName());
                this.upstreamClientSession.send(packet);
            }
        }
    }

    /**
     * Given a packet from the sent to the Server (such as those forwarded from the Client), notify listeners as-needed.
     *
     * @param packet
     */
    private void handlePacketSentToServer(Packet packet) {
        MinecraftProtocol minecraftProtocol = (MinecraftProtocol) upstreamClientSession.getPacketProtocol();

        if (this.shouldForwardPacketFromClient(packet, minecraftProtocol)) {
            this.notifySentPacketInterceptors(packet, false);
        }
    }

    private void handleDownstreamDisconnect(ProxyServerSessionAdapter proxyServerSessionAdapter,
                                            DisconnectedEvent disconnectedEvent) {

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

        this.log.debug("HAVE PACKET {}", packet.getClass().getSimpleName());

        MinecraftProtocol minecraftProtocol = (MinecraftProtocol) upstreamClientSession.getPacketProtocol();

        if (packet instanceof ServerboundKeyPacket) {
            this.log.debug("HAVE KEY PACKET FROM CLIENT");
        }

        if (this.shouldForwardPacketFromClient(packet, minecraftProtocol)) {
            this.log.debug("SHOULD FORWARD PACKET {}", packet.getClass().getSimpleName());

            boolean sendOriginal = this.applyReceivedPacketInterceptors(packet, true);
            if (sendOriginal) {
                this.log.debug("FORWARDING PACKET {}", packet.getClass().getSimpleName());
                this.downstreamServerConnection.send(packet);
            }
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
            this.upstreamClientSession.send(clientBoundPacket);
        }


        //
        // Inject any server-bound packets added by the interceptor
        //
        List<Packet> serverBoundInjectedPackets = proxyPacketControl.getAddedServerBoundPackets();
        for (Packet serverBoundPacket : serverBoundInjectedPackets) {
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
        return (
                    (this.upstreamClientSession.isConnected()) &&
                    (minecraftProtocol.getState() == ProtocolState.GAME) &&
                    (!(this.isLoginPacket(packet))) &&
                    (!(packet instanceof ClientboundKeepAlivePacket))
        );
    }

    private boolean shouldForwardPacketFromClient(Packet packet, MinecraftProtocol minecraftProtocol) {
        return (
                    (this.downstreamServerConnection != null) &&
                    (minecraftProtocol.getState() == ProtocolState.GAME) &&
                    (!this.isLoginPacket(packet)) &&
                    (!(packet instanceof ServerboundKeepAlivePacket))
        );
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
//                        (packet instanceof ClientboundLoginPacket) ||
                        (packet instanceof ClientboundLoginDisconnectPacket) ||
                        (packet instanceof ClientboundHelloPacket) ||
                        (packet instanceof ClientboundGameProfilePacket) ||
                        (packet instanceof ClientboundLoginCompressionPacket) ||
                        (packet instanceof ClientboundCustomQueryPacket) ||
                        (packet instanceof ServerboundHelloPacket) ||
                        (packet instanceof ServerboundKeyPacket) ||
                        (packet instanceof ServerboundCustomQueryPacket)
        );
    }
}
