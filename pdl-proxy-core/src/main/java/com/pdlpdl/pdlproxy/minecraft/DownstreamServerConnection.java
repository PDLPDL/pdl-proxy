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

package com.pdlpdl.pdlproxy.minecraft;

import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;

import java.util.Set;

public interface DownstreamServerConnection {

    void send(Packet packet);
    void disconnect(String reason);
    void waitForState(Set<ProtocolState> targetStateSet, long expiration);

    /**
     * Safely switch the inbound connection state.
     * @param newInboundState
     */
    void switchInboundState(ProtocolState newInboundState);

    /**
     * Safely switch the outbound connection state.
     * @param newOutboundState
     */
    void switchOutboundState(ProtocolState newOutboundState);
}
