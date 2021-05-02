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

package com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.interceptor;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import com.pdlpdl.pdlproxy.minecraft.api.PacketInterceptor;
import com.pdlpdl.pdlproxy.minecraft.api.ProxyPacketControl;
import com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.MinecraftGameStateTracker;
import com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model.Position;
import com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model.Rotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Track game state.
 *
 * Currently tracks:
 *  - Player Position
 *  - Player Rotation
 *
 * Additional Tracking Being Considered:
 *  - Player Entity ID
 *  - Player Health
 *  - Players who have joined the server
 *  - Entities (id, position, rotation, etc)
 *  - Inventories
 *  - Block Positions (* - this could be processing and memory intensive)
 *
 * IMMUTABILITY:
 *  - Having immutable game-state is valuable because:
 *      - It guarantees readers of the state see consistent information (e.g. only see position information for entities that still exist)
 *      - Eliminates concurrency issues since the state cannot be mutated by concurrent threads
 */
public class GameStateTrackingPacketInterceptor implements PacketInterceptor, GameStateTrackerProvider {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(GameStateTrackingPacketInterceptor.class);

    private Logger log = DEFAULT_LOGGER;


    //
    // RUNTIME STATE
    //
    private MinecraftGameStateTracker minecraftGameStateTracker = new MinecraftGameStateTracker();


//========================================
// Getters and Setters
//----------------------------------------

    @Override
    public MinecraftGameStateTracker getMinecraftGameStateTracker() {
        return minecraftGameStateTracker;
    }

    public void setMinecraftGameStateTracker(MinecraftGameStateTracker minecraftGameStateTracker) {
        this.minecraftGameStateTracker = minecraftGameStateTracker;
    }

//========================================
// Interceptor Operations
//----------------------------------------

    @Override
    public void onInterceptorInstalled(Session session) {
    }

    @Override
    public void onInterceptorRemoved(Session session) {
    }

    @Override
    public void onClientPacketReceived(Packet clientPacket, ProxyPacketControl proxyPacketControl) {
        if (clientPacket instanceof ClientPlayerPositionPacket) {
            ClientPlayerPositionPacket clientPlayerPositionPacket = (ClientPlayerPositionPacket) clientPacket;

            Position newPosition = new Position(
                    clientPlayerPositionPacket.getX(),
                    clientPlayerPositionPacket.getY(),
                    clientPlayerPositionPacket.getZ()
            );

            this.minecraftGameStateTracker.updatePlayerPosition(newPosition, clientPlayerPositionPacket.isOnGround());
        } else if (clientPacket instanceof ClientPlayerRotationPacket) {
            ClientPlayerRotationPacket clientPlayerRotationPacket = (ClientPlayerRotationPacket) clientPacket;

            Rotation newRotation = new Rotation(
                    clientPlayerRotationPacket.getPitch(),
                    clientPlayerRotationPacket.getYaw(),
                    0.0 // Player's can't roll (i.e. tilt entire body to left or right)
            );

            this.minecraftGameStateTracker.updatePlayerRotation(
                    newRotation,
                    clientPlayerRotationPacket.isOnGround()
            );
        } else if (clientPacket instanceof ClientPlayerPositionRotationPacket) {
            ClientPlayerPositionRotationPacket clientPlayerPositionRotationPacket =
                    (ClientPlayerPositionRotationPacket) clientPacket;

            Position newPosition = new Position(
                    clientPlayerPositionRotationPacket.getX(),
                    clientPlayerPositionRotationPacket.getY(),
                    clientPlayerPositionRotationPacket.getZ()
            );
            Rotation newRotation = new Rotation(
                    clientPlayerPositionRotationPacket.getPitch(),
                    clientPlayerPositionRotationPacket.getYaw(),
                    0.0 // Player's can't roll (i.e. tilt entire body to left or right)
            );

            this.minecraftGameStateTracker
                    .updatePlayerPositionRotation(
                            newPosition,
                            newRotation,
                            clientPlayerPositionRotationPacket.isOnGround());
        }
    }

    @Override
    public void onServerPacketReceived(Packet serverPacket, ProxyPacketControl proxyPacketControl) {
        if (serverPacket instanceof ServerPlayerPositionRotationPacket) {
            ServerPlayerPositionRotationPacket serverPlayerPositionRotationPacket =
                    (ServerPlayerPositionRotationPacket) serverPacket;

            Position newPosition = new Position(
                    serverPlayerPositionRotationPacket.getX(),
                    serverPlayerPositionRotationPacket.getY(),
                    serverPlayerPositionRotationPacket.getZ()
            );

            Rotation newRotation = new Rotation(
                    serverPlayerPositionRotationPacket.getPitch(),
                    serverPlayerPositionRotationPacket.getYaw(),
                    0.0 // Player's can't roll (i.e. tilt entire body to left or right)
            );

            this.minecraftGameStateTracker.updatePlayerPositionRotation(newPosition, newRotation, null);
        } else if (serverPacket instanceof ServerJoinGamePacket) {
            ServerJoinGamePacket serverJoinGamePacket = (ServerJoinGamePacket) serverPacket;

            this.minecraftGameStateTracker.updatePlayerEntityId(serverJoinGamePacket.getEntityId());
        }
    }

    @Override
    public void onPacketSentToClient(Packet clientBoundPacket) {
    }

    @Override
    public void onPacketSentToServer(Packet serverBoundPacket) {
    }
}
