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

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import com.pdlpdl.pdlproxy.minecraft.DownstreamServerConnection;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;

import java.util.Set;

public class DownstreamServerConnectionImpl implements DownstreamServerConnection {

    private final Session downstreamSession;
    private final MinecraftProtocolStateWaiter minecraftProtocolStateWaiter;

//========================================
// Constructor
//----------------------------------------

    public DownstreamServerConnectionImpl(Session downstreamSession) {
        this.downstreamSession = downstreamSession;
        this.minecraftProtocolStateWaiter = new MinecraftProtocolStateWaiter(this.downstreamSession.getPacketProtocol());
    }

//========================================
// Operations
//----------------------------------------

    @Override
    public void send(Packet packet) {
        this.downstreamSession.send(packet);
    }

    @Override
    public void disconnect(String reason) {
        this.downstreamSession.disconnect(reason);
    }

    @Override
    public void waitForState(Set<ProtocolState> targetStateSet, long expiration) {
        this.minecraftProtocolStateWaiter.waitForOutboundStateWithTimeout(targetStateSet, expiration);
    }

    @Override
    public void switchInboundState(ProtocolState newInboundState) {
        MinecraftProtocol packetProtocol = this.downstreamSession.getPacketProtocol();
        this.downstreamSession.switchInboundState(() -> packetProtocol.setInboundState(newInboundState));
    }

    @Override
    public void switchOutboundState(ProtocolState newOutboundState) {
        MinecraftProtocol packetProtocol = this.downstreamSession.getPacketProtocol();
        this.downstreamSession.switchInboundState(() -> packetProtocol.setOutboundState(newOutboundState));
    }
}
