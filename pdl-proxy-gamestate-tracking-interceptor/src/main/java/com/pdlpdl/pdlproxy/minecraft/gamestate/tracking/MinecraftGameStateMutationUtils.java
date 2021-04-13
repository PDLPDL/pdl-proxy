package com.pdlpdl.pdlproxy.minecraft.gamestate.tracking;

import com.artnaseef.immutable.utils.MutationUtils;
import com.artnaseef.immutable.utils.Mutator;
import com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model.MinecraftGameState;
import com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model.Position;
import com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model.Rotation;

import java.util.Objects;

public class MinecraftGameStateMutationUtils {
    private MutationUtils mutationUtils = new MutationUtils();

//========================================
//
//----------------------------------------

    public MutationUtils getMutationUtils() {
        return mutationUtils;
    }

    public void setMutationUtils(MutationUtils mutationUtils) {
        this.mutationUtils = mutationUtils;
    }

//========================================
//
//----------------------------------------

    public MinecraftGameState updatePlayerEntityId(MinecraftGameState orig, int newEntityId) {
        Mutator entityIdMutator =
                this.mutationUtils.makeAnchoredPathMutator((origEntityId) -> newEntityId, MinecraftGameState.class, "playerEntityId");

        return this.mutationUtils.mutateDeep(orig, entityIdMutator);
    }

    public MinecraftGameState updatePlayerPosition(MinecraftGameState orig, Position updatedPosition) {
        if (Objects.equals(orig.getPlayerPosition(), updatedPosition)) {
            return orig;
        }

        Mutator positionMutator =
                this.mutationUtils.makeAnchoredPathMutator((origPosition) -> updatedPosition, MinecraftGameState.class, "playerPosition");

        return this.mutationUtils.mutateDeep(orig, positionMutator);
    }

    public MinecraftGameState updatePlayerRotation(MinecraftGameState orig, Rotation updatedRotation) {
        if (Objects.equals(orig.getPlayerRotation(), updatedRotation)) {
            return orig;
        }

        Mutator rotationMutator =
                this.mutationUtils.makeAnchoredPathMutator((origRotation) -> updatedRotation, MinecraftGameState.class, "playerRotation");

        return this.mutationUtils.mutateDeep(orig, rotationMutator);
    }

    public MinecraftGameState updatePlayerPositionRotation(MinecraftGameState orig, Position updatedPosition, Rotation updatedRotation) {
        MinecraftGameState intermediate = this.updatePlayerPosition(orig, updatedPosition);
        return this.updatePlayerRotation(intermediate, updatedRotation);
    }
}
