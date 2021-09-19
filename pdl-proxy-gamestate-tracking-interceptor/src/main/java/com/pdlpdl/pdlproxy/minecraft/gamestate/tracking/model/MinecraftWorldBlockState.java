package com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model;

import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Track the state of blocks in the world by coordinates.  Maintains the set of all known blocks in the world that are
 * currently loaded into memory.
 *
 * Immutable.
 *
 * TODO:
 * Q. When to unload chunks?  Is the server unload-chunk packet adequate?
 */
public class MinecraftWorldBlockState {
    public static final int AIR = 0;

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MinecraftWorldBlockState.class);

    private Logger log = DEFAULT_LOGGER;


    private final Map<ChunkPosition, MinecraftChunkBlockState> chunks;

//========================================
// Constructor
//========================================

    public MinecraftWorldBlockState(Map<ChunkPosition, MinecraftChunkBlockState> chunks) {
        if (chunks == null) {
            chunks = new HashMap<>();
        }

        this.chunks = Collections.unmodifiableMap(new HashMap<>(chunks));
    }

//========================================
// Getters
//========================================

    public Map<ChunkPosition, MinecraftChunkBlockState> getChunks() {
        return chunks;
    }

    /**
     * Retrieve the block at the given world coordinates.
     *
     * @param x
     * @param y
     * @param z
     *
     * @return
     */
    public int getBlock(int x, int y, int z) {
        // Determine the chunk position for the world coordinates
        ChunkPosition chunkPosition = this.getChunkPositionForWorldPosition(x, z);

        MinecraftChunkBlockState minecraftChunkBlockState = this.chunks.get(chunkPosition);
        if (minecraftChunkBlockState != null) {
            short blockRelX = (short) (x - chunkPosition.getX());
            short blockRelZ = (short) (z - chunkPosition.getZ());

            return minecraftChunkBlockState.getChunkBlock(blockRelX, (short) y, blockRelZ);
        } else {
            return AIR;
        }
    }

