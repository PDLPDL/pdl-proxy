package com.pdlpdl.pdlproxy.minecraft.impl;

import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.*;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.*;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.*;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.*;
import org.geysermc.mcprotocollib.protocol.packet.cookie.clientbound.*;
import org.geysermc.mcprotocollib.protocol.packet.cookie.serverbound.*;
import org.geysermc.mcprotocollib.protocol.packet.handshake.serverbound.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.border.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.*;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.*;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.*;
import org.geysermc.mcprotocollib.protocol.packet.ping.clientbound.*;
import org.geysermc.mcprotocollib.protocol.packet.ping.serverbound.*;
import org.geysermc.mcprotocollib.protocol.packet.status.clientbound.*;
import org.geysermc.mcprotocollib.protocol.packet.status.serverbound.*;

import java.util.HashSet;
import java.util.Set;

public class MinecraftPacketClassifierUtil {
    /**
     * Currently only used for logging.
     *
     * WARNING: some packets are valid in multiple states.
     *
     * @param packet
     * @return true => if the packet is a valid CONFIGURATION packet; false => otherwise.
     */
    public boolean isConfigurationPacket(Packet packet) {
        return (
            (packet instanceof ClientboundCookieRequestPacket) ||
                (packet instanceof ClientboundCustomPayloadPacket) ||   // NOTE can also be sent in GAME mode
                (packet instanceof ClientboundDisconnectPacket) ||
                (packet instanceof ClientboundFinishConfigurationPacket) ||
                (packet instanceof ClientboundRegistryDataPacket) ||
                (packet instanceof ClientboundResourcePackPopPacket) ||
                (packet instanceof ClientboundResourcePackPushPacket) ||
                (packet instanceof ClientboundStoreCookiePacket) ||
                (packet instanceof ClientboundTransferPacket) ||
                (packet instanceof ClientboundUpdateEnabledFeaturesPacket) ||
                (packet instanceof ClientboundUpdateTagsPacket) ||
                (packet instanceof ClientboundSelectKnownPacks) ||
                (packet instanceof ClientboundCustomReportDetailsPacket) ||
                (packet instanceof ClientboundServerLinksPacket) ||
                (packet instanceof ClientboundRecipeBookSettingsPacket) ||
                (packet instanceof ClientboundRecipeBookAddPacket) ||
                (packet instanceof ServerboundClientInformationPacket) ||
                (packet instanceof ServerboundCookieResponsePacket) ||
                (packet instanceof ServerboundCustomPayloadPacket) ||
                (packet instanceof ServerboundFinishConfigurationPacket) ||
                (packet instanceof ServerboundKeepAlivePacket) ||
                (packet instanceof ServerboundPongPacket) ||
                (packet instanceof ServerboundResourcePackPacket) ||
                (packet instanceof ServerboundSelectKnownPacks)
        );
    }

