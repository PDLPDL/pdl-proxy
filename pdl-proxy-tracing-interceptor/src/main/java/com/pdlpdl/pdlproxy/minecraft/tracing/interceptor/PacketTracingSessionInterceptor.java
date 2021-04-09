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

package com.pdlpdl.pdlproxy.minecraft.tracing.interceptor;

import com.github.steveice10.packetlib.Session;
import com.pdlpdl.pdlproxy.minecraft.api.PacketInterceptor;
import com.pdlpdl.pdlproxy.minecraft.api.PacketInterceptorControl;
import com.pdlpdl.pdlproxy.minecraft.api.SessionInterceptor;

import java.util.function.Consumer;

/**
 * Session interceptor that starts and shuts down packet tracing via the PacketTracingInterceptor.
 */
public class PacketTracingSessionInterceptor implements SessionInterceptor {

    private Consumer<PacketTracingInterceptor> packetTracingInterceptorConfigurer = null;

//========================================
// Getters and Setters
//----------------------------------------

    public Consumer<PacketTracingInterceptor> getPacketTracingInterceptorConfigurer() {
        return packetTracingInterceptorConfigurer;
    }

    public void setPacketTracingInterceptorConfigurer(Consumer<PacketTracingInterceptor> packetTracingInterceptorConfigurer) {
        this.packetTracingInterceptorConfigurer = packetTracingInterceptorConfigurer;
    }

//========================================
// Session Interceptor Operations
//----------------------------------------

    @Override
    public void onSessionAdded(Session addedSession, PacketInterceptorControl packetInterceptorControl) {
        PacketTracingInterceptor interceptor = new PacketTracingInterceptor();

        if (this.packetTracingInterceptorConfigurer != null) {
            this.packetTracingInterceptorConfigurer.accept(interceptor);
        }

        packetInterceptorControl.insertInterceptorAfter(-1, interceptor);
    }

    @Override
    public void onDownstreamConnected(Session session, String inGameName) {
    }

    @Override
    public void onSessionRemoved(Session removedSession, PacketInterceptorControl packetInterceptorControl) {
        for (PacketInterceptor packetInterceptor : packetInterceptorControl.getInterceptorIterable()) {
            if (packetInterceptor instanceof PacketTracingInterceptor) {
                PacketTracingInterceptor packetTracingInterceptor = (PacketTracingInterceptor) packetInterceptor;

                packetTracingInterceptor.shutdown();
            }
        }
    }
}