//===========================================
// Mutation Methods (returning "full" copies)
//===========================================

    public MinecraftWorldBlockState placeChunk(ChunkPosition chunkPosition, MinecraftChunkBlockState minecraftChunkBlockState) {
        Map<ChunkPosition, MinecraftChunkBlockState> updatedMap = new HashMap<>(this.chunks);
        updatedMap.put(chunkPosition, minecraftChunkBlockState);

        return new MinecraftWorldBlockState(updatedMap);
    }

    public MinecraftWorldBlockState unloadChunk(ChunkPosition chunkPosition) {
        // Only do the work if we have the chunk in memory
        if (this.chunks.containsKey(chunkPosition)) {
            Map<ChunkPosition, MinecraftChunkBlockState> updatedMap = new HashMap<>(this.chunks);
            updatedMap.remove(chunkPosition);

            return new MinecraftWorldBlockState(updatedMap);
        }

        // Don't have the chunk in memory, so this is a no-op
        return this;
    }

    public MinecraftWorldBlockState updateBlock(Position blockWorldPosition, int blockId) {
        Map<BlockChunkBasedPosition, Integer> updatedBlocks = new HashMap<>();

        // Map the block to a chunk
        ChunkPosition chunkPosition =
                this.getChunkPositionForWorldPosition((int) Math.floor(blockWorldPosition.getX()), (int) Math.floor(blockWorldPosition.getZ()));

        // Lookup the chunk
        MinecraftChunkBlockState minecraftChunkBlockState = this.chunks.get(chunkPosition);

        // Calculate the block's position relative to the chunk
        BlockChunkBasedPosition relativeBlockPos = this.calculateBlockRelativePositionInChunk(blockWorldPosition, chunkPosition);

        // Remember existing chunks, if they exist
        ImmutableChunkSectionFacade[] immutableChunkSectionFacades;

        //
        // If none was found, warn - we should not be getting updates for chunks that are not loaded.  Otherwise, copy
        //  the current state of the chunk.
        //
        if (minecraftChunkBlockState == null) {
            this.log.warn("Have unexpected updated block position for a chunk that is not loaded; x={}; y={}; z={}; block-id={}",
                    blockWorldPosition.getX(), blockWorldPosition.getY(), blockWorldPosition.getZ(), blockId);

            // No chunks
            immutableChunkSectionFacades = new ImmutableChunkSectionFacade[0];
        } else {
            // Keep the existing chunks
            immutableChunkSectionFacades = minecraftChunkBlockState.getImmutableChunkFacade();

            // Check if the target block is actually the same as specified, in which case just return the current object
            if (minecraftChunkBlockState.getChunkBlock(relativeBlockPos.getX(), relativeBlockPos.getY(), relativeBlockPos.getZ()) == blockId) {
                return this;
            }

            // Copy all the updates already existing for the chunk
            updatedBlocks.putAll(minecraftChunkBlockState.getUpdatedBlocks());
        }

        //
        // Update the target block
        //
        updatedBlocks.put(relativeBlockPos, blockId);
        minecraftChunkBlockState = new MinecraftChunkBlockState(chunkPosition, immutableChunkSectionFacades, updatedBlocks);

        //
        // Create a new chunk map.  Copy all of the chunks from the current state, then add in the updated one.
        //
        Map<ChunkPosition, MinecraftChunkBlockState> updatedChunks = new HashMap<>(this.chunks);
        updatedChunks.put(chunkPosition, minecraftChunkBlockState);

        return new MinecraftWorldBlockState(updatedChunks);
    }


    public MinecraftWorldBlockState updateMultipleBlocks(int chunkX, int chunkZ, BlockChangeRecord[] changeRecords) {
        Map<BlockChunkBasedPosition, Integer> updatedBlocks = new HashMap<>();

        ChunkPosition chunkPosition = new ChunkPosition(chunkX, chunkZ);

        // Lookup the chunk
        MinecraftChunkBlockState minecraftChunkBlockState = this.chunks.get(chunkPosition);

        // Remember existing chunks, if they exist
        ImmutableChunkSectionFacade[] immutableChunkSectionFacades;

        //
        // If none was found, warn - we should not be getting updates for chunks that are not loaded.  Otherwise, copy
        //  the current state of the chunk.
        //
        if (minecraftChunkBlockState == null) {
            this.log.warn("Have unexpected updated multiple-block update for a chunk that is not loaded; x={}; z={}",
                    chunkX, chunkZ);

            // No chunks
            immutableChunkSectionFacades = new ImmutableChunkSectionFacade[0];
        } else {
            // Keep the existing chunks
            immutableChunkSectionFacades = minecraftChunkBlockState.getImmutableChunkFacade();

            // Copy all the updates already existing for the chunk
            updatedBlocks.putAll(minecraftChunkBlockState.getUpdatedBlocks());
        }

        //
        // Update the chunk with the list of target blocks
        //
        for (BlockChangeRecord oneBlockChange : changeRecords) {
            // Calculate the block's position relative to the chunk
            BlockChunkBasedPosition relativeBlockPos =
                    this.calculateBlockRelativePositionInChunk(oneBlockChange.getPosition(), chunkPosition);

            updatedBlocks.put(relativeBlockPos, oneBlockChange.getBlock());
        }

        minecraftChunkBlockState =
                new MinecraftChunkBlockState(chunkPosition, immutableChunkSectionFacades, updatedBlocks);

        //
        // Create a new chunk map.  Copy all of the chunks from the current state, then add in the updated one.
        //
        Map<ChunkPosition, MinecraftChunkBlockState> updatedChunks = new HashMap<>(this.chunks);
        updatedChunks.put(chunkPosition, minecraftChunkBlockState);

        return new MinecraftWorldBlockState(updatedChunks);
    }

//========================================
//
//========================================

    private ChunkPosition getChunkPositionForWorldPosition(int x, int z) {
        int chunkPosX;
        int chunkPosZ;

        if (x >= 0) {
            chunkPosX = x - (x % 16);
        } else {
            chunkPosX = x - 16 + (-x % 16);
        }

        if (z >= 0) {
            chunkPosZ = z - (z % 16);
        } else {
            chunkPosZ = z - 16 + (-z % 16);
        }

        return new ChunkPosition(chunkPosX, chunkPosZ);
    }

    private BlockChunkBasedPosition calculateBlockRelativePositionInChunk(Position blockWorldPosition, ChunkPosition chunkPosition) {
        short relX = (short) (Math.floor(blockWorldPosition.getX()) - chunkPosition.getX());
        short relY = (short) Math.floor(blockWorldPosition.getY());
        short relZ = (short) (Math.floor(blockWorldPosition.getZ()) - chunkPosition.getZ());

        BlockChunkBasedPosition result = new BlockChunkBasedPosition(relX, relY, relZ);

        return result;
    }

    private BlockChunkBasedPosition calculateBlockRelativePositionInChunk(
            com.github.steveice10.mc.protocol.data.game.entity.metadata.Position  blockWorldPosition, ChunkPosition chunkPosition) {

        short relX = (short) (Math.floor(blockWorldPosition.getX()) - chunkPosition.getX());
        short relY = (short) Math.floor(blockWorldPosition.getY());
        short relZ = (short) (Math.floor(blockWorldPosition.getZ()) - chunkPosition.getZ());

        BlockChunkBasedPosition result = new BlockChunkBasedPosition(relX, relY, relZ);

        return result;
    }
}
