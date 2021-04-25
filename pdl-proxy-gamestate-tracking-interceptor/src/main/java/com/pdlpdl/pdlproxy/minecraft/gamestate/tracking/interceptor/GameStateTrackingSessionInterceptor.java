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

import com.github.steveice10.packetlib.Session;
import com.pdlpdl.pdlproxy.minecraft.api.PacketInterceptorControl;
import com.pdlpdl.pdlproxy.minecraft.api.SessionInterceptor;

import java.util.function.BiConsumer;

/**
 * Session interceptor that starts and shuts down packet tracing via the GameStateTrackingPacketInterceptor.
 */
public class GameStateTrackingSessionInterceptor implements SessionInterceptor {

    /**
     * Notify the given listener when the game-state tracking interceptor is created.
     */
    private BiConsumer<Session, GameStateTrackingPacketInterceptor> gameStateTrackingInterceptorListener = null;

//========================================
// Getters and Setters
//----------------------------------------

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
        GameStateTrackingPacketInterceptor interceptor = new GameStateTrackingPacketInterceptor();

        if (this.gameStateTrackingInterceptorListener != null) {
            this.gameStateTrackingInterceptorListener.accept(addedSession, interceptor);
        }

        packetInterceptorControl.addInterceptorAtEnd(interceptor);
    }

    @Override
    public void onDownstreamConnected(Session session, String inGameName) {
    }

    @Override
    public void onSessionRemoved(Session removedSession) {
    }
}
