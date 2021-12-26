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
import com.github.steveice10.packetlib.event.server.SessionAddedEvent;
import com.github.steveice10.packetlib.event.server.SessionRemovedEvent;
import com.github.steveice10.packetlib.event.session.PacketSendingEvent;
import com.pdlpdl.pdlproxy.minecraft.DownstreamServerConnection;
import com.pdlpdl.pdlproxy.minecraft.DownstreamServerConnectionFactory;
import com.pdlpdl.pdlproxy.minecraft.api.PacketInterceptor;
import com.pdlpdl.pdlproxy.minecraft.api.PacketInterceptorControl;
import com.pdlpdl.pdlproxy.minecraft.api.ProxyDirectPacketControl;
import com.pdlpdl.pdlproxy.minecraft.api.ProxyDirectPacketControlAware;
import com.pdlpdl.pdlproxy.minecraft.api.ProxyServer;
import com.pdlpdl.pdlproxy.minecraft.api.SessionInterceptor;
import com.pdlpdl.pdlproxy.minecraft.api.SessionInterceptorControl;
import com.pdlpdl.pdlproxy.minecraft.api.SessionLoginInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Proxy Server that starts listening for inbound connections from Upstream clients, creates an outbound connection to
 * a downstream Server and copies packets between the endpoints.
 *
 * To install interceptors:
 *      void installInterceptor(ProxyServer proxyServer, SessionInterceptor interceptor) {
 *          SessionInterceptorControl control = proxyServer.getServerInterceptor();
 *          control.addInterceptorAtEnd(interceptor);
 *      }
 */
public class ProxyServerImpl implements ProxyServer {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(ProxyServerImpl.class);

    private Logger log = DEFAULT_LOGGER;

    private final Object sync = new Object();

    //
    // Configuration
    //
    private String serverListenHost;
    private int serverListenPort;
    private boolean serverOnlineMode = true;

    private String downstreamServerHostname;
    private int downstreamServerPort;

    //
    // Runtime State
    //
    private SessionInterceptorControl sessionInterceptorControl;
    private HollowMinecraftServer hollowMinecraftServer;
    private DownstreamServerConnectionFactory downstreamServerConnectionFactory;
    private Map<Session, ProxyClientSessionState> sessionStateMap = new HashMap<>();
    private List<SessionLoginInterceptor> loginInterceptorList = new LinkedList<>();

//========================================
// Constructor
//----------------------------------------

    public ProxyServerImpl() {
        this.prepareSessionInterceptorControl();
    }

//========================================
// Getters and Setters
//----------------------------------------

    public String getServerListenHost() {
        return serverListenHost;
    }

    public void setServerListenHost(String serverListenHost) {
        this.serverListenHost = serverListenHost;
    }

    public int getServerListenPort() {
        return serverListenPort;
    }

    public void setServerListenPort(int serverListenPort) {
        this.serverListenPort = serverListenPort;
    }

    public String getDownstreamServerHostname() {
        return downstreamServerHostname;
    }

    public void setDownstreamServerHostname(String downstreamServerHostname) {
        this.downstreamServerHostname = downstreamServerHostname;
    }

    public int getDownstreamServerPort() {
        return downstreamServerPort;
    }

    public void setDownstreamServerPort(int downstreamServerPort) {
        this.downstreamServerPort = downstreamServerPort;
    }

    public List<SessionLoginInterceptor> getLoginInterceptorList() {
        return loginInterceptorList;
    }

    public void setLoginInterceptorList(List<SessionLoginInterceptor> loginInterceptorList) {
        this.loginInterceptorList = loginInterceptorList;
    }

    public boolean isServerOnlineMode() {
        return serverOnlineMode;
    }

    public void setServerOnlineMode(boolean serverOnlineMode) {
        this.serverOnlineMode = serverOnlineMode;
    }

//========================================
// Proxy Server Interface
//----------------------------------------

