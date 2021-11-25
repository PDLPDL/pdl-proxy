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

import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import com.pdlpdl.pdlproxy.minecraft.DownstreamServerConnection;
import com.pdlpdl.pdlproxy.minecraft.DownstreamServerConnectionFactory;
import com.pdlpdl.pdlproxy.minecraft.ProxyPacketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Proxy;
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
            BiConsumer<ProxyServerSessionAdapter, DisconnectedEvent> onDisconnectListener
    ) {

        Session session = this.prepareMinecraftSession(username);

        this.wireSessionAdapter(session, proxyPacketListener, proxyPacketSentListener, onDisconnectListener);

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
    private Session prepareMinecraftSession(String username) {
        MinecraftProtocol minecraftProtocol = new MinecraftProtocol(username);
        Session client = new TcpClientSession(downstreamHost, downstreamPort, minecraftProtocol);

        SessionService sessionService = new SessionService();
        sessionService.setProxy(Proxy.NO_PROXY);

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
            BiConsumer<ProxyServerSessionAdapter, DisconnectedEvent> onDisconnectListener
    ) {

        ProxyServerSessionAdapter proxyServerSessionAdapter =
                new ProxyServerSessionAdapter(session, proxyPacketListener, proxyPacketSentListener);

        proxyServerSessionAdapter.setOnDisconnectListener(onDisconnectListener);
        session.addListener(proxyServerSessionAdapter);
    }
}
