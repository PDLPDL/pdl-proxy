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

package com.pdlpdl.pdlproxy.minecraft.impl;

import com.github.steveice10.packetlib.Session;

public class ProxyClientSessionState {
    private final Session clientSession;
    private final ProxyClientSessionAdapter proxyClientSessionAdapter;

    public ProxyClientSessionState(Session clientSession, ProxyClientSessionAdapter proxyClientSessionAdapter) {
        this.clientSession = clientSession;
        this.proxyClientSessionAdapter = proxyClientSessionAdapter;
    }

    public Session getClientSession() {
        return clientSession;
    }

    public ProxyClientSessionAdapter getProxyClientSessionAdapter() {
        return proxyClientSessionAdapter;
    }
}