    @Override
    public void start() {
        this.prepareHollowServer();
        this.prepareDownstreamConnectionFactory();

        this.hollowMinecraftServer.start();
    }

    @Override
    public void shutdown() {
        //
        // Shutdown out internal Server.  This will close all of the Client sessions from upstream.
        //
        if (this.hollowMinecraftServer != null) {
            this.hollowMinecraftServer.shutdown();
        }

        //
        // Shutdown our proxy Client sessions to the downstream server.
        //
        this.shutdownClientSessions();
    }

    @Override
    public SessionInterceptorControl getSessionInterceptorControl() {
        return this.sessionInterceptorControl;
    }

    @Override
    public void addSessionLoginInterceptor(SessionLoginInterceptor sessionLoginInterceptor) {
        synchronized (this.sync) {
            this.loginInterceptorList.add(sessionLoginInterceptor);
        }
    }

//========================================
//  Initialize Runtime State
//----------------------------------------

    private void prepareSessionInterceptorControl() {
        this.sessionInterceptorControl = new SessionInterceptorControlImpl();
    }

    private void prepareHollowServer() {
        this.hollowMinecraftServer = new HollowMinecraftServer();
        this.hollowMinecraftServer.setListenHost(this.serverListenHost);
        this.hollowMinecraftServer.setListenPort(this.serverListenPort);
        this.hollowMinecraftServer.setOnlineMode(this.serverOnlineMode);
        this.hollowMinecraftServer.setOnSessionAdded(this::handleSessionAdded);
        this.hollowMinecraftServer.setOnSessionRemoved(this::handleSessionRemoved);
    }

    private void prepareDownstreamConnectionFactory() {
        this.downstreamServerConnectionFactory =
                new DownstreamServerConnectionFactoryImpl(this.downstreamServerHostname, this.downstreamServerPort);
    }

//========================================
// Event Handlers
//----------------------------------------

    private void handleSessionAdded(SessionAddedEvent sessionAddedEvent) {
        this.log.info("CLIENT SESSION created from {}:{}",
                sessionAddedEvent.getSession().getHost(), sessionAddedEvent.getSession().getPort());

        Session upstreamClientSession = sessionAddedEvent.getSession();

        //
        // Apply the Session Interceptors and collect the Packet Interceptors.
        //
        PacketInterceptorControl packetInterceptorControl = new PacketInterceptorControlImpl();
        for (SessionInterceptor sessionInterceptor : this.sessionInterceptorControl.getInterceptorIterable()) {
            sessionInterceptor.onSessionAdded(sessionAddedEvent.getSession(), packetInterceptorControl);
        }

        //
        // Create the Proxy Session Adapter and add it as a listener to the new Client Session.
        //
        ProxyClientSessionAdapter proxyClientSessionAdapter =
                new ProxyClientSessionAdapter(this::connectDownstream, upstreamClientSession, packetInterceptorControl,
                        this::executeSessionLoginInterceptors);


        //
        // Inject the proxy direct packet control into the downstream for any session or packet interceptors that expose
        //  the ProxyDirectPacketControlAware interface.
        //
        this.callDirectPacketAwareInterceptors(
                proxyClientSessionAdapter.getProxyDirectPacketControl(),
                sessionInterceptorControl.getInterceptorIterable(),
                packetInterceptorControl);


        //
        // Now that everything is prepared, wire this adapter into the upstream client session (this listener receives
        //  packets as well as connected/disconnected events).
        //
        upstreamClientSession.addListener(proxyClientSessionAdapter);

        //
        // Remember this Session
        //
        synchronized (this.sync) {
            ProxyClientSessionState state = new ProxyClientSessionState(upstreamClientSession, proxyClientSessionAdapter);
            this.sessionStateMap.put(upstreamClientSession, state);
        }
    }

