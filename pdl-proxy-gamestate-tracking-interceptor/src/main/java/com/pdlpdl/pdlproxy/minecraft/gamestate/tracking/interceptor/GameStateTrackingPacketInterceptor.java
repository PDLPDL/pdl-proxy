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

import com.pdlpdl.pdlproxy.minecraft.gamestate.datagen.MinecraftDatagenManager;
import com.pdlpdl.pdlproxy.minecraft.gamestate.datagen.model.DatagenDimensionType;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import com.pdlpdl.pdlproxy.minecraft.api.GameProfileAwareInterceptor;
import com.pdlpdl.pdlproxy.minecraft.api.PacketInterceptor;
import com.pdlpdl.pdlproxy.minecraft.api.ProxyPacketControl;
import com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.MinecraftGameStateTracker;
import com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model.Position;
import com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model.Rotation;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundBlockUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundForgetLevelChunkPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSectionBlocksUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

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
public class GameStateTrackingPacketInterceptor implements PacketInterceptor, GameStateTrackerProvider, GameProfileAwareInterceptor {

    public static final String MIN_Y_CODEC_TAG = "min_y";
    public static final String HEIGHT_CODEC_TAG = "height";
    public static final String DIMENSION_TYPE_CODEC_TAG = "minecraft:dimension_type";

    public static final long WARN_WORLD_HEIGHT_UNKNOWN_PERIOD_LENGTH = 300L; // 5 minutes in seconds

    private static AtomicLong lastWarnWorldHeightUnknown = new AtomicLong(0);  // Rate-limit the warning when world-height == Integer.MIN_VALUE

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(GameStateTrackingPacketInterceptor.class);

    private Logger log = DEFAULT_LOGGER;


    //
    // WORLD STATE (does not change while server is running)
    //
    private int worldMinY = Integer.MIN_VALUE;
    private int worldHeight = Integer.MIN_VALUE;

    private final MinecraftDatagenManager minecraftDatagenManager;

    //
    // RUNTIME STATE
    //
    private GameProfile gameProfile;
    private MinecraftGameStateTracker minecraftGameStateTracker = new MinecraftGameStateTracker();
    private Map<String, Integer> registryOverrideHeightByDimension = new HashMap<>();
    private Map<String, Integer> registryOverrideMinYByDimension = new HashMap<>();


//========================================
// Constructors
//----------------------------------------

    public GameStateTrackingPacketInterceptor(MinecraftDatagenManager minecraftDatagenManager) {
        this.minecraftDatagenManager = minecraftDatagenManager;
    }


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
// GameProfileAwareInterceptor Operations
//----------------------------------------

