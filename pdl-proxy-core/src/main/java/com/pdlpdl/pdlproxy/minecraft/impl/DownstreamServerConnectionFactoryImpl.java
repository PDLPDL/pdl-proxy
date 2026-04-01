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

import com.pdlpdl.pdlproxy.minecraft.ProxyEventListener;
import org.geysermc.mcprotocollib.auth.SessionService;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import com.pdlpdl.pdlproxy.minecraft.DownstreamServerConnection;
import com.pdlpdl.pdlproxy.minecraft.DownstreamServerConnectionFactory;
import com.pdlpdl.pdlproxy.minecraft.ProxyPacketListener;
import org.geysermc.mcprotocollib.network.session.ClientNetworkSession;
import org.geysermc.mcprotocollib.protocol.ClientListener;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.handshake.HandshakeIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public class DownstreamServerConnectionFactoryImpl implements DownstreamServerConnectionFactory {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(DownstreamServerConnectionFactoryImpl.class);

    private Logger log = DEFAULT_LOGGER;

    private final String downstreamHost;
    private final int downstreamPort;

    public DownstreamServerConnectionFactoryImpl(String downstreamHost, int downstreamPort) {
        this.downstreamHost = downstreamHost;
        this.downstreamPort = downstreamPort;
    }

    /**
     * Create a Client connection to the downstream server using the given username and packet listener.
     *
     * Note that this currently does not support login with the downstream server.  The only challenge to supporting it
     * is getting access to the account id (email, not ign) and password.
     *
     * @param username ign (in-game-name) of the user connecting to the downstream server.
     * @param proxyPacketListener listener that listens to packets from the downstream connection.
     * @return
     */
    @Override
    public DownstreamServerConnection connect(
            String username,
            ProxyPacketListener proxyPacketListener,
            ProxyPacketListener proxyPacketSentListener,
            ProxyEventListener proxyPacketSendingListener,
            BiConsumer<ProxyServerSessionAdapter, DisconnectedEvent> onDisconnectListener
    ) {

        ClientNetworkSession session = this.prepareMinecraftSession(username);

        // Tell the protocol library to NOT automatically send the known-packs CONFIGURATION response; we will let
        //  the upstream client and downstream server handle all configuration.
        session.setFlag(MinecraftConstants.SEND_BLANK_KNOWN_PACKS_RESPONSE, false);

        this.wireSessionAdapter(
            session,
            proxyPacketListener,
            proxyPacketSentListener,
            onDisconnectListener,
            proxyPacketSendingListener);

        //
        // Connect now.
        //
        this.log.info("[ ] STARTING connection to downstream for user {}", username);
        session.connect();
        this.log.info("[x] STARTED connection to downstream for user {}", username);

        //
        // Return a new Connection object with the session.
        //
        DownstreamServerConnection result = new DownstreamServerConnectionImpl(session);

        return result;
    }

//========================================
// Internals
//----------------------------------------

    /**
     * Prepare a new Minecraft Client Session connecting to the downstream server, using the provided username.
     *
     * @param username ign (in-game-name) of the user connecting to the downstream server.
     * @return Client Session prepared to connect with the downstream server.
     */
    private ClientNetworkSession prepareMinecraftSession(String username) {
        MinecraftProtocol minecraftProtocol = new MinecraftProtocol(username);
        SocketAddress socketAddress = new InetSocketAddress(this.downstreamHost, this.downstreamPort);

        // Disable the default ClientListener and use our own custom listener.
        minecraftProtocol.setUseDefaultListeners(false);

        Executor packetExecutor = Executors.newSingleThreadExecutor();
        ClientNetworkSession client = new ClientNetworkSession(socketAddress, minecraftProtocol, packetExecutor, null, null);

        // Install our custom ClientListener
        this.wireSessionToInterceptClientListenerConfiguration(client);

        SessionService sessionService = new SessionService();
        sessionService.setProxy(null);

        return client;
    }

    /**
     * Wire a Session Adapter into the given session with the packet listener and disconnect listener.
     *
     * @param session
     * @param proxyPacketListener
     * @param onDisconnectListener
     */
    private void wireSessionAdapter(
            Session session,
            ProxyPacketListener proxyPacketListener,
            ProxyPacketListener proxyPacketSentListener,
            BiConsumer<ProxyServerSessionAdapter, DisconnectedEvent> onDisconnectListener,
            ProxyEventListener proxyEventListener
    ) {

        ProxyServerSessionAdapter proxyServerSessionAdapter =
                new ProxyServerSessionAdapter(
                    session,
                    proxyPacketListener,
                    proxyPacketSentListener,
                    proxyEventListener
                );

        proxyServerSessionAdapter.setOnDisconnectListener(onDisconnectListener);
        session.addListener(proxyServerSessionAdapter);
    }

    /**
     * Given a client network session, find the ClientListener in the session's listeners and wrap it with our own
     *  interceptor that custom handles Configuration.
     *
     * @param session
     */
    private void wireSessionToInterceptClientListenerConfiguration(ClientNetworkSession session) {
        // Prepare a ClientListener to handle Login and other "admin" stuff for us
        ClientListener clientListener = new ClientListener(HandshakeIntent.LOGIN);
        CustomClientListenerConfigurationInterceptor interceptor = new CustomClientListenerConfigurationInterceptor(clientListener);

        session.addListener(interceptor);
    }
}
