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

package com.pdlpdl.pdlproxy.minecraft.main.poc;

import com.github.steveice10.packetlib.Session;
import com.pdlpdl.pdlproxy.minecraft.api.PacketInterceptorControl;
import com.pdlpdl.pdlproxy.minecraft.api.SessionInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhantomBeeSessionInterceptor implements SessionInterceptor {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(PhantomBeeSessionInterceptor.class);

    private Logger log = DEFAULT_LOGGER;

    @Override
    public void onSessionAdded(Session addedSession, PacketInterceptorControl packetInterceptorControl) {
        this.log.debug("BEE session interceptor: new session added");

        packetInterceptorControl.insertInterceptorAfter(-1, new PhantomBeePacketInterceptor());
    }

    @Override
    public void onDownstreamConnected(Session session, String inGameName) {
    }

    @Override
    public void onSessionRemoved(Session removedSession) {

    }
}
