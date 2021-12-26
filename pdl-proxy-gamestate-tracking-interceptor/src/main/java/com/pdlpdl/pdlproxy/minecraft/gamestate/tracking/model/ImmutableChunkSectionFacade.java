package com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model;

import com.github.steveice10.mc.protocol.data.game.chunk.DataPalette;


/**
 * Immutable wrapper for a Chunk Section (16x16x16)
 */
public class ImmutableChunkSectionFacade {
    private final DataPalette delegate;
    private final DataPalette biomeData;

    public ImmutableChunkSectionFacade(DataPalette delegate, DataPalette biomeData) {
        this.delegate = delegate;
        this.biomeData = biomeData;
    }

    public int get(int x, int y, int z) {
        return delegate.get(x, y, z);
    }
}
