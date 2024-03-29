package com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model;

import com.artnaseef.immutable.utils.MutationUtilsImmutableProperties;

import java.util.HashMap;

@MutationUtilsImmutableProperties(
        properties = {
                "playerName",
                "playerId",
                "playerEntityId",
                "playerPosition",
                "playerIsOnGround",
                "playerRotation",
                "playerHealth",
                "playerSaturation",
                "playerFood",
                "minecraftWorldBlockState"
        })
public class MinecraftGameState {
    public static final MinecraftGameState INITIAL_GAME_STATE = new MinecraftGameState("unknown", "", -1, null, true, null, 20.0f, 0f, 20, new MinecraftWorldBlockState(new HashMap<>(), 0));

    private final String playerName;
    private final String playerId;  // UUID
    private final int playerEntityId;
    private final Position playerPosition;
    private final boolean playerIsOnGround;
    private final Rotation playerRotation;

    private final float playerHealth;
    private final float playerSaturation;
    private final int playerFood;

    private final MinecraftWorldBlockState minecraftWorldBlockState;

    public MinecraftGameState(
            String playerName,
            String playerId,
            int playerEntityId,
            Position playerPosition,
            boolean playerIsOnGround,
            Rotation playerRotation,
            float playerHealth,
            float playerSaturation,
            int playerFood,
            MinecraftWorldBlockState minecraftWorldBlockState
            ) {

        this.playerName       = playerName;
        this.playerId         = playerId;
        this.playerEntityId   = playerEntityId;
        this.playerPosition   = playerPosition;
        this.playerIsOnGround = playerIsOnGround;
        this.playerRotation   = playerRotation;
        this.playerHealth     = playerHealth;
        this.playerSaturation = playerSaturation;
        this.playerFood       = playerFood;

        if (minecraftWorldBlockState == null) {
            minecraftWorldBlockState = new MinecraftWorldBlockState(null, 0);
        }

        this.minecraftWorldBlockState = minecraftWorldBlockState;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getPlayerId() {
        return playerId;
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

    public float getPlayerHealth() {
        return playerHealth;
    }

    public float getPlayerSaturation() {
        return playerSaturation;
    }

    public int getPlayerFood() {
        return playerFood;
    }

    public MinecraftWorldBlockState getMinecraftWorldBlockState() {
        return minecraftWorldBlockState;
    }

    @Override
    public String toString() {
        return "MinecraftGameState{" +
                "playerEntityId=" + this.playerEntityId +
                ", playerPosition=" + this.playerPosition +
                ", playerIsOnGround=" + this.playerIsOnGround +
                ", playerRotation=" + this.playerRotation +
                ", playerHealth=" + this.playerHealth +
                ", playerSaturation=" + this.playerSaturation +
                ", playerFood=" + this.playerFood +
                ", numChunks=" + this.minecraftWorldBlockState.getChunks().size() +
                '}';
    }
}
