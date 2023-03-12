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

package com.pdlpdl.pdlproxy.minecraft.main.poc;

import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import com.pdlpdl.pdlproxy.minecraft.api.PacketInterceptor;
import com.pdlpdl.pdlproxy.minecraft.api.ProxyPacketControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class PhantomBeePacketInterceptor implements PacketInterceptor {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(PhantomBeePacketInterceptor.class);

    private Logger log = DEFAULT_LOGGER;

    private double playerX;
    private double playerY;
    private double playerZ;
    private AtomicInteger entityIdGenerator = new AtomicInteger(777000);

    @Override
    public void onInterceptorInstalled(Session session) {
        this.log.debug("BEE interceptor installed");
    }

    @Override
    public void onInterceptorRemoved(Session session) {
    }

    @Override
    public void onClientPacketReceived(Packet clientPacket, ProxyPacketControl proxyPacketControl) {
        if (clientPacket instanceof ServerboundMovePlayerPosPacket) {
            ServerboundMovePlayerPosPacket clientPlayerPositionPacket = (ServerboundMovePlayerPosPacket) clientPacket;

            playerX = clientPlayerPositionPacket.getX();
            playerY = clientPlayerPositionPacket.getY();
            playerZ = clientPlayerPositionPacket.getZ();
        }

        if (clientPacket instanceof ServerboundChatPacket) {
            ServerboundChatPacket clientChatPacket = (ServerboundChatPacket) clientPacket;

            if (clientChatPacket.getMessage().contains("BEE")) {
                this.log.debug("BEE!!!");
                ClientboundAddEntityPacket spawnPhantomBeePacket = new ClientboundAddEntityPacket(
                        entityIdGenerator.getAndIncrement(),
                        UUID.randomUUID(),
                        EntityType.BEE,
                        playerX + 2,
                        playerY + 1,
                        playerZ + 2,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0
                );
                proxyPacketControl.addPacketToClient(spawnPhantomBeePacket);
            }
        }
    }

    @Override
    public void onServerPacketReceived(Packet serverPacket, ProxyPacketControl proxyPacketControl) {

    }

    @Override
    public void onPacketSentToClient(Packet clientBoundPacket) {
        if (clientBoundPacket instanceof ClientboundAddEntityPacket) {
            ClientboundAddEntityPacket spawnPacket = (ClientboundAddEntityPacket) clientBoundPacket;
            if (spawnPacket.getType() == EntityType.BEE) {
                this.log.info("BEE SPAWN sent to client!");
            }
        }
    }

    @Override
    public void onPacketSentToServer(Packet serverBoundPacket) {

    }
}
