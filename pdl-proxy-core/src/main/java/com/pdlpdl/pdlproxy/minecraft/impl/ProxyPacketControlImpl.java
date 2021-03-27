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

import com.github.steveice10.packetlib.packet.Packet;
import com.pdlpdl.pdlproxy.minecraft.api.ProxyPacketControl;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class ProxyPacketControlImpl implements ProxyPacketControl {

    private boolean dropPacket = false;
    private final Consumer<Packet> clientDirectPacketHandler;
    private final Consumer<Packet> serverDirectPacketHandler;
    private List<Packet> addedClientBoundPackets = new LinkedList<>();
    private List<Packet> addedServerBoundPackets = new LinkedList<>();

//========================================
// Constructor(s)
//----------------------------------------

    public ProxyPacketControlImpl(Consumer<Packet> clientDirectPacketHandler, Consumer<Packet> serverDirectPacketHandler) {
        this.clientDirectPacketHandler = clientDirectPacketHandler;
        this.serverDirectPacketHandler = serverDirectPacketHandler;
    }

//========================================
// Getters
//----------------------------------------

    public boolean isDropPacket() {
        return dropPacket;
    }

    public List<Packet> getAddedClientBoundPackets() {
        return addedClientBoundPackets;
    }

    public List<Packet> getAddedServerBoundPackets() {
        return addedServerBoundPackets;
    }

//========================================
// Proxy Packet Control Interface
//----------------------------------------

    @Override
    public void dropPacket() {
        this.dropPacket = true;
    }

    @Override
    public void addPacketToClient(Packet newPacket) {
        this.addedClientBoundPackets.add(newPacket);
    }

    @Override
    public void addPacketToServer(Packet newPacket) {
        this.addedServerBoundPackets.add(newPacket);
    }

    @Override
    public void directSendPacketToClient(Packet newPacket) {
        this.clientDirectPacketHandler.accept(newPacket);
    }

    @Override
    public void directSendPacketToServer(Packet newPacket) {
        this.serverDirectPacketHandler.accept(newPacket);
    }
}
