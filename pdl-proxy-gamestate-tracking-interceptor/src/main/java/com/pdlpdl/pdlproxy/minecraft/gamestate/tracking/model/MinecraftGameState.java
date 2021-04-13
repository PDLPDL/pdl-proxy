package com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model;

import com.artnaseef.immutable.utils.MutationUtilsImmutableProperties;

@MutationUtilsImmutableProperties(properties = {"playerEntityId", "playerPosition", "playerRotation"})
public class MinecraftGameState {
    public static final MinecraftGameState INITIAL_GAME_STATE = new MinecraftGameState(-1, null, null);

    private final int playerEntityId;
    private final Position playerPosition;
    private final Rotation playerRotation;

    public MinecraftGameState(int playerEntityId, Position playerPosition, Rotation playerRotation) {
        this.playerEntityId = playerEntityId;
        this.playerPosition = playerPosition;
        this.playerRotation = playerRotation;
    }

    public int getPlayerEntityId() {
        return playerEntityId;
    }

    public Position getPlayerPosition() {
        return playerPosition;
    }

    public Rotation getPlayerRotation() {
        return playerRotation;
    }

    @Override
    public String toString() {
        return "MinecraftGameState{" +
                "playerEntityId=" + playerEntityId +
                ", playerPosition=" + playerPosition +
                ", playerRotation=" + playerRotation +
                '}';
    }
}
