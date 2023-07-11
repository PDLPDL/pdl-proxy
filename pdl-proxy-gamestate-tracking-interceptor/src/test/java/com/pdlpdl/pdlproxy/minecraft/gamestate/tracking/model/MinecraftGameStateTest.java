package com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model;

import com.github.steveice10.mc.protocol.data.game.chunk.DataPalette;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockChangeEntry;
import com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.MinecraftGameStateMutationUtils;
import org.cloudburstmc.math.vector.Vector3i;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

public class MinecraftGameStateTest {

    private MinecraftGameState minecraftGameState;

    private MinecraftGameStateMutationUtils mutationUtils;

    @Before
    public void setUp() throws Exception {
        this.minecraftGameState = new MinecraftGameState(
                "cool-gamer",
                "64f7a3b4-445c-47bf-a54c-d44a24d69efd",
                13,
                new Position(1.0, 2.0, 3.0),
                true,
                new Rotation(180.0, -90.0, 0.0),
                20.0f,
                0f,
                20,
                null
        );

        this.mutationUtils = new MinecraftGameStateMutationUtils();
    }

    /**
     * Just perform a mutation.  If it throws an exception, we know our mutation config is broken.
     */
    @Test
    public void testMutatorConfig() {
        MinecraftGameState updated = this.mutationUtils.updatePlayerEntityId(this.minecraftGameState, 23);
    }

    @Test
    public void testMutateBlockId() {
        MinecraftGameState updated = this.mutationUtils.updateBlock(this.minecraftGameState, new Position(100.0, 60.0, 100.0), 22);

        assertEquals(22, updated.getMinecraftWorldBlockState().getBlock(100, 60, 100));
    }

    @Test
    public void testMutateBlockIdUnchanged() {
        MinecraftGameState updated = this.mutationUtils.updateBlock(this.minecraftGameState, new Position(100.0, 60.0, 100.0), 22);
        MinecraftGameState updated2 = this.mutationUtils.updateBlock(updated, new Position(100.0, 60.0, 100.0), 22);

        assertEquals(22, updated2.getMinecraftWorldBlockState().getBlock(100, 60, 100));
        assertSame(updated.getMinecraftWorldBlockState(), updated2.getMinecraftWorldBlockState());
    }

    @Test
    public void testMutateMultipleBlocks() {
        MinecraftGameState updated = this.mutationUtils.updateMultipleBlocks(
                this.minecraftGameState, 96, 96, new BlockChangeEntry[]{
                        new BlockChangeEntry(Vector3i.from(100, 60, 100), 23),
                        new BlockChangeEntry(Vector3i.from(101, 60, 100), 24),
                        new BlockChangeEntry(Vector3i.from(102, 60, 100), 25)
                });

        assertEquals(23, updated.getMinecraftWorldBlockState().getBlock(100, 60, 100));
        assertEquals(24, updated.getMinecraftWorldBlockState().getBlock(101, 60, 100));
        assertEquals(25, updated.getMinecraftWorldBlockState().getBlock(102, 60, 100));

        MinecraftGameState updated2 = this.mutationUtils.updateMultipleBlocks(
                updated, 96, 96, new BlockChangeEntry[]{
                        new BlockChangeEntry(Vector3i.from(100, 60, 100), 123),
                        new BlockChangeEntry(Vector3i.from(103, 60, 100), 26),
                        new BlockChangeEntry(Vector3i.from(104, 60, 100), 27)
                });

        assertEquals(123, updated2.getMinecraftWorldBlockState().getBlock(100, 60, 100));
        assertEquals(24, updated2.getMinecraftWorldBlockState().getBlock(101, 60, 100));
        assertEquals(25, updated2.getMinecraftWorldBlockState().getBlock(102, 60, 100));
        assertEquals(26, updated2.getMinecraftWorldBlockState().getBlock(103, 60, 100));
        assertEquals(27, updated2.getMinecraftWorldBlockState().getBlock(104, 60, 100));
    }

