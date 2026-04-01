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

import org.geysermc.mcprotocollib.network.event.server.ServerBoundEvent;
import org.geysermc.mcprotocollib.network.event.server.ServerClosedEvent;
import org.geysermc.mcprotocollib.network.event.server.ServerClosingEvent;
import org.geysermc.mcprotocollib.network.event.server.ServerListener;
import org.geysermc.mcprotocollib.network.event.server.SessionAddedEvent;
import org.geysermc.mcprotocollib.network.event.server.SessionRemovedEvent;
import org.geysermc.mcprotocollib.network.server.NetworkServer;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.function.Consumer;


/**
 * Hollow Minecraft Server which accepts connections with clients, and performs the login handshake.
 */
public class HollowMinecraftServer {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(HollowMinecraftServer.class);

    private Logger log = DEFAULT_LOGGER;

    private String listenHost = "0.0.0.0";
    private int listenPort = 7777;
    private boolean onlineMode = true;

    private NetworkServer server;

    private Consumer<SessionAddedEvent> onSessionAdded;
    private Consumer<SessionRemovedEvent> onSessionRemoved;

//========================================
// Getters and Setters
//----------------------------------------

    public String getListenHost() {
        return listenHost;
    }

    public void setListenHost(String listenHost) {
        this.listenHost = listenHost;
    }

    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public Consumer<SessionAddedEvent> getOnSessionAdded() {
        return onSessionAdded;
    }

    public void setOnSessionAdded(Consumer<SessionAddedEvent> onSessionAdded) {
        this.onSessionAdded = onSessionAdded;
    }

    public Consumer<SessionRemovedEvent> getOnSessionRemoved() {
        return onSessionRemoved;
    }

    public void setOnSessionRemoved(Consumer<SessionRemovedEvent> onSessionRemoved) {
        this.onSessionRemoved = onSessionRemoved;
    }

    public boolean isOnlineMode() {
        return onlineMode;
    }

    public void setOnlineMode(boolean onlineMode) {
        this.onlineMode = onlineMode;
    }

//========================================
// Operation
//----------------------------------------

    public void start() {
        SocketAddress socketAddress = new InetSocketAddress(this.listenHost, this.listenPort);
        this.server = new NetworkServer(socketAddress, () -> {
            MinecraftProtocol result = new MinecraftProtocol();

            // Turn off the default listeners for the sessions on this server.  Login, configuration, and other
            //  processing by the library are disabled.
            result.setUseDefaultListeners(false);
            return result;
        });
        this.server.setGlobalFlag(MinecraftConstants.ENCRYPT_CONNECTION, true);

        if (this.onlineMode) {
            this.server.setGlobalFlag(MinecraftConstants.SHOULD_AUTHENTICATE, true);
            this.log.info("PDL PROXY SERVER -- ONLINE");
        } else {
            this.server.setGlobalFlag(MinecraftConstants.SHOULD_AUTHENTICATE, false);
            this.log.info("PDL PROXY SERVER -- OFFLINE");
        }

        this.server.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, 100);

        // Disable the library sending of blank "Known Packs" automatically; we let the real client and real server
        //  negotiate the known packs.
        this.server.setGlobalFlag(MinecraftConstants.SEND_BLANK_KNOWN_PACKS_RESPONSE, false);
        // server.setGlobalFlag(SERVER_INFO_BUILDER_KEY, (ServerInfoBuilder) session -> SERVER_INFO);
        // server.setGlobalFlag(SERVER_LOGIN_HANDLER_KEY, (ServerLoginHandler) session -> session.send(JOIN_GAME_PACKET));

        //
        // Add the server listener.
        //
        this.server.addListener(new HollowServerListener());

        //
        // Start and bind the server.  Don't wait for it to finish startup.
        //
        this.server.bind(false);
    }

    public void shutdown() {
        if (this.server != null) {
            this.server.close(false);
            this.server = null;
        }
    }

//========================================
// Handlers
//----------------------------------------

    /**
     * Handle the "server bound" event which indicates the server is now listening for incoming Client connections.
     */
    private void handleServerBound(ServerBoundEvent serverBoundEvent) {
        this.log.info("Hollow Minecraft Server bound at {}:{}", this.listenHost, this.listenPort);
    }

    /**
     * Handle the "server closing" event which indicates the server is starting to shutdown.
     */
    private void handleServerClosing(ServerClosingEvent serverClosingEvent) {
        this.log.info("Hollow Minecraft Server closing...");
    }

    /**
     * Handle the "server closed" event which indicates the server has completed shutting down.
     */
    private void handleServerClosed(ServerClosedEvent serverClosedEvent) {
        this.log.info("Hollow Minecraft Server CLOSED.");
    }

    /**
     * Handle a "session added" event which indicates a Client has connected to the server.
     *
     * @param sessionAddedEvent
     */
    private void handleSessionAdded(SessionAddedEvent sessionAddedEvent) {
        this.log.debug("Session added from {}", sessionAddedEvent.getSession().getRemoteAddress());

        // Add out custom "server listener" (which is our server-side handling).
        CustomServerListenerConfigurationInterceptor interceptor = new CustomServerListenerConfigurationInterceptor();
        sessionAddedEvent.getSession().addListener(interceptor);

        if (this.onSessionAdded != null) {
            this.onSessionAdded.accept(sessionAddedEvent);
        }
    }

    /**
     * Handle a "session removed" event which indicates a Client has disconnected from the server.
     *
     * @param sessionRemovedEvent
     */
    private void handleSessionRemoved(SessionRemovedEvent sessionRemovedEvent) {
        this.log.debug("Session removed from {}", sessionRemovedEvent.getSession().getRemoteAddress());

        if (this.onSessionRemoved != null) {
            this.onSessionRemoved.accept(sessionRemovedEvent);
        }
    }


//========================================
// Internal Classes
//----------------------------------------

    /**
     * Listener of Minecraft Server Events.  Simply calls back into the handler methods above.
     */
    private class HollowServerListener implements ServerListener {
        @Override
        public void serverBound(ServerBoundEvent event) {
            HollowMinecraftServer.this.handleServerBound(event);
        }

        @Override
        public void serverClosing(ServerClosingEvent event) {
            HollowMinecraftServer.this.handleServerClosing(event);
        }

        @Override
        public void serverClosed(ServerClosedEvent event) {
            HollowMinecraftServer.this.handleServerClosed(event);
        }

        @Override
        public void sessionAdded(SessionAddedEvent event) {
            HollowMinecraftServer.this.handleSessionAdded(event);
        }

        @Override
        public void sessionRemoved(SessionRemovedEvent event) {
            HollowMinecraftServer.this.handleSessionRemoved(event);
        }
    }
}