    /**
     * Retrieve the valid set of protocol states for the given packet.
     *
     * @param packet
     * @return set of ProtocolState values for which this packet is valid.
     */
    public Set<ProtocolState> getValidProtocolStateSetForPacket(Packet packet) {
        Set<ProtocolState> result = new HashSet<>();

        if (packet instanceof ClientIntentionPacket) {
            result.add(ProtocolState.HANDSHAKE);
        }

        if ((packet instanceof ClientboundLoginDisconnectPacket) ||
            (packet instanceof ClientboundHelloPacket) ||
            (packet instanceof ClientboundLoginFinishedPacket) ||
            (packet instanceof ClientboundLoginCompressionPacket) ||
            (packet instanceof ClientboundCustomQueryPacket) ||
            (packet instanceof ClientboundCookieRequestPacket) ||
            (packet instanceof ServerboundHelloPacket) ||
            (packet instanceof ServerboundKeyPacket) ||
            (packet instanceof ServerboundCustomQueryAnswerPacket) ||
            (packet instanceof ServerboundLoginAcknowledgedPacket) ||
            (packet instanceof ServerboundCookieResponsePacket)) {
            result.add(ProtocolState.LOGIN);
        }

        if ((packet instanceof ClientboundStatusResponsePacket) ||
            (packet instanceof ClientboundPongResponsePacket) ||
            (packet instanceof ServerboundStatusRequestPacket) ||
            (packet instanceof ServerboundPingRequestPacket)) {
            result.add(ProtocolState.STATUS);
        }

        if ((packet instanceof ClientboundCookieRequestPacket) ||
            (packet instanceof ClientboundCustomPayloadPacket) ||
            (packet instanceof ClientboundDisconnectPacket) ||
            (packet instanceof ClientboundFinishConfigurationPacket) ||
            (packet instanceof ClientboundKeepAlivePacket) ||
            (packet instanceof ClientboundPingPacket) ||
            (packet instanceof ClientboundResetChatPacket) ||
            (packet instanceof ClientboundRegistryDataPacket) ||
            (packet instanceof ClientboundResourcePackPopPacket) ||
            (packet instanceof ClientboundResourcePackPushPacket) ||
            (packet instanceof ClientboundStoreCookiePacket) ||
            (packet instanceof ClientboundTransferPacket) ||
            (packet instanceof ClientboundUpdateEnabledFeaturesPacket) ||
            (packet instanceof ClientboundUpdateTagsPacket) ||
            (packet instanceof ClientboundSelectKnownPacks) ||
            (packet instanceof ClientboundCustomReportDetailsPacket) ||
            (packet instanceof ClientboundServerLinksPacket) ||
            (packet instanceof ServerboundClientInformationPacket) ||
            (packet instanceof ServerboundCookieResponsePacket) ||
            (packet instanceof ServerboundCustomPayloadPacket) ||
            (packet instanceof ServerboundFinishConfigurationPacket) ||
            (packet instanceof ServerboundKeepAlivePacket) ||
            (packet instanceof ServerboundPongPacket) ||
            (packet instanceof ServerboundResourcePackPacket) ||
            (packet instanceof ServerboundSelectKnownPacks)
        ) {
            result.add(ProtocolState.CONFIGURATION);
        }

        if ((packet instanceof ClientboundDelimiterPacket) ||
            (packet instanceof ClientboundAddEntityPacket) ||
            (packet instanceof ClientboundAnimatePacket) ||
            (packet instanceof ClientboundAwardStatsPacket) ||
            (packet instanceof ClientboundBlockChangedAckPacket) ||
            (packet instanceof ClientboundBlockDestructionPacket) ||
            (packet instanceof ClientboundBlockEntityDataPacket) ||
            (packet instanceof ClientboundBlockEventPacket) ||
            (packet instanceof ClientboundBlockUpdatePacket) ||
            (packet instanceof ClientboundBossEventPacket) ||
            (packet instanceof ClientboundChangeDifficultyPacket) ||
            (packet instanceof ClientboundChunkBatchFinishedPacket) ||
            (packet instanceof ClientboundChunkBatchStartPacket) ||
            (packet instanceof ClientboundChunksBiomesPacket) ||
            (packet instanceof ClientboundClearTitlesPacket) ||
            (packet instanceof ClientboundCommandSuggestionsPacket) ||
            (packet instanceof ClientboundCommandsPacket) ||
            (packet instanceof ClientboundContainerClosePacket) ||
            (packet instanceof ClientboundContainerSetContentPacket) ||
            (packet instanceof ClientboundContainerSetDataPacket) ||
            (packet instanceof ClientboundContainerSetSlotPacket) ||
            (packet instanceof ClientboundCookieRequestPacket) ||
            (packet instanceof ClientboundCooldownPacket) ||
            (packet instanceof ClientboundCustomChatCompletionsPacket) ||
            (packet instanceof ClientboundCustomPayloadPacket) ||
            (packet instanceof ClientboundDamageEventPacket) ||
            (packet instanceof ClientboundDebugSamplePacket) ||
            (packet instanceof ClientboundDeleteChatPacket) ||
            (packet instanceof ClientboundDisconnectPacket) ||
            (packet instanceof ClientboundDisguisedChatPacket) ||
            (packet instanceof ClientboundEntityEventPacket) ||
            (packet instanceof ClientboundEntityPositionSyncPacket) ||
            (packet instanceof ClientboundExplodePacket) ||
            (packet instanceof ClientboundForgetLevelChunkPacket) ||
            (packet instanceof ClientboundGameEventPacket) ||
            (packet instanceof ClientboundHorseScreenOpenPacket) ||
            (packet instanceof ClientboundHurtAnimationPacket) ||
            (packet instanceof ClientboundInitializeBorderPacket) ||
            (packet instanceof ClientboundKeepAlivePacket) ||
            (packet instanceof ClientboundLevelChunkWithLightPacket) ||
            (packet instanceof ClientboundLevelEventPacket) ||
            (packet instanceof ClientboundLevelParticlesPacket) ||
            (packet instanceof ClientboundLightUpdatePacket) ||
            (packet instanceof ClientboundLoginPacket) ||
            (packet instanceof ClientboundMapItemDataPacket) ||
            (packet instanceof ClientboundMerchantOffersPacket) ||
            (packet instanceof ClientboundMoveEntityPosPacket) ||
            (packet instanceof ClientboundMoveEntityPosRotPacket) ||
            (packet instanceof ClientboundMoveMinecartPacket) ||
            (packet instanceof ClientboundMoveEntityRotPacket) ||
            (packet instanceof ClientboundMoveVehiclePacket) ||
            (packet instanceof ClientboundOpenBookPacket) ||
            (packet instanceof ClientboundOpenScreenPacket) ||
            (packet instanceof ClientboundOpenSignEditorPacket) ||
            (packet instanceof ClientboundPingPacket) ||
            (packet instanceof ClientboundPongResponsePacket) ||
            (packet instanceof ClientboundPlaceGhostRecipePacket) ||
            (packet instanceof ClientboundPlayerAbilitiesPacket) ||
            (packet instanceof ClientboundPlayerChatPacket) ||
            (packet instanceof ClientboundPlayerCombatEndPacket) ||
            (packet instanceof ClientboundPlayerCombatEnterPacket) ||
            (packet instanceof ClientboundPlayerCombatKillPacket) ||
            (packet instanceof ClientboundPlayerInfoRemovePacket) ||
            (packet instanceof ClientboundPlayerInfoUpdatePacket) ||
            (packet instanceof ClientboundPlayerLookAtPacket) ||
            (packet instanceof ClientboundPlayerPositionPacket) ||
            (packet instanceof ClientboundPlayerRotationPacket) ||
            (packet instanceof ClientboundRecipeBookAddPacket) ||
            (packet instanceof ClientboundRecipeBookRemovePacket) ||
            (packet instanceof ClientboundRecipeBookSettingsPacket) ||
            (packet instanceof ClientboundRemoveEntitiesPacket) ||
            (packet instanceof ClientboundRemoveMobEffectPacket) ||
            (packet instanceof ClientboundResetScorePacket) ||
            (packet instanceof ClientboundResourcePackPopPacket) ||
            (packet instanceof ClientboundResourcePackPushPacket) ||
            (packet instanceof ClientboundRespawnPacket) ||
            (packet instanceof ClientboundRotateHeadPacket) ||
            (packet instanceof ClientboundSectionBlocksUpdatePacket) ||
            (packet instanceof ClientboundSelectAdvancementsTabPacket) ||
            (packet instanceof ClientboundServerDataPacket) ||
            (packet instanceof ClientboundSetActionBarTextPacket) ||
            (packet instanceof ClientboundSetBorderCenterPacket) ||
            (packet instanceof ClientboundSetBorderLerpSizePacket) ||
            (packet instanceof ClientboundSetBorderSizePacket) ||
            (packet instanceof ClientboundSetBorderWarningDelayPacket) ||
            (packet instanceof ClientboundSetBorderWarningDistancePacket) ||
            (packet instanceof ClientboundSetCameraPacket) ||
            (packet instanceof ClientboundSetChunkCacheCenterPacket) ||
            (packet instanceof ClientboundSetChunkCacheRadiusPacket) ||
            (packet instanceof ClientboundSetCursorItemPacket) ||
            (packet instanceof ClientboundSetDefaultSpawnPositionPacket) ||
            (packet instanceof ClientboundSetDisplayObjectivePacket) ||
            (packet instanceof ClientboundSetEntityDataPacket) ||
            (packet instanceof ClientboundSetEntityLinkPacket) ||
            (packet instanceof ClientboundSetEntityMotionPacket) ||
            (packet instanceof ClientboundSetEquipmentPacket) ||
            (packet instanceof ClientboundSetExperiencePacket) ||
            (packet instanceof ClientboundSetHealthPacket) ||
            (packet instanceof ClientboundSetHeldSlotPacket) ||
            (packet instanceof ClientboundSetObjectivePacket) ||
            (packet instanceof ClientboundSetPassengersPacket) ||
            (packet instanceof ClientboundSetPlayerInventoryPacket) ||
            (packet instanceof ClientboundSetPlayerTeamPacket) ||
            (packet instanceof ClientboundSetScorePacket) ||
            (packet instanceof ClientboundSetSimulationDistancePacket) ||
            (packet instanceof ClientboundSetSubtitleTextPacket) ||
            (packet instanceof ClientboundSetTimePacket) ||
            (packet instanceof ClientboundSetTitleTextPacket) ||
            (packet instanceof ClientboundSetTitlesAnimationPacket) ||
            (packet instanceof ClientboundSoundEntityPacket) ||
            (packet instanceof ClientboundSoundPacket) ||
            (packet instanceof ClientboundStartConfigurationPacket) ||
            (packet instanceof ClientboundStopSoundPacket) ||
            (packet instanceof ClientboundStoreCookiePacket) ||
            (packet instanceof ClientboundSystemChatPacket) ||
            (packet instanceof ClientboundTabListPacket) ||
            (packet instanceof ClientboundTagQueryPacket) ||
            (packet instanceof ClientboundTakeItemEntityPacket) ||
            (packet instanceof ClientboundTeleportEntityPacket) ||
            (packet instanceof ClientboundTestInstanceBlockStatus) ||
            (packet instanceof ClientboundTickingStatePacket) ||
            (packet instanceof ClientboundTickingStepPacket) ||
            (packet instanceof ClientboundTransferPacket) ||
            (packet instanceof ClientboundUpdateAdvancementsPacket) ||
            (packet instanceof ClientboundUpdateAttributesPacket) ||
            (packet instanceof ClientboundUpdateMobEffectPacket) ||
            (packet instanceof ClientboundUpdateRecipesPacket) ||
            (packet instanceof ClientboundUpdateTagsPacket) ||
            (packet instanceof ClientboundProjectilePowerPacket) ||
            (packet instanceof ClientboundCustomReportDetailsPacket) ||
            (packet instanceof ClientboundServerLinksPacket) ||
            (packet instanceof ServerboundAcceptTeleportationPacket) ||
            (packet instanceof ServerboundBlockEntityTagQueryPacket) ||
            (packet instanceof ServerboundSelectBundleItemPacket) ||
            (packet instanceof ServerboundChangeDifficultyPacket) ||
            (packet instanceof ServerboundChatAckPacket) ||
            (packet instanceof ServerboundChatCommandPacket) ||
            (packet instanceof ServerboundChatCommandSignedPacket) ||
            (packet instanceof ServerboundChatPacket) ||
            (packet instanceof ServerboundChatSessionUpdatePacket) ||
            (packet instanceof ServerboundChunkBatchReceivedPacket) ||
            (packet instanceof ServerboundClientCommandPacket) ||
            (packet instanceof ServerboundClientTickEndPacket) ||
            (packet instanceof ServerboundClientInformationPacket) ||
            (packet instanceof ServerboundCommandSuggestionPacket) ||
            (packet instanceof ServerboundConfigurationAcknowledgedPacket) ||
            (packet instanceof ServerboundContainerButtonClickPacket) ||
            (packet instanceof ServerboundContainerClickPacket) ||
            (packet instanceof ServerboundContainerClosePacket) ||
            (packet instanceof ServerboundContainerSlotStateChangedPacket) ||
            (packet instanceof ServerboundCookieResponsePacket) ||
            (packet instanceof ServerboundCustomPayloadPacket) ||
            (packet instanceof ServerboundDebugSampleSubscriptionPacket) ||
            (packet instanceof ServerboundEditBookPacket) ||
            (packet instanceof ServerboundEntityTagQuery) ||
            (packet instanceof ServerboundInteractPacket) ||
            (packet instanceof ServerboundJigsawGeneratePacket) ||
            (packet instanceof ServerboundKeepAlivePacket) ||
            (packet instanceof ServerboundLockDifficultyPacket) ||
            (packet instanceof ServerboundMovePlayerPosPacket) ||
            (packet instanceof ServerboundMovePlayerPosRotPacket) ||
            (packet instanceof ServerboundMovePlayerRotPacket) ||
            (packet instanceof ServerboundMovePlayerStatusOnlyPacket) ||
            (packet instanceof ServerboundMoveVehiclePacket) ||
            (packet instanceof ServerboundPaddleBoatPacket) ||
            (packet instanceof ServerboundPickItemFromBlockPacket) ||
            (packet instanceof ServerboundPickItemFromEntityPacket) ||
            (packet instanceof ServerboundPingRequestPacket) ||
            (packet instanceof ServerboundPlaceRecipePacket) ||
            (packet instanceof ServerboundPlayerAbilitiesPacket) ||
            (packet instanceof ServerboundPlayerActionPacket) ||
            (packet instanceof ServerboundPlayerCommandPacket) ||
            (packet instanceof ServerboundPlayerInputPacket) ||
            (packet instanceof ServerboundPlayerLoadedPacket) ||
            (packet instanceof ServerboundPongPacket) ||
            (packet instanceof ServerboundRecipeBookChangeSettingsPacket) ||
            (packet instanceof ServerboundRecipeBookSeenRecipePacket) ||
            (packet instanceof ServerboundRenameItemPacket) ||
            (packet instanceof ServerboundResourcePackPacket) ||
            (packet instanceof ServerboundSeenAdvancementsPacket) ||
            (packet instanceof ServerboundSelectTradePacket) ||
            (packet instanceof ServerboundSetBeaconPacket) ||
            (packet instanceof ServerboundSetCarriedItemPacket) ||
            (packet instanceof ServerboundSetCommandBlockPacket) ||
            (packet instanceof ServerboundSetCommandMinecartPacket) ||
            (packet instanceof ServerboundSetCreativeModeSlotPacket) ||
            (packet instanceof ServerboundSetJigsawBlockPacket) ||
            (packet instanceof ServerboundSetStructureBlockPacket) ||
            (packet instanceof ServerboundSetTestBlockPacket) ||
            (packet instanceof ServerboundSignUpdatePacket) ||
            (packet instanceof ServerboundSwingPacket) ||
            (packet instanceof ServerboundTeleportToEntityPacket) ||
            (packet instanceof ServerboundTestInstanceBlockActionPacket) ||
            (packet instanceof ServerboundUseItemOnPacket) ||
            (packet instanceof ServerboundUseItemPacket)) {
            result.add(ProtocolState.GAME);
        }

        return result;
    }
}
