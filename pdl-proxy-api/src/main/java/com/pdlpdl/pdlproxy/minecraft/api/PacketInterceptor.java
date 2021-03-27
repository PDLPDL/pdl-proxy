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

package com.pdlpdl.pdlproxy.minecraft.api;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;

/**
 * Interceptor which enables packet flow to be monitored or altered.  All of the listeners operate on the basis of
 * "proceed normally until told otherwise" basis.
 *
 * To install interceptors, use a SessionInterceptor and add the interceptor when new sessions are added.
 */
public interface PacketInterceptor {
    /**
     * Notification that this interceptor instance has been installed on a Player session.
     */
    void onInterceptorInstalled(Session session);

    /**
     * Notification that this interceptor instance has been removed from a Player session.
     */
    void onInterceptorRemoved(Session session);

    /**
     * Intercept a packet received from a Player session.
     *
     * @param clientPacket
     * @param proxyPacketControl
     */
    void onClientPacketReceived(Packet clientPacket, ProxyPacketControl proxyPacketControl);

    /**
     * Intercept a packet received from the server, destined for a Player session.
     *
     * @param serverPacket
     * @param proxyPacketControl
     */
    void onServerPacketReceived(Packet serverPacket, ProxyPacketControl proxyPacketControl);

    /**
     * Notification that a packet has finished being sent to a Player session.  Note that "finished sending" does not
     * necessarily mean the client received it - just that it should be in the transport (i.e. TCP) stream.
     *
     * @param clientBoundPacket
     */
    void onPacketSentToClient(Packet clientBoundPacket);

    /**
     * Notification that a packet has finished being sent to the server.  Note that "finished sending" does not
     * necessarily mean the server received it - just that it should be in the transport (i.e. TCP) stream.
     *
     * @param serverBoundPacket
     */
    void onPacketSentToServer(Packet serverBoundPacket);
}
