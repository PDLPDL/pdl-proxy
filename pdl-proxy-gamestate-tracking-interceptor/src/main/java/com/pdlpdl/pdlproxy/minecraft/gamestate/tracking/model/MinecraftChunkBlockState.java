package com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable block state within a single world chunk.  Uses the original Chunk data from the server together with the
 * set of changes to that state.
 */
public class MinecraftChunkBlockState {
    /**
     * X, Z coordinates of the Chunk's position in the world.
     */
    private final ChunkPosition chunkWorldPosition;

    /**
     * The original chunk state as received from the Minecraft Server
     */
    private final ImmutableChunkSectionFacade[] immutableChunkSections;

    /**
     * Block states updated after the chunk was loaded.
     *
     * TODO: more performant data structure?
     */
    private final Map<BlockChunkBasedPosition, Integer> updatedBlocks;

//========================================
// Constructor
//========================================

    public MinecraftChunkBlockState(
            ChunkPosition chunkWorldPosition,
            ImmutableChunkSectionFacade[] immutableChunkSections,
            Map<BlockChunkBasedPosition, Integer> updatedBlocks) {

        this.chunkWorldPosition = chunkWorldPosition;
        this.immutableChunkSections = immutableChunkSections;

        if (updatedBlocks == null) {
            updatedBlocks = new HashMap<>();
        }

        this.updatedBlocks = Collections.unmodifiableMap(new HashMap<>(updatedBlocks));
    }

//========================================
// Getters
//========================================

    public ChunkPosition getChunkWorldPosition() {
        return chunkWorldPosition;
    }

    public ImmutableChunkSectionFacade[] getImmutableChunkFacade() {
        return immutableChunkSections;
    }

    public Map<BlockChunkBasedPosition, Integer> getUpdatedBlocks() {
        return updatedBlocks;
    }

    /**
     * Get the block at the given chunk-relative position.  Note that chunks are stored in 16-block-high sections, so
     * calculations are required on the Y coordinate to determine the section, and Y offset within the section.
     *
     * @param x
     * @param y
     * @param z
     * @return
     */
    public int getChunkBlock(short x, short y, short z) {
        Integer result = this.updatedBlocks.get(new BlockChunkBasedPosition(x, y, z));

        if (result == null) {
            int sectionNum = y / 16;

            if ((sectionNum >= this.immutableChunkSections.length) || (this.immutableChunkSections[sectionNum] == null)) {
                return MinecraftWorldBlockState.AIR;
            }

            short sectionY = (short) (y % 16);

            return this.immutableChunkSections[sectionNum].get(x, sectionY, z);
        }

        return result;
    }
}