    private void callDirectPacketAwareInterceptors(
            ProxyDirectPacketControl proxyDirectPacketControl,
            Iterable<SessionInterceptor> sessionInterceptorIterable,
            PacketInterceptorControl packetInterceptorControl) {

        //
        // First do the session interceptors
        //
        for (SessionInterceptor sessionInterceptor : this.sessionInterceptorControl.getInterceptorIterable()) {
            if (sessionInterceptor instanceof ProxyDirectPacketControlAware) {
                ((ProxyDirectPacketControlAware) sessionInterceptor)
                        .injectProxyDirectPacketControl(proxyDirectPacketControl);
            }
        }

        //
        // Next the packet interceptors
        //
        for (PacketInterceptor packetInterceptor : packetInterceptorControl.getInterceptorIterable()) {
            if (packetInterceptor instanceof ProxyDirectPacketControlAware) {
                ((ProxyDirectPacketControlAware) packetInterceptor)
                        .injectProxyDirectPacketControl(proxyDirectPacketControl);
            }
        }

    }

    private void handleSessionRemoved(SessionRemovedEvent sessionRemovedEvent) {
        this.log.info("CLIENT SESSION removed from {}:{}",
                sessionRemovedEvent.getSession().getHost(), sessionRemovedEvent.getSession().getPort());

        Session removedSession = sessionRemovedEvent.getSession();
        ProxyClientSessionState sessionState;

        synchronized (this.sync) {
            sessionState = this.sessionStateMap.get(removedSession);
        }

        if (sessionState != null) {
            //
            // Shutdown the proxy adapter, which will in-turn shutdown the associated downstream connection.
            //
            sessionState.getProxyClientSessionAdapter().shutdown("proxy server removed client session");
        }

        //
        // Notify listeners of the removal
        //
        for (SessionInterceptor sessionInterceptor : this.sessionInterceptorControl.getInterceptorIterable()) {
            sessionInterceptor.onSessionRemoved(sessionRemovedEvent.getSession());
        }
    }

//========================================
// Downstream Server Setup / Shutdown
//----------------------------------------

    private DownstreamServerConnection connectDownstream(IncomingClientSessionInfo incomingClientSessionInfo) {
        DownstreamServerConnection result =
                this.downstreamServerConnectionFactory.connect(
                        incomingClientSessionInfo.getUsername(),
                        incomingClientSessionInfo.getProxyPacketReceivedListener(),
                        incomingClientSessionInfo.getProxyPacketSentListener(),
                        incomingClientSessionInfo.getOnDisconnectListener());

        for (SessionInterceptor sessionInterceptor : this.sessionInterceptorControl.getInterceptorIterable()) {
            sessionInterceptor.onDownstreamConnected(
                    incomingClientSessionInfo.getClientSession(),
                    incomingClientSessionInfo.getUsername());
        }

        return result;
    }

    private void shutdownClientSessions() {
        Map<Session, ProxyClientSessionState> activeSessions;

        synchronized (this.sync) {
            activeSessions = this.sessionStateMap;
            this.sessionStateMap = null;
        }

        //
        // Loop through the active sessions and shutdown the downstream for each.  Don't worry about upstream; that is
        //  handled earlier.
        //
        for (ProxyClientSessionState proxyClientSessionState : activeSessions.values()) {
            proxyClientSessionState.getProxyClientSessionAdapter().shutdown("proxy server shutdown");
        }
    }

    /**
     * Call all of the login-success interceptors for the given login success sending event.
     *
     * @param loginSuccessSendingEvent the event with the LoginSuccessPacket about to be sent.
     */
    private void executeSessionLoginInterceptors(PacketSendingEvent loginSuccessSendingEvent) {
        List<SessionLoginInterceptor> interceptors = new LinkedList<>();
        synchronized (this.sync) {
            interceptors.addAll(this.loginInterceptorList);
        }

        for (SessionLoginInterceptor oneInterceptor : interceptors) {
            oneInterceptor.onPlayerLoginSuccessSending(loginSuccessSendingEvent);
        }
    }
}