    @Override
    public void injectGameProfile(GameProfile gameProfile) {
        this.gameProfile = gameProfile;

        this.minecraftGameStateTracker.updatePlayerProfileInfo(gameProfile.getName(), gameProfile.getIdAsString());
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
        if (clientPacket instanceof ServerboundMovePlayerPosPacket) {
            ServerboundMovePlayerPosPacket clientPlayerPositionPacket = (ServerboundMovePlayerPosPacket) clientPacket;

            Position newPosition = new Position(
                    clientPlayerPositionPacket.getX(),
                    clientPlayerPositionPacket.getY(),
                    clientPlayerPositionPacket.getZ()
            );

            this.minecraftGameStateTracker.updatePlayerPosition(newPosition, clientPlayerPositionPacket.isOnGround());
        } else if (clientPacket instanceof ServerboundMovePlayerRotPacket) {
            ServerboundMovePlayerRotPacket clientPlayerRotationPacket = (ServerboundMovePlayerRotPacket) clientPacket;

            Rotation newRotation = new Rotation(
                    clientPlayerRotationPacket.getPitch(),
                    clientPlayerRotationPacket.getYaw(),
                    0.0 // Player's can't roll (i.e. tilt entire body to left or right)
            );

            this.minecraftGameStateTracker.updatePlayerRotation(
                    newRotation,
                    clientPlayerRotationPacket.isOnGround()
            );
        } else if (clientPacket instanceof ServerboundMovePlayerPosRotPacket) {
            ServerboundMovePlayerPosRotPacket clientPlayerPositionRotationPacket =
                    (ServerboundMovePlayerPosRotPacket) clientPacket;

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
        if (serverPacket instanceof ClientboundPlayerPositionPacket) {
            ClientboundPlayerPositionPacket serverPlayerPositionRotationPacket =
                    (ClientboundPlayerPositionPacket) serverPacket;

            Position newPosition = new Position(
                    serverPlayerPositionRotationPacket.getPosition().getX(),
                    serverPlayerPositionRotationPacket.getPosition().getY(),
                    serverPlayerPositionRotationPacket.getPosition().getZ()
            );

            Rotation newRotation = new Rotation(
                    serverPlayerPositionRotationPacket.getXRot(),
                    serverPlayerPositionRotationPacket.getYRot(),
                    0.0 // Player's can't roll (i.e. tilt entire body to left or right)
            );

            this.minecraftGameStateTracker.updatePlayerPositionRotation(newPosition, newRotation, null);
        } else if (serverPacket instanceof ClientboundLoginPacket) {
            ClientboundLoginPacket serverJoinGamePacket = (ClientboundLoginPacket) serverPacket;

            this.minecraftGameStateTracker.updatePlayerEntityId(serverJoinGamePacket.getEntityId());

            // NOTE: world name has "minecraft:" prefix.  asMinimalString() removes "minecraft:" when that's the prefix.
            String worldName = serverJoinGamePacket.getCommonPlayerSpawnInfo().getWorldName().asMinimalString();

            // Update the world info based on the dimension/world just spawned into (e.g. min_y and height)
            this.updateOnEnterDimension(worldName);
        } else if (serverPacket instanceof ClientboundRespawnPacket) {
            ClientboundRespawnPacket respawnPacket = (ClientboundRespawnPacket) serverPacket;

            this.log.debug("RESPAWN to world {}", respawnPacket.getCommonPlayerSpawnInfo().getWorldName());

            // Clear all the existing chunks
            this.minecraftGameStateTracker.clearChunks();

            // NOTE: world name has "minecraft:" prefix.  asMinimalString() removes "minecraft:" when that's the prefix.
            String worldName = respawnPacket.getCommonPlayerSpawnInfo().getWorldName().asMinimalString();

            // Update the world info based on the dimension/world just spawned into (e.g. min_y and height)
            this.updateOnEnterDimension(worldName);
        } else if (serverPacket instanceof ClientboundSetHealthPacket) {
            ClientboundSetHealthPacket serverPlayerHealthPacket = (ClientboundSetHealthPacket) serverPacket;


            this.minecraftGameStateTracker.updatePlayerHealth(
                serverPlayerHealthPacket.getHealth(),
                serverPlayerHealthPacket.getSaturation(),
                serverPlayerHealthPacket.getFood()
            );
        } else if (serverPacket instanceof ClientboundLevelChunkWithLightPacket) {
            ClientboundLevelChunkWithLightPacket chunkWithLightPacket = (ClientboundLevelChunkWithLightPacket) serverPacket;

            List<ChunkSection> chunkSections = this.parseChunks(chunkWithLightPacket);
            this.minecraftGameStateTracker.chunkLoaded((ClientboundLevelChunkWithLightPacket) serverPacket, chunkSections);
        } else if (serverPacket instanceof ClientboundForgetLevelChunkPacket) {
            ClientboundForgetLevelChunkPacket serverUnloadChunkPacket = (ClientboundForgetLevelChunkPacket) serverPacket;

            this.minecraftGameStateTracker.chunkUnloaded(serverUnloadChunkPacket.getX(),serverUnloadChunkPacket.getZ());
        } else if (serverPacket instanceof ClientboundBlockUpdatePacket) {
            ClientboundBlockUpdatePacket serverBlockChangePacket = (ClientboundBlockUpdatePacket) serverPacket;

            // X, Y, and Z getters return absolute world position
            Position blockPosition = new Position(
                    serverBlockChangePacket.getEntry().getPosition().getX(),
                    serverBlockChangePacket.getEntry().getPosition().getY(),
                    serverBlockChangePacket.getEntry().getPosition().getZ()
            );

            int blockId = serverBlockChangePacket.getEntry().getBlock();

            this.minecraftGameStateTracker.blockChanged(blockPosition, blockId);
        } else if (serverPacket instanceof ClientboundSectionBlocksUpdatePacket) {
            ClientboundSectionBlocksUpdatePacket serverMultiBlockChangePacket = (ClientboundSectionBlocksUpdatePacket) serverPacket;

            int chunkX = serverMultiBlockChangePacket.getChunkX();
            int chunkZ = serverMultiBlockChangePacket.getChunkZ();

            // Note we ignore the packet's chunkY because the tracker doesn't keep "chunk sections" it just keeps
            //  full-height chunks

            this.minecraftGameStateTracker.multipleBlocksChanged(chunkX, chunkZ, serverMultiBlockChangePacket.getEntries());
        } else if (serverPacket instanceof ClientboundRegistryDataPacket) {
            ClientboundRegistryDataPacket clientboundRegistryDataPacket = (ClientboundRegistryDataPacket) serverPacket;
            if (Objects.equals(clientboundRegistryDataPacket.getRegistry().asString(), DIMENSION_TYPE_CODEC_TAG)) {
                // Dimension Type registry
                for (RegistryEntry oneDimension : clientboundRegistryDataPacket.getEntries()) {
                    this.updateGameDimensionType(oneDimension);
                }
            }
        }
    }

    /**
     * Given a registry definition for a dimension, extract the override details for the dimension and record them for
     *  use when the player enters the dimension.
     *
     * NOTE: as of this writing, the precise format and content of the nbtMap here is not confirmed.  The sad truth of
     *  generic data typing...
     *
     * @param dimensionDetails
     */
    private void updateGameDimensionType(RegistryEntry dimensionDetails) {
        String dimensionSimpleName = dimensionDetails.getId().asMinimalString();
        NbtMap nbtMap = dimensionDetails.getData();

        Integer minY = null;
        Integer height = null;

        if (nbtMap != null) {
            if (nbtMap.containsKey(MIN_Y_CODEC_TAG)) {
                minY = nbtMap.getInt(MIN_Y_CODEC_TAG);
            }

            if (nbtMap.containsKey(HEIGHT_CODEC_TAG)) {
                height = nbtMap.getInt(HEIGHT_CODEC_TAG);
            }
        }

        // Use defaults if either of the required values are missing.
        if ((minY == null) || (height == null)) {
            try {
                // Try loading the defaults just to confirm we have the dimension's data a little sooner than later.
                this.minecraftDatagenManager.readDimensionType(dimensionSimpleName);
            } catch (IOException ioExc) {
                throw new RuntimeException("Updating dimension type data, but missing " + MIN_Y_CODEC_TAG + " and/or " + HEIGHT_CODEC_TAG + " values and defaults failed to load", ioExc);
            }
        }

        // If a minY value was found, use it as the override for this dimension.
        if (minY != null) {
            this.registryOverrideMinYByDimension.put(dimensionSimpleName, minY);
        }

        // If a height value was found, use it as the override for this dimension.
        if (height != null) {
            this.registryOverrideHeightByDimension.put(dimensionSimpleName, height);
        }
    }

    /**
     * Update the world state, specifically data needed for chunk parsing, on player entering a dimension.
     *
     * @param worldName
     */
    private void updateOnEnterDimension(String worldName) {
        try {
            DatagenDimensionType dimensionType = this.minecraftDatagenManager.readDimensionType(worldName);

            // Apply the override value, if one is defined.
            if (this.registryOverrideMinYByDimension.containsKey(worldName)) {
                this.worldMinY = this.registryOverrideMinYByDimension.get(worldName);
            } else {
                // Fallback to the dimension defaults.
                this.worldMinY = dimensionType.getMinY();
            }

            // Apply the override value, if one is defined.
            if (this.registryOverrideHeightByDimension.containsKey(worldName)) {
                this.worldHeight = this.registryOverrideHeightByDimension.get(worldName);
            } else {
                // Fallback to the dimension defaults.
                this.worldHeight = dimensionType.getHeight();
            }

            this.log.debug("have world dimensions: min-y={}; height={}", this.worldMinY, this.worldHeight);

            // Update the world game state for Min Y as chunk Y coordinates are relative to this value.
            this.minecraftGameStateTracker.updatedMinY(this.worldMinY);
        } catch (IOException ioExc) {
            this.log.error("Cannot load dimension type datagen; player will NOT be able to use chunk/block info (e.g. BLOCK AT) statement", ioExc);
        }
    }

    @Override
    public void onPacketSentToClient(Packet clientBoundPacket) {
    }

    @Override
    public void onPacketSentToServer(Packet serverBoundPacket) {
    }

//========================================
// Internals
//========================================


    @SuppressWarnings("unchecked")
    private List<ChunkSection> parseChunks(ClientboundLevelChunkWithLightPacket packet) {
        if (! this.checkWorldHeightKnown()) {
            return Collections.EMPTY_LIST;
        }

        byte[] rawChunkData = packet.getChunkData();

        try {
            List<ChunkSection> result = new LinkedList<>();

            ByteBuf byteBuf = Unpooled.copiedBuffer(rawChunkData);

            int numSectionToRead = ( this.worldHeight / 16 ) + 1;

            // Read until we have all of the sections for the chunk, or we run out of data from the server.
            int cur = 0;
            while ((cur < numSectionToRead) && (byteBuf.isReadable())) {
                ChunkSection oneChunkSection = MinecraftTypes.readChunkSection(byteBuf);

                result.add(oneChunkSection);
                cur++;
            }

            return result;
        } catch (Exception ioExc) {
            this.log.error("ERROR parsing chunk section data", ioExc);
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Check that the world height is known, and if not, log a warning.  This method rate-limits the warning so we don't
     *  spam the log repeatedly when this happens.
     *
     * @return
     */
    private boolean checkWorldHeightKnown() {
        if (this.worldHeight == Integer.MIN_VALUE) {
            long lastUpdatedSnapshot = lastWarnWorldHeightUnknown.get();
            long nowTimestamp = System.nanoTime();
            long deltaNanos = nowTimestamp - lastUpdatedSnapshot;
            long deltaSeconds = Duration.ofNanos(deltaNanos).getSeconds();

            // If elapsed > limit, alert
            if (deltaSeconds > WARN_WORLD_HEIGHT_UNKNOWN_PERIOD_LENGTH) {
                // Make sure we didn't lose the race with another thread
                if (lastWarnWorldHeightUnknown.compareAndSet(lastUpdatedSnapshot, nowTimestamp)) {
                    this.log.error("ATTEMPT to parse CHUNK data before world height known (message rate limit = {}s)", WARN_WORLD_HEIGHT_UNKNOWN_PERIOD_LENGTH);
                }
            }

            return false;
        }

        return true;
    }
}
