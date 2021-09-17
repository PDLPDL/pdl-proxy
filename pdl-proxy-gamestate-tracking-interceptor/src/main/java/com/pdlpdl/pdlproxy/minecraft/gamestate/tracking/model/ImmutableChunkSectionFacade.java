package com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;


/**
 * Immutable wrapper for a Chunk Section (16x16x16)
 */
public class ImmutableChunkSectionFacade {
    private final Chunk delegate;

    public ImmutableChunkSectionFacade(Chunk delegate) {
        this.delegate = delegate;
    }

    public int get(int x, int y, int z) {
        return delegate.get(x, y, z);
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public int getBlockCount() {
        return delegate.getBlockCount();
    }
}
