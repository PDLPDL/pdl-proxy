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

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.codec.MinecraftCodec;
import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.data.game.chunk.ChunkSection;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundBlockUpdatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundForgetLevelChunkPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSectionBlocksUpdatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import com.pdlpdl.pdlproxy.minecraft.api.GameProfileAwareInterceptor;
import com.pdlpdl.pdlproxy.minecraft.api.PacketInterceptor;
import com.pdlpdl.pdlproxy.minecraft.api.ProxyPacketControl;
import com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.MinecraftGameStateTracker;
import com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model.Position;
import com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model.Rotation;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

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
    public static final String WORLDGEN_BIOME_CODEC_TAG = "minecraft:worldgen/biome";

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(GameStateTrackingPacketInterceptor.class);

    private Logger log = DEFAULT_LOGGER;


    //
    // WORLD STATE (does not change while server is running)
    //
    private int worldMinY = Integer.MIN_VALUE;
    private int worldHeight = Integer.MIN_VALUE;
    private int biomeGlobalPaletteBits = Integer.MIN_VALUE;

    private Map<String, CompoundTag> dimensionDetailsByName = new TreeMap<>();
    private Map<Integer, CompoundTag> dimensionDetailsById = new TreeMap<>();
    private Map<String, CompoundTag> worldgenBiomeDetailsByName = new TreeMap<>();
    private Map<Integer, CompoundTag> worldgenBiomeDetailsById = new TreeMap<>();

    //
    // RUNTIME STATE
    //
    private GameProfile gameProfile;
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
        } else if (serverPacket instanceof ClientboundLoginPacket) {
            ClientboundLoginPacket serverJoinGamePacket = (ClientboundLoginPacket) serverPacket;

            this.minecraftGameStateTracker.updatePlayerEntityId(serverJoinGamePacket.getEntityId());

            this.extractWorldInfo(serverJoinGamePacket);

            // Update the world game state for Min Y as chunk Y coordinates are relative to this value.
            this.minecraftGameStateTracker.updatedMinY(this.worldMinY);
        } else if (serverPacket instanceof ClientboundRespawnPacket) {
            ClientboundRespawnPacket respawnPacket = (ClientboundRespawnPacket) serverPacket;

            this.log.debug("RESPAWN to world {}", respawnPacket.getWorldName());

            // Clear all the existing chunks
            this.minecraftGameStateTracker.clearChunks();

            // Update the world info based on the dimension/world just spawned into (e.g. min_y and height)
            this.extractUpdatedWorldInfo(respawnPacket);
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

    private void extractWorldInfo(ClientboundLoginPacket loginPacket) {
        CompoundTag dimensionCodec = loginPacket.getRegistry();

        //
        // Need these values starting from 1.18 in order to properly parse + use chunk data
        //

        // Extract the details of all the dimensions
        this.extractWorldDimensionsDetails(dimensionCodec);
        this.extractWorldgenBiomeDetails(dimensionCodec);

        // Lookup the current dimension's details
        String currentDimensionName = loginPacket.getWorldName();
        CompoundTag dimensionDetails = this.dimensionDetailsByName.get(currentDimensionName);

        // Min Y and world Height
        this.worldMinY = ((IntTag) dimensionDetails.get(MIN_Y_CODEC_TAG)).getValue();
        this.worldHeight = ((IntTag) dimensionDetails.get(HEIGHT_CODEC_TAG)).getValue();

        this.biomeGlobalPaletteBits = this.calculateBiomePaletteBits();
    }

    private void extractUpdatedWorldInfo(ClientboundRespawnPacket respawnPacket) {
        String currentDimensionName = respawnPacket.getWorldName();

        CompoundTag dimensionDetails = this.dimensionDetailsByName.get(currentDimensionName);

        // Min Y and world Height
        this.worldMinY = ((IntTag) dimensionDetails.get(MIN_Y_CODEC_TAG)).getValue();
        this.worldHeight = ((IntTag) dimensionDetails.get(HEIGHT_CODEC_TAG)).getValue();

        this.biomeGlobalPaletteBits = this.calculateBiomePaletteBits();
    }

    @SuppressWarnings("unchecked")
    private void extractWorldDimensionsDetails(CompoundTag dimensionCodec) {
        //
        // Clear any old settings
        //
        this.dimensionDetailsByName.clear();
        this.dimensionDetailsById.clear();

        //
        // Navigate the complex world of Tags to get the details out...
        //
        this.log.debug("Extracting world dimension details");

        CompoundTag dimensionTypeTag = dimensionCodec.get(DIMENSION_TYPE_CODEC_TAG);

        this.extractWorldDetailsFromDimensionCodec(
                dimensionTypeTag,
                (size) -> this.log.debug("Have {} world dimensions", size),
                (dimensionName, dimensionId, dimensionInfo) -> {
                    this.log.debug("Have world dimension details: dimension-name={}, dimension-id={}", dimensionName, dimensionId);

                    this.dimensionDetailsByName.put(dimensionName, dimensionInfo);
                    this.dimensionDetailsById.put(dimensionId, dimensionInfo);
                });
    }

    // ((Map) ((CompoundTag) ((ListTag) ((Map)((CompoundTag)loginPacket.getDimensionCodec()).get("minecraft:worldgen/biome").getValue()).get("value")).get(0)).getValue()).get("element")
    @SuppressWarnings("unchecked")
    private void extractWorldgenBiomeDetails(CompoundTag dimensionCodec) {
        //
        // Clear any old settings
        //
        this.worldgenBiomeDetailsByName.clear();
        this.worldgenBiomeDetailsById.clear();


        //
        // Navigate the complex world of Tags to get the details out...
        //
        CompoundTag biomeTypeTag = dimensionCodec.get(WORLDGEN_BIOME_CODEC_TAG);

        this.extractWorldDetailsFromDimensionCodec(
                biomeTypeTag,
                (size) -> this.log.debug("Have {} worldgen biomes", size),
                (biomeName, biomeId, biomeInfo) -> {
                    this.log.debug("Have worldgen biome details: biome-name={}, biome-id={}", biomeName, biomeId);

                    this.worldgenBiomeDetailsByName.put(biomeName, biomeInfo);
                    this.worldgenBiomeDetailsById.put(biomeId, biomeInfo);
                });
    }

    private void extractWorldDetailsFromDimensionCodec(CompoundTag topTag, Consumer<Integer> listSizeConsumer, DimensionCodecElementCallback elementCallback) {
        Map<String, Tag> typeMap = topTag.getValue();
        ListTag typeTagList = (ListTag) typeMap.get("value");

        listSizeConsumer.accept(typeTagList.size());

        for (Tag oneTag : typeTagList) {
            Map<String, Tag> detailsMap = (Map) oneTag.getValue();

            String name = ((StringTag) detailsMap.get("name")).getValue();
            int id = ((IntTag) detailsMap.get("id")).getValue();
            CompoundTag details = (CompoundTag) detailsMap.get("element");

            elementCallback.accept(name, id, details);
        }
    }

    private int calculateBiomePaletteBits () {
        int numBiome = this.worldgenBiomeDetailsByName.size();

        return 32 - Integer.numberOfLeadingZeros(numBiome - 1);
    }

    @SuppressWarnings("unchecked")
    private List<ChunkSection> parseChunks(ClientboundLevelChunkWithLightPacket packet) {
        if (this.worldHeight == Integer.MIN_VALUE) {
            this.log.error("ATTEMPT to parse CHUNK data before world height known");
            return Collections.EMPTY_LIST;
        }

        byte[] rawChunkData = packet.getChunkData();

        try {
            List<ChunkSection> result = new LinkedList<>();

            ByteBuf byteBuf = Unpooled.copiedBuffer(rawChunkData);

            int numSectionToRead = ( this.worldHeight / 16 ) + 1;

            int cur = 0;
            while (cur < numSectionToRead) {
                ChunkSection oneChunkSection = MinecraftCodec.CODEC.getHelperFactory().get().readChunkSection(byteBuf, this.biomeGlobalPaletteBits);

                result.add(oneChunkSection);
                cur++;
            }

            return result;
        } catch (IOException ioExc) {
            this.log.error("ERROR parsing chunk section data", ioExc);
            return Collections.EMPTY_LIST;
        }
    }

    private static interface DimensionCodecElementCallback {
        void accept(String name, int id, CompoundTag element);
    }
}
