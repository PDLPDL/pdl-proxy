package com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model;

import java.util.Objects;

/**
 * Position of a block within its Chunk.
 */
public class BlockChunkBasedPosition {
    /**
     * X position of the block within the chunk (add to the Chunk's world position to find the absolute world
     * coordinates).
     *
     * Since Chunks are only 16 blocks wide, this value is only valid between 0..15, inclusive.
     */
    private final short x;

    /**
     * Y position of the block. Since Chunks are the full height of the world, the absolute Y position of the block
     * is the same.
     */
    private final short y;

    /**
     * Z position of the block within the chunk (add to the Chunk's world position to find the absolute world
     * coordinates).
     *
     * Since Chunks are only 16 blocks long, this value is only valid between 0..15, inclusive.
     */
    private final short z;

//========================================
// Constructor
//========================================

    public BlockChunkBasedPosition(short x, short y, short z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public short getX() {
        return x;
    }

    public short getY() {
        return y;
    }

    public short getZ() {
        return z;
    }

//========================================
// Equals + Hash
//========================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockChunkBasedPosition that = (BlockChunkBasedPosition) o;
        return x == that.x &&
                y == that.y &&
                z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
