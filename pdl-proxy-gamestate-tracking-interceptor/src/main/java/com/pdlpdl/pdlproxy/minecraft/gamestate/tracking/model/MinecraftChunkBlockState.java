package com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Immutable block state within a single world chunk.  Uses the original Chunk data from the server together with the
 * set of changes to that state.
 */
public class MinecraftChunkBlockState {
    public static final long WARN_MIN_Y_PERIOD_LENGTH = 300L; // 5 minutes in seconds

    private static final Logger LOG = LoggerFactory.getLogger(MinecraftChunkBlockState.class);

    private static AtomicLong lastWarnMinYUnknown = new AtomicLong(0);  // Rate-limit the warning when minY == Integer.MIN_VALUE

    /**
     * X, Z coordinates of the Chunk's position in the world.
     */
    private final ChunkPosition chunkWorldPosition;

    /**
     * Min Y coordinate of the work for this chunk
     */
    private final int minY;

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
            int minY,
            ImmutableChunkSectionFacade[] immutableChunkSections,
            Map<BlockChunkBasedPosition, Integer> updatedBlocks) {

        this.chunkWorldPosition = chunkWorldPosition;
        this.immutableChunkSections = immutableChunkSections;

        if (updatedBlocks == null) {
            updatedBlocks = new HashMap<>();
        }

        this.updatedBlocks = Collections.unmodifiableMap(new HashMap<>(updatedBlocks));
        this.minY = minY;
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

    public int getMinY() {
        return minY;
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
        if (! checkWorldMinHeightKnown()) {
            // World height unknown; just return AIR
            return MinecraftWorldBlockState.AIR;
        }

        // Ignore Y values below the bottom of the world.
        if (y < this.minY ){
            return MinecraftWorldBlockState.AIR;
        }

        Integer result = this.updatedBlocks.get(new BlockChunkBasedPosition(x, y, z));

        if (result == null) {
            int sectionNum = ( y - this.minY ) / 16;

            // Ignore Y values above the top of the world
            // Sections that are a not loaded may be just AIR, so report them as such; note that this may lead to false
            //  reporting on chunk sections that are not actually loaded, but the data is sometimes sparse to save
            //  memory.
            if ((sectionNum >= this.immutableChunkSections.length) || (this.immutableChunkSections[sectionNum] == null)) {
                return MinecraftWorldBlockState.AIR;
            }

            short sectionY = (short) ((y - this.minY) % 16);

            return this.immutableChunkSections[sectionNum].get(x, sectionY, z);
        }

        return result;
    }

    private boolean checkWorldMinHeightKnown() {
        if (this.minY == Integer.MIN_VALUE) {
            long lastUpdatedSnapshot = lastWarnMinYUnknown.get();
            long nowTimestamp = System.nanoTime();
            long deltaNanos = nowTimestamp - lastUpdatedSnapshot;
            long deltaSeconds = Duration.ofNanos(deltaNanos).getSeconds();

            // If elapsed > limit, alert
            if (deltaSeconds > WARN_MIN_Y_PERIOD_LENGTH) {
                // Make sure we didn't lose the race with another thread
                if (lastWarnMinYUnknown.compareAndSet(lastUpdatedSnapshot, nowTimestamp)) {
                    LOG.error("ATTEMPT to parse CHUNK data before world floor known (message rate limit = {}s)", WARN_MIN_Y_PERIOD_LENGTH);
                }
            }

            return false;
        }

        return true;
    }

//========================================
// Internals
//----------------------------------------


}
