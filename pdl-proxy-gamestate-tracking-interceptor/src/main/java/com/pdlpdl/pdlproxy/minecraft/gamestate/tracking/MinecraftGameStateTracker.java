package com.pdlpdl.pdlproxy.minecraft.gamestate.tracking;

import com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model.MinecraftGameState;
import com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model.Position;
import com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model.Rotation;

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

//========================================
// Internals
//----------------------------------------

    private MinecraftGameState updateCommon(Supplier<MinecraftGameState> updateOperation) {
        MinecraftGameState result = updateOperation.get();
        this.updateMinecraftGameState(result);
        return result;
    }
}
