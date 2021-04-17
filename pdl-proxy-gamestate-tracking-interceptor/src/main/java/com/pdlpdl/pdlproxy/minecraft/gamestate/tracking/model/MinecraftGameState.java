package com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model;

import com.artnaseef.immutable.utils.MutationUtilsImmutableProperties;

@MutationUtilsImmutableProperties(properties = {"playerEntityId", "playerPosition", "playerIsOnGround", "playerRotation"})
public class MinecraftGameState {
    public static final MinecraftGameState INITIAL_GAME_STATE = new MinecraftGameState(-1, null, true, null);

    private final int playerEntityId;
    private final Position playerPosition;
    private final boolean playerIsOnGround;
    private final Rotation playerRotation;

    public MinecraftGameState(int playerEntityId, Position playerPosition, boolean playerIsOnGround, Rotation playerRotation) {
        this.playerEntityId   = playerEntityId;
        this.playerPosition   = playerPosition;
        this.playerIsOnGround = playerIsOnGround;
        this.playerRotation   = playerRotation;
    }

    public int getPlayerEntityId() {
        return playerEntityId;
    }

    public Position getPlayerPosition() {
        return playerPosition;
    }

    public boolean getPlayerIsOnGround() {
        return playerIsOnGround;
    }

    public Rotation getPlayerRotation() {
        return playerRotation;
    }

    @Override
    public String toString() {
        return "MinecraftGameState{" +
                "playerEntityId=" + this.playerEntityId +
                ", playerPosition=" + this.playerPosition +
                ", playerIsOnGround=" + this.playerIsOnGround +
                ", playerRotation=" + this.playerRotation +
                '}';
    }
}