    @Test
    public void testMutateChunkData() {
        //
        // Set test data and interactions
        //
        DataPalette mockChunkDataPalette = Mockito.mock(DataPalette.class);
        Mockito.when(mockChunkDataPalette.get(0, 0, 0)).thenReturn(100);
        Mockito.when(mockChunkDataPalette.get(1, 1, 1)).thenReturn(111);

        ChunkPosition chunkPosition = new ChunkPosition(96, 96);

        ImmutableChunkSectionFacade[] sections = new ImmutableChunkSectionFacade[16];
        sections[0] = new ImmutableChunkSectionFacade(mockChunkDataPalette, null);


        //
        // Execute
        //
        MinecraftGameState updated = this.mutationUtils.updateChunk(this.minecraftGameState, chunkPosition, sections);


        //
        // Validate
        //
        assertEquals(100, updated.getMinecraftWorldBlockState().getBlock(96, 0, 96));
        assertEquals(111, updated.getMinecraftWorldBlockState().getBlock(97, 1, 97));

        assertEquals(0, updated.getMinecraftWorldBlockState().getBlock(0, 0, 0));
        assertEquals(0, updated.getMinecraftWorldBlockState().getBlock(-100, 0, -100));


        //
        // Execute (2): Add some updates post-ChunkSection-load
        //
        MinecraftGameState updated2 = this.mutationUtils.updateBlock(updated, new Position(96, 0, 96), 30);
        MinecraftGameState updated3 = this.mutationUtils.updateBlock(updated2, new Position(100, 60, 100), 31);


        //
        // Validate (2)
        //
        assertEquals(30, updated3.getMinecraftWorldBlockState().getBlock(96, 0, 96));
        assertEquals(31, updated3.getMinecraftWorldBlockState().getBlock(100, 60, 100));

        assertEquals(100, updated.getMinecraftWorldBlockState().getBlock(96, 0, 96));
        assertEquals(111, updated.getMinecraftWorldBlockState().getBlock(97, 1, 97));
        assertEquals(0, updated.getMinecraftWorldBlockState().getBlock(100, 60, 100));

        assertEquals(30, updated2.getMinecraftWorldBlockState().getBlock(96, 0, 96));
        assertEquals(111, updated2.getMinecraftWorldBlockState().getBlock(97, 1, 97));
        assertEquals(0, updated3.getMinecraftWorldBlockState().getBlock(0, 0, 0));
        assertEquals(0, updated3.getMinecraftWorldBlockState().getBlock(-100, 0, -100));

    }

    @Test
    public void testMutateUnloadChunkData() {
        //
        // Set test data and interactions
        //
        DataPalette mockChunkDataPalette = Mockito.mock(DataPalette.class);
        Mockito.when(mockChunkDataPalette.get(0, 0, 0)).thenReturn(100);
        Mockito.when(mockChunkDataPalette.get(1, 1, 1)).thenReturn(111);

        ChunkPosition chunkPosition = new ChunkPosition(96, 96);

        ImmutableChunkSectionFacade[] sections = new ImmutableChunkSectionFacade[16];
        sections[0] = new ImmutableChunkSectionFacade(mockChunkDataPalette, null);


        //
        // Execute
        //
        MinecraftGameState updated = this.mutationUtils.updateChunk(this.minecraftGameState, chunkPosition, sections);
        MinecraftGameState unloaded = this.mutationUtils.unloadChunk(updated, chunkPosition);


        //
        // Validate
        //
        assertEquals(100, updated.getMinecraftWorldBlockState().getBlock(96, 0, 96));
        assertEquals(111, updated.getMinecraftWorldBlockState().getBlock(97, 1, 97));

        assertEquals(0, unloaded.getMinecraftWorldBlockState().getBlock(96, 0, 96));
        assertEquals(0, unloaded.getMinecraftWorldBlockState().getBlock(97, 1, 97));

        assertEquals(0, unloaded.getMinecraftWorldBlockState().getBlock(0, 0, 0));
        assertEquals(0, unloaded.getMinecraftWorldBlockState().getBlock(-100, 0, -100));
    }

    @Test
    public void testMutateUnloadChunkNotLoadedData() {
        ChunkPosition chunkPosition = new ChunkPosition(96, 96);

        //
        // Execute
        //
        MinecraftGameState unloaded = this.mutationUtils.unloadChunk(this.minecraftGameState, chunkPosition);


        //
        // Validate
        //
        assertSame(unloaded.getMinecraftWorldBlockState(), this.minecraftGameState.getMinecraftWorldBlockState());
    }
}