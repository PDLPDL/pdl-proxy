package com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model;

import com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.MinecraftGameStateMutationUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MinecraftGameStateTest {

    private MinecraftGameState minecraftGameState;

    private MinecraftGameStateMutationUtils mutationUtils;

    @Before
    public void setUp() throws Exception {
        this.minecraftGameState = new MinecraftGameState(
                13,
                new Position(1.0, 2.0, 3.0),
                true,
                new Rotation(180.0, -90.0, 0.0)
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
}