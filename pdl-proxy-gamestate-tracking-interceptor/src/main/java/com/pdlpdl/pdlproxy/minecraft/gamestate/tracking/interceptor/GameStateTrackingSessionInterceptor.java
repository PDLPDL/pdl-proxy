/*
 * Copyright (c) 2021 Playful Digital Learning LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.interceptor;

import com.pdlpdl.pdlproxy.minecraft.gamestate.datagen.MinecraftDatagenManager;
import org.geysermc.mcprotocollib.network.Session;
import com.pdlpdl.pdlproxy.minecraft.api.PacketInterceptorControl;
import com.pdlpdl.pdlproxy.minecraft.api.SessionInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.BiConsumer;

/**
 * Session interceptor that starts and shuts down game state tracking via the GameStateTrackingPacketInterceptor.
 */
public class GameStateTrackingSessionInterceptor implements SessionInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(GameStateTrackingSessionInterceptor.class);

    /**
     * Notify the given listener when the game-state tracking interceptor is created.
     */
    private BiConsumer<Session, GameStateTrackingPacketInterceptor> gameStateTrackingInterceptorListener = null;

    /**
     * Minecraft Datagen Manager used to load and maintain Minecraft "generated" data (version-specific data with
     *  hard-coded defaults built-into the servers and clients, which can be accessed by generating reports with a
     *  special startup command-line invocation).
     *
     * @return
     */
    private MinecraftDatagenManager minecraftDatagenManager;

//========================================
// Lifecycle
//----------------------------------------

    /**
     * Preload to fail-fast and provide better error handling if the datagen content access is problematic.
     *
     * NOTE that setMinecraftDatagenManager() should be called first, if the caller wants to define their own manager
     *  (for example, to use override files in place of the hard-coded, built-in versions).  If this is called before
     *  setMinecraftDatagenManager(), after the set, the entire value of calling preload() here is lost.
     *
     * @throws IOException
     */
    public void preload() throws IOException {
        if (this.minecraftDatagenManager == null) {
            this.minecraftDatagenManager = new MinecraftDatagenManager();
            this.minecraftDatagenManager.preload();
        }
    }

//========================================
// Getters and Setters
//----------------------------------------

    public MinecraftDatagenManager getMinecraftDatagenManager() {
        return minecraftDatagenManager;
    }

    public void setMinecraftDatagenManager(MinecraftDatagenManager minecraftDatagenManager) {
        this.minecraftDatagenManager = minecraftDatagenManager;
    }

    public BiConsumer<Session, GameStateTrackingPacketInterceptor> getGameStateTrackingInterceptorListener() {
        return gameStateTrackingInterceptorListener;
    }

    public void setGameStateTrackingInterceptorListener(BiConsumer<Session, GameStateTrackingPacketInterceptor> gameStateTrackingInterceptorListener) {
        this.gameStateTrackingInterceptorListener = gameStateTrackingInterceptorListener;
    }


//========================================
// Session Interceptor Operations
//----------------------------------------

    @Override
    public void onSessionAdded(Session addedSession, PacketInterceptorControl packetInterceptorControl) {
        GameStateTrackingPacketInterceptor interceptor = new GameStateTrackingPacketInterceptor(this.minecraftDatagenManager);

        if (this.gameStateTrackingInterceptorListener != null) {
            this.gameStateTrackingInterceptorListener.accept(addedSession, interceptor);
        }

        LOG.info("Installing Game State Tracking interceptor");
        packetInterceptorControl.addInterceptorAtEnd(interceptor);
    }

    @Override
    public void onDownstreamConnected(Session session, String inGameName) {
    }

    @Override
    public void onSessionRemoved(Session removedSession) {
    }
}
