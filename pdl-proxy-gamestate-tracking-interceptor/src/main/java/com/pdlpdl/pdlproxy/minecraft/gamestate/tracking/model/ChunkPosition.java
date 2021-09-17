package com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model;

import java.util.Objects;

/**
 * minimum (X, Z) coordinates for a Chunk - e.g. (0, 0), (0, 16), (-16, 0)
 */
public class ChunkPosition {
    private final int x;
    private final int z;

    public ChunkPosition(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkPosition that = (ChunkPosition) o;
        return x == that.x &&
                z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }
}
