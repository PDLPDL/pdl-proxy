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

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectingEvent;
import com.github.steveice10.packetlib.event.session.PacketErrorEvent;
import com.github.steveice10.packetlib.event.session.PacketSendingEvent;
import com.github.steveice10.packetlib.event.session.SessionListener;
import com.github.steveice10.packetlib.packet.Packet;
import com.pdlpdl.pdlproxy.minecraft.ProxyPacketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

/**
 * SessionListener for upstream Client connections that forwards packets to the downstream server after initiating the
 * downstream connection on login success.
 */
public class ProxyServerSessionAdapter implements SessionListener {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(ProxyServerSessionAdapter.class);
    private Logger log = DEFAULT_LOGGER;

    private final Session downstreamServerSession;
    private final ProxyPacketListener onPacketReceivedListener;
    private final ProxyPacketListener onPacketSentListener;

    private BiConsumer<ProxyServerSessionAdapter, DisconnectedEvent> onDisconnectListener;


//========================================
// Constructor
//----------------------------------------

    public ProxyServerSessionAdapter(Session downstreamServerSession, ProxyPacketListener onPacketReceivedListener, ProxyPacketListener onPacketSentListener) {
        this.downstreamServerSession = downstreamServerSession;
        this.onPacketReceivedListener = onPacketReceivedListener;
        this.onPacketSentListener = onPacketSentListener;
    }


//========================================
// Getters and Setters
//----------------------------------------

    public BiConsumer<ProxyServerSessionAdapter, DisconnectedEvent> getOnDisconnectListener() {
        return onDisconnectListener;
    }

    public void setOnDisconnectListener(BiConsumer<ProxyServerSessionAdapter, DisconnectedEvent> onDisconnectListener) {
        this.onDisconnectListener = onDisconnectListener;
    }


//========================================
// Lifecycle
//----------------------------------------

    /**
     * Shutdown this session adapter, and the associated downstream server session.
     */
    public void shutdown() {
        if (this.downstreamServerSession != null) {
            this.downstreamServerSession.disconnect("shutting down");
        }
    }

//========================================
// SessionListener
//----------------------------------------

    @Override
    public void packetReceived(Session session, Packet packet) {
        this.log.trace("received packet from server: class={}", packet.getClass().getName());

        this.handlePacketFromServer(packet);
    }

    @Override
    public void packetSending(PacketSendingEvent event) {
        this.log.trace("sending packet to server: class={}", event.getPacket().getClass().getName());
    }

    @Override
    public void packetSent(Session session, Packet packet) {
        this.log.trace("sent packet to server: class={}", packet.getClass().getName());

        this.handlePacketSentToServer(packet);
    }

    @Override
    public void packetError(PacketErrorEvent event) {
        this.log.error("Packet handling error", event.getCause());
    }

    @Override
    public void connected(ConnectedEvent event) {
        this.log.info("Proxy server Session CONNECTED");
    }

    @Override
    public void disconnecting(DisconnectingEvent event) {
        this.log.info("Proxy server Session Disconnecting!");
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        this.log.info("Proxy server Session DISCONNECTED! {}", event.getReason(), event.getCause());

        if (this.onDisconnectListener != null) {
            this.onDisconnectListener.accept(this, event);
        }
    }


//========================================
// Internals
//----------------------------------------

    /**
     * Given a packet from the downstream Server, forward the packet to the upstream Client, as appropriate.
     *
     * SEE the ProxyClientSessionAdapter which holds all of the filtering logic.
     *
     * @param packet
     */
    private void handlePacketFromServer(Packet packet) {
        this.onPacketReceivedListener.handlePacket(packet);
    }

    /**
     * Given a packet that was sent to the Server, notify listeners.
     *
     * @param packet
     */
    private void handlePacketSentToServer(Packet packet) {
        this.onPacketSentListener.handlePacket(packet);
    }
}
