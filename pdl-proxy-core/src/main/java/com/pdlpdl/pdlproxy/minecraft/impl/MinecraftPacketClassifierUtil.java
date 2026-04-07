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
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.debug.*;
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

    private static final Set<Class<? extends Packet>> loginPacketSet = Set.of(
        ClientboundLoginDisconnectPacket.class,
        ClientboundHelloPacket.class,
        ClientboundLoginFinishedPacket.class,
        ClientboundLoginCompressionPacket.class,
        ClientboundCustomQueryPacket.class,
        ClientboundCookieRequestPacket.class,
        ServerboundHelloPacket.class,
        ServerboundKeyPacket.class,
        ServerboundCustomQueryAnswerPacket.class,
        ServerboundLoginAcknowledgedPacket.class,
        ServerboundCookieResponsePacket.class
    );


    private static final Set<Class<? extends Packet>> statusPacketSet = Set.of(
        ClientboundStatusResponsePacket.class,
        ClientboundPongResponsePacket.class,
        ServerboundStatusRequestPacket.class,
        ServerboundPingRequestPacket.class
    );

    private static final Set<Class<? extends Packet>> configurationPacketSet = Set.of(
        ClientboundClearDialogPacket.class,
        ClientboundCodeOfConductPacket.class,
        ClientboundCookieRequestPacket.class,
        ClientboundCustomPayloadPacket.class,
        ClientboundCustomReportDetailsPacket.class,
        ClientboundDisconnectPacket.class,
        ClientboundFinishConfigurationPacket.class,
        ClientboundKeepAlivePacket.class,
        ClientboundPingPacket.class,
        ClientboundRecipeBookAddPacket.class,
        ClientboundRecipeBookSettingsPacket.class,
        ClientboundRegistryDataPacket.class,
        ClientboundResetChatPacket.class,
        ClientboundResourcePackPopPacket.class,
        ClientboundResourcePackPushPacket.class,
        ClientboundSelectKnownPacks.class,
        ClientboundServerLinksPacket.class,
        ClientboundShowDialogConfigurationPacket.class,
        ClientboundStoreCookiePacket.class,
        ClientboundTransferPacket.class,
        ClientboundUpdateEnabledFeaturesPacket.class,
        ClientboundUpdateTagsPacket.class,
        ServerboundAcceptCodeOfConductPacket.class,
        ServerboundClientInformationPacket.class,
        ServerboundCookieResponsePacket.class,
        ServerboundCustomClickActionPacket.class,
        ServerboundCustomPayloadPacket.class,
        ServerboundFinishConfigurationPacket.class,
        ServerboundKeepAlivePacket.class,
        ServerboundPongPacket.class,
        ServerboundResourcePackPacket.class,
        ServerboundSelectKnownPacks.class
    );

    private static final Set<Class<? extends Packet>> gamePacketSet = Set.of(
        ClientboundDelimiterPacket.class,
        ClientboundAddEntityPacket.class,
        ClientboundAnimatePacket.class,
        ClientboundAwardStatsPacket.class,
        ClientboundBlockChangedAckPacket.class,
        ClientboundBlockDestructionPacket.class,
        ClientboundBlockEntityDataPacket.class,
        ClientboundBlockEventPacket.class,
        ClientboundBlockUpdatePacket.class,
        ClientboundBossEventPacket.class,
        ClientboundChangeDifficultyPacket.class,
        ClientboundChunkBatchFinishedPacket.class,
        ClientboundChunkBatchStartPacket.class,
        ClientboundChunksBiomesPacket.class,
        ClientboundClearTitlesPacket.class,
        ClientboundCommandSuggestionsPacket.class,
        ClientboundCommandsPacket.class,
        ClientboundContainerClosePacket.class,
        ClientboundContainerSetContentPacket.class,
        ClientboundContainerSetDataPacket.class,
        ClientboundContainerSetSlotPacket.class,
        ClientboundCookieRequestPacket.class,
        ClientboundCooldownPacket.class,
        ClientboundCustomChatCompletionsPacket.class,
        ClientboundCustomPayloadPacket.class,
        ClientboundDamageEventPacket.class,
        ClientboundDebugBlockValuePacket.class,
        ClientboundDebugChunkValuePacket.class,
        ClientboundDebugEntityValuePacket.class,
        ClientboundDebugEventPacket.class,
        ClientboundDebugSamplePacket.class,
        ClientboundDeleteChatPacket.class,
        ClientboundDisconnectPacket.class,
        ClientboundDisguisedChatPacket.class,
        ClientboundEntityEventPacket.class,
        ClientboundEntityPositionSyncPacket.class,
        ClientboundExplodePacket.class,
        ClientboundForgetLevelChunkPacket.class,
        ClientboundGameEventPacket.class,
        ClientboundGameRuleValuesPacket.class,
        ClientboundGameTestHighlightPosPacket.class,
        ClientboundMountScreenOpenPacket.class,
        ClientboundHurtAnimationPacket.class,
        ClientboundInitializeBorderPacket.class,
        ClientboundKeepAlivePacket.class,
        ClientboundLevelChunkWithLightPacket.class,
        ClientboundLevelEventPacket.class,
        ClientboundLevelParticlesPacket.class,
        ClientboundLightUpdatePacket.class,
        ClientboundLoginPacket.class,
        ClientboundLowDiskSpaceWarningPacket.class,
        ClientboundMapItemDataPacket.class,
        ClientboundMerchantOffersPacket.class,
        ClientboundMoveEntityPosPacket.class,
        ClientboundMoveEntityPosRotPacket.class,
        ClientboundMoveMinecartPacket.class,
        ClientboundMoveEntityRotPacket.class,
        ClientboundMoveVehiclePacket.class,
        ClientboundOpenBookPacket.class,
        ClientboundOpenScreenPacket.class,
        ClientboundOpenSignEditorPacket.class,
        ClientboundPingPacket.class,
        ClientboundPongResponsePacket.class,
        ClientboundPlaceGhostRecipePacket.class,
        ClientboundPlayerAbilitiesPacket.class,
        ClientboundPlayerChatPacket.class,
        ClientboundPlayerCombatEndPacket.class,
        ClientboundPlayerCombatEnterPacket.class,
        ClientboundPlayerCombatKillPacket.class,
        ClientboundPlayerInfoRemovePacket.class,
        ClientboundPlayerInfoUpdatePacket.class,
        ClientboundPlayerLookAtPacket.class,
        ClientboundPlayerPositionPacket.class,
        ClientboundPlayerRotationPacket.class,
        ClientboundRecipeBookAddPacket.class,
        ClientboundRecipeBookRemovePacket.class,
        ClientboundRecipeBookSettingsPacket.class,
        ClientboundRemoveEntitiesPacket.class,
        ClientboundRemoveMobEffectPacket.class,
        ClientboundResetScorePacket.class,
        ClientboundResourcePackPopPacket.class,
        ClientboundResourcePackPushPacket.class,
        ClientboundRespawnPacket.class,
        ClientboundRotateHeadPacket.class,
        ClientboundSectionBlocksUpdatePacket.class,
        ClientboundSelectAdvancementsTabPacket.class,
        ClientboundServerDataPacket.class,
        ClientboundSetActionBarTextPacket.class,
        ClientboundSetBorderCenterPacket.class,
        ClientboundSetBorderLerpSizePacket.class,
        ClientboundSetBorderSizePacket.class,
        ClientboundSetBorderWarningDelayPacket.class,
        ClientboundSetBorderWarningDistancePacket.class,
        ClientboundSetCameraPacket.class,
        ClientboundSetChunkCacheCenterPacket.class,
        ClientboundSetChunkCacheRadiusPacket.class,
        ClientboundSetCursorItemPacket.class,
        ClientboundSetDefaultSpawnPositionPacket.class,
        ClientboundSetDisplayObjectivePacket.class,
        ClientboundSetEntityDataPacket.class,
        ClientboundSetEntityLinkPacket.class,
        ClientboundSetEntityMotionPacket.class,
        ClientboundSetEquipmentPacket.class,
        ClientboundSetExperiencePacket.class,
        ClientboundSetHealthPacket.class,
        ClientboundSetHeldSlotPacket.class,
        ClientboundSetObjectivePacket.class,
        ClientboundSetPassengersPacket.class,
        ClientboundSetPlayerInventoryPacket.class,
        ClientboundSetPlayerTeamPacket.class,
        ClientboundSetScorePacket.class,
        ClientboundSetSimulationDistancePacket.class,
        ClientboundSetSubtitleTextPacket.class,
        ClientboundSetTimePacket.class,
        ClientboundSetTitleTextPacket.class,
        ClientboundSetTitlesAnimationPacket.class,
        ClientboundSoundEntityPacket.class,
        ClientboundSoundPacket.class,
        ClientboundStartConfigurationPacket.class,
        ClientboundStopSoundPacket.class,
        ClientboundStoreCookiePacket.class,
        ClientboundSystemChatPacket.class,
        ClientboundTabListPacket.class,
        ClientboundTagQueryPacket.class,
        ClientboundTakeItemEntityPacket.class,
        ClientboundTeleportEntityPacket.class,
        ClientboundTestInstanceBlockStatus.class,
        ClientboundTickingStatePacket.class,
        ClientboundTickingStepPacket.class,
        ClientboundTransferPacket.class,
        ClientboundUpdateAdvancementsPacket.class,
        ClientboundUpdateAttributesPacket.class,
        ClientboundUpdateMobEffectPacket.class,
        ClientboundUpdateRecipesPacket.class,
        ClientboundUpdateTagsPacket.class,
        ClientboundProjectilePowerPacket.class,
        ClientboundCustomReportDetailsPacket.class,
        ClientboundServerLinksPacket.class,
        ClientboundTrackedWaypointPacket.class,
        ClientboundClearDialogPacket.class,
        ClientboundShowDialogGamePacket.class,
        ServerboundAttackPacket.class,
        ServerboundAcceptTeleportationPacket.class,
        ServerboundBlockEntityTagQueryPacket.class,
        ServerboundSelectBundleItemPacket.class,
        ServerboundChangeDifficultyPacket.class,
        ServerboundChangeGameModePacket.class,
        ServerboundChatAckPacket.class,
        ServerboundChatCommandPacket.class,
        ServerboundChatCommandSignedPacket.class,
        ServerboundChatPacket.class,
        ServerboundChatSessionUpdatePacket.class,
        ServerboundChunkBatchReceivedPacket.class,
        ServerboundClientCommandPacket.class,
        ServerboundClientTickEndPacket.class,
        ServerboundClientInformationPacket.class,
        ServerboundCommandSuggestionPacket.class,
        ServerboundConfigurationAcknowledgedPacket.class,
        ServerboundContainerButtonClickPacket.class,
        ServerboundContainerClickPacket.class,
        ServerboundContainerClosePacket.class,
        ServerboundContainerSlotStateChangedPacket.class,
        ServerboundCookieResponsePacket.class,
        ServerboundCustomPayloadPacket.class,
        ServerboundDebugSubscriptionRequestPacket.class,
        ServerboundEditBookPacket.class,
        ServerboundEntityTagQuery.class,
        ServerboundInteractPacket.class,
        ServerboundJigsawGeneratePacket.class,
        ServerboundKeepAlivePacket.class,
        ServerboundLockDifficultyPacket.class,
        ServerboundMovePlayerPosPacket.class,
        ServerboundMovePlayerPosRotPacket.class,
        ServerboundMovePlayerRotPacket.class,
        ServerboundMovePlayerStatusOnlyPacket.class,
        ServerboundMoveVehiclePacket.class,
        ServerboundPaddleBoatPacket.class,
        ServerboundPickItemFromBlockPacket.class,
        ServerboundPickItemFromEntityPacket.class,
        ServerboundPingRequestPacket.class,
        ServerboundPlaceRecipePacket.class,
        ServerboundPlayerAbilitiesPacket.class,
        ServerboundPlayerActionPacket.class,
        ServerboundPlayerCommandPacket.class,
        ServerboundPlayerInputPacket.class,
        ServerboundPlayerLoadedPacket.class,
        ServerboundPongPacket.class,
        ServerboundRecipeBookChangeSettingsPacket.class,
        ServerboundRecipeBookSeenRecipePacket.class,
        ServerboundRenameItemPacket.class,
        ServerboundResourcePackPacket.class,
        ServerboundSeenAdvancementsPacket.class,
        ServerboundSelectTradePacket.class,
        ServerboundSetBeaconPacket.class,
        ServerboundSetCarriedItemPacket.class,
        ServerboundSetCommandBlockPacket.class,
        ServerboundSetCommandMinecartPacket.class,
        ServerboundSetCreativeModeSlotPacket.class,
        ServerboundSetGameRulePacket.class,
        ServerboundSetJigsawBlockPacket.class,
        ServerboundSetStructureBlockPacket.class,
        ServerboundSetTestBlockPacket.class,
        ServerboundSignUpdatePacket.class,
        ServerboundSpectateEntityPacket.class,
        ServerboundSwingPacket.class,
        ServerboundTeleportToEntityPacket.class,
        ServerboundTestInstanceBlockActionPacket.class,
        ServerboundUseItemOnPacket.class,
        ServerboundUseItemPacket.class,
        ServerboundCustomClickActionPacket.class
    );


    /**
     * Currently only used for logging.
     *
     * WARNING: some packets are valid in multiple states.
     *
     * @param packet
     * @return true => if the packet is a valid CONFIGURATION packet; false => otherwise.
     */
    public boolean isConfigurationPacket(Packet packet) {
        return (MinecraftPacketClassifierUtil.configurationPacketSet.contains(packet.getClass()));
    }

    /**
     * Retrieve the valid set of protocol states for the given packet.
     *
     * @param packet
     * @return set of ProtocolState values for which this packet is valid.
     */
    public Set<ProtocolState> getValidProtocolStateSetForPacket(Packet packet) {
        Set<ProtocolState> result = new HashSet<>();
        Class<? extends Packet> clazz = packet.getClass();

        // Don't bother with a set for this one.
        if (packet instanceof ClientIntentionPacket) {
            result.add(ProtocolState.HANDSHAKE);
        }

        if (MinecraftPacketClassifierUtil.loginPacketSet.contains(clazz)) {
            result.add(ProtocolState.LOGIN);
        }

        if (MinecraftPacketClassifierUtil.statusPacketSet.contains(clazz)) {
            result.add(ProtocolState.STATUS);
        }

        if (MinecraftPacketClassifierUtil.configurationPacketSet.contains(clazz)) {
            result.add(ProtocolState.CONFIGURATION);
        }

        if (MinecraftPacketClassifierUtil.gamePacketSet.contains(clazz)) {
            result.add(ProtocolState.GAME);
        }

        return result;
    }
}
