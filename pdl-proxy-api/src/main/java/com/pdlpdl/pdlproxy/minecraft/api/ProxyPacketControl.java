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

import com.github.steveice10.packetlib.packet.Packet;

public interface ProxyPacketControl {
    /**
     * Drop the original packet from the proxy stream.
     */
    void dropPacket();

    /**
     * Add the given packet to the proxy stream of messages to the Client.  Note that the original packet is sent after
     * these are sent, unless dropPacket() is called by one (or more) of the interceptors.
     *
     * Also note that packets added by interceptors are NOT delivered to other interceptors.  This may change in the
     * future.
     *
     * @param newPacket packet to add to the stream.
     */
    void addPacketToClient(Packet newPacket);

    /**
     * Add the given packet to the proxy stream of messages to the Server.  Note that the original packet is sent after
     * these are sent, unless dropPacket() is called by one (or more) of the interceptors.
     *
     * Also note that packets added by interceptors are NOT delivered to other interceptors.  This may change in the
     * future.
     *
     * @param newPacket packet to add to the stream.
     */
    void addPacketToServer(Packet newPacket);

    /**
     * Directly / immediately send the given packet to the client.
     *
     * TODO: consider whether to separate this for async processing that uses it.
     *
     * @param newPacket
     */
    void directSendPacketToClient(Packet newPacket);


    /**
     * Directly / immediately send the given packet to the server.
     *
     * TODO: consider whether to separate this for async processing that uses it.
     *
     * @param newPacket
     */
    void directSendPacketToServer(Packet newPacket);
}
