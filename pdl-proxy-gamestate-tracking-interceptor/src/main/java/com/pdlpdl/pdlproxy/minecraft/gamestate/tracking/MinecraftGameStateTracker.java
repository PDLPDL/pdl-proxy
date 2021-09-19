package com.pdlpdl.pdlproxy.minecraft.gamestate.tracking;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model.*;

import java.util.function.Supplier;

/**
 * NOTE: every player session currently gets its own game state.
 *
 * TODO: create a unified game state for all players.
 */
public class MinecraftGameStateTracker {
    private MinecraftGameState minecraftGameState = MinecraftGameState.INITIAL_GAME_STATE;

    private MinecraftGameStateMutationUtils minecraftGameStateMutationUtils = new MinecraftGameStateMutationUtils();

//========================================
// Getters and Setters
//----------------------------------------

    public MinecraftGameStateMutationUtils getMinecraftGameStateMutationUtils() {
        return minecraftGameStateMutationUtils;
    }

    public void setMinecraftGameStateMutationUtils(MinecraftGameStateMutationUtils minecraftGameStateMutationUtils) {
        this.minecraftGameStateMutationUtils = minecraftGameStateMutationUtils;
    }

//========================================
// Read and Update Game State
//
// NOTE: these operations are guaranteed atomic
//----------------------------------------


    public MinecraftGameState getCurrentMinecraftGameState() {
        return this.minecraftGameState;
    }

    public void updateMinecraftGameState(MinecraftGameState updated) {
        this.minecraftGameState = updated;
    }

//========================================
// Game State Mutations
//----------------------------------------

    public MinecraftGameState updatePlayerEntityId(int newEntityId) {
        return this.updateCommon(
                () -> this.minecraftGameStateMutationUtils.updatePlayerEntityId(this.minecraftGameState, newEntityId));
    }

    public MinecraftGameState updatePlayerPosition(Position updatedPosition, Boolean updatedIsOnGround) {
        return this.updateCommon(
                () -> this.minecraftGameStateMutationUtils
                        .updatePlayerPosition(this.minecraftGameState, updatedPosition, updatedIsOnGround));
    }

    public MinecraftGameState updatePlayerRotation(Rotation updatedRotation, Boolean updatedIsOnGround) {
        return this.updateCommon(
                () -> this.minecraftGameStateMutationUtils
                        .updatePlayerRotation(this.minecraftGameState, updatedRotation, updatedIsOnGround));
    }

    public MinecraftGameState updatePlayerPositionRotation(Position updatedPosition, Rotation updatedRotation, Boolean updatedIsOnGround) {
        return this.updateCommon(
                () -> this.minecraftGameStateMutationUtils
                        .updatePlayerPositionRotation(this.minecraftGameState, updatedPosition, updatedRotation, updatedIsOnGround));
    }

    public MinecraftGameState updatePlayerHealth(float health, float saturation, int food) {
        return this.updateCommon(
                () -> this.minecraftGameStateMutationUtils
                        .updatePlayerHealth(this.minecraftGameState, health, saturation, food));
    }

    public MinecraftGameState chunkLoaded(ServerChunkDataPacket chunkDataPacket) {
        Column column = chunkDataPacket.getColumn();

        ChunkPosition chunkPosition = new ChunkPosition(column.getX() * 16, column.getZ() * 16);
        Chunk[] chunkSections = chunkDataPacket.getColumn().getChunks();

        ImmutableChunkSectionFacade[] immutableChunkSections = this.convertChunksArrayToImmutables(chunkSections);

        return  this.updateCommon(
                () -> this.minecraftGameStateMutationUtils
                        .updateChunk(this.minecraftGameState, chunkPosition, immutableChunkSections)
        );
    }

    public MinecraftGameState chunkUnloaded(int x, int z) {
        ChunkPosition chunkPosition = new ChunkPosition(x, z);

        return  this.updateCommon(
                () -> this.minecraftGameStateMutationUtils
                        .unloadChunk(this.minecraftGameState, chunkPosition)
        );
    }

    public MinecraftGameState blockChanged(Position blockPosition, int blockId) {
        return  this.updateCommon(
                () -> this.minecraftGameStateMutationUtils
                        .updateBlock(this.minecraftGameState, blockPosition, blockId)
        );
    }

    public MinecraftGameState multipleBlocksChanged(int chunkX, int chunkZ, BlockChangeRecord[] blockChangeRecords) {
        return  this.updateCommon(
                () -> this.minecraftGameStateMutationUtils
                        .updateMultipleBlocks(this.minecraftGameState, chunkX, chunkZ, blockChangeRecords)
        );
    }

//========================================
// Internals
//----------------------------------------

    private MinecraftGameState updateCommon(Supplier<MinecraftGameState> updateOperation) {
        MinecraftGameState result = updateOperation.get();
        this.updateMinecraftGameState(result);
        return result;
    }

    private ImmutableChunkSectionFacade[] convertChunksArrayToImmutables(Chunk[] arr) {
        ImmutableChunkSectionFacade[] result = new ImmutableChunkSectionFacade[arr.length];

        int cur = 0;
        while (cur < arr.length) {
            if (arr[cur] != null) {
                result[cur] = new ImmutableChunkSectionFacade(arr[cur]);
            } else {
                result[cur] = null;
            }

            cur++;
        }

        return result;
    }

//========================================
//
//========================================

}
