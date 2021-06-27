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

    /**
     * Update the player position given the new position and is-on-ground indicator.
     *
     * @param orig original Minecraft game state to update.
     * @param updatedPosition new player position to update.
     * @param updatedIsOnGround new "isOnGround" setting for the player; null => do not update.
     * @return the updated game state.  Note the original game state is returned if the updated values are equal to the
     * originals.
     */
    public MinecraftGameState updatePlayerPosition(MinecraftGameState orig, Position updatedPosition, Boolean updatedIsOnGround) {
        if (
                Objects.equals(orig.getPlayerPosition(), updatedPosition) &&
                this.isOnGroundChanging(orig, updatedIsOnGround)
        ) {
            return orig;
        }

        Mutator mutator = this.mutationUtils.makeAnchoredPathMutator((origPosition) -> updatedPosition, MinecraftGameState.class, "playerPosition");

        mutator = this.mergeIsOnGroundUpdateAsNeeded(orig, updatedIsOnGround, mutator);

        return this.mutationUtils.mutateDeep(orig, mutator);
    }

    /**
     * Update the player rotation in the given game state.
     *
     * @param orig original Minecraft game state to update.
     * @param updatedRotation new rotation for the player.
     * @param updatedIsOnGround new "isOnGround" setting for the player; null => do not update.
     * @return the updated game state.  Note the original game state is returned if the updated values are equal to the
     * originals.
     */
    public MinecraftGameState updatePlayerRotation(MinecraftGameState orig, Rotation updatedRotation, Boolean updatedIsOnGround) {
        if (
                Objects.equals(orig.getPlayerRotation(), updatedRotation) &&
                (! this.isOnGroundChanging(orig, updatedIsOnGround))
        ) {
            return orig;
        }

        Mutator mutator =
                this.mutationUtils.makeAnchoredPathMutator((origRotation) -> updatedRotation, MinecraftGameState.class, "playerRotation");

        mutator = this.mergeIsOnGroundUpdateAsNeeded(orig, updatedIsOnGround, mutator);

        return this.mutationUtils.mutateDeep(orig, mutator);
    }

    /**
     * Update both the player position and rotation in the given game state.
     *
     * @param orig original Minecraft game state to update.
     * @param updatedPosition new player position to update.
     * @param updatedRotation new rotation for the player.
     * @param updatedIsOnGround new "isOnGround" setting for the player; null => do not update.
     * @return the updated game state.  Note the original game state is returned if the updated values are equal to the
     * originals.
     */
    public MinecraftGameState updatePlayerPositionRotation(MinecraftGameState orig, Position updatedPosition, Rotation updatedRotation, Boolean updatedIsOnGround) {
        MinecraftGameState intermediate = this.updatePlayerPosition(orig, updatedPosition, updatedIsOnGround);
        return this.updatePlayerRotation(intermediate, updatedRotation, null);
    }

    /**
     * Update the player health in the given game state.
     *
     * @param orig original Minecraft game state to update.
     * @param updatedHealth new player health value.
     * @param updatedSaturation new player saturation value.
     * @param updatedFood new player food value.
     * @return the updated game state.  Note the original game state is returned if the updated values are equal to the
     * originals.
     */
    public MinecraftGameState updatePlayerHealth(MinecraftGameState orig, float updatedHealth, float updatedSaturation, int updatedFood) {
        if (
                ( orig.getPlayerHealth() == updatedHealth ) &&
                ( orig.getPlayerSaturation() == updatedSaturation) &&
                ( orig.getPlayerFood() == updatedFood )
        ) {
            return orig;
        }

        Mutator healthMutator =
                this.mutationUtils.makeAnchoredPathMutator((origHealth) -> updatedHealth, MinecraftGameState.class, "playerHealth");
        Mutator saturationMutator =
                this.mutationUtils.makeAnchoredPathMutator((origSaturation) -> updatedSaturation, MinecraftGameState.class, "playerSaturation");
        Mutator foodMutator =
                this.mutationUtils.makeAnchoredPathMutator((origFood) -> updatedFood, MinecraftGameState.class, "playerFood");

        Mutator fullMutator = this.mutationUtils.combineMutators(healthMutator, saturationMutator, foodMutator);

        return this.mutationUtils.mutateDeep(orig, fullMutator);
    }

//========================================
//
//----------------------------------------

    /**
     * Convenience helper - check if playerIsOnGround needs to be updated.
     *
     * @param minecraftGameState original minecraft game state to mutate.
     * @param updateValue new settings for is-on-ground, or null to leave unchanged.
     * @return true => the new setting needs to be applied and differs from the original game state.
     */
    private boolean isOnGroundChanging(MinecraftGameState minecraftGameState, Boolean updateValue) {
        if (updateValue != null) {
            return minecraftGameState.getPlayerIsOnGround() == updateValue;
        }

        return false;
    }

    /**
     * Merge the is-on-ground update mutator into the given mutator, if the update is needed.
     *
     * @param minecraftGameState original minecraft game state to mutate.
     * @param updatedIsOnGround new "isOnGround" setting for the player; null => do not update.
     * @param origMutator original mutator to merge with the is-on-ground update mutator, if needed.
     * @return the resulting mutator: either a merged mutator using the original update followed by the is-on-ground
     * mutator, or the original mutator if the is-on-ground value is not being updated.
     */
    private Mutator mergeIsOnGroundUpdateAsNeeded(MinecraftGameState minecraftGameState, Boolean updatedIsOnGround, Mutator origMutator) {
        if (this.isOnGroundChanging(minecraftGameState, updatedIsOnGround)) {
            return
                    this.mutationUtils.combineMutators(
                            origMutator,
                            this.mutationUtils.makeAnchoredPathMutator((origIsOnGround) -> updatedIsOnGround, MinecraftGameState.class, "playerIsOnGround")
                    );
        }

        // No is-on-ground update; just use the original mutator.
        return origMutator;
    }
}
