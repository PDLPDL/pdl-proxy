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

package com.pdlpdl.pdlproxy.minecraft.api;

import com.github.steveice10.packetlib.Session;

/**
 * Interceptor of Player Sessions being added to the Proxy.
 */
public interface SessionInterceptor {
    /**
     * Notification when a Player session is added to the Proxy.
     *
     * @param addedSession new session being added.
     * @param packetInterceptorControl control which can be used to adjust the interceptors on the session.
     */
    void onSessionAdded(Session addedSession, PacketInterceptorControl packetInterceptorControl);

    /**
     * Notification when a Player session is removed from the proxy.
     *
     * @param removedSession session being removed.
     * @param packetInterceptorControl control which can be used to view the interceptors registered for the session;
     *                                 note that modifications to the interceptors at this point won't have any effect
     *                                 since the session is already removed.
     */
    void onSessionRemoved(Session removedSession, PacketInterceptorControl packetInterceptorControl);
}
