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

package com.pdlpdl.pdlproxy.minecraft.ioc;

import com.pdlpdl.pdlproxy.minecraft.api.ProxyServer;
import com.pdlpdl.pdlproxy.minecraft.api.SessionInterceptor;

import java.util.List;

/**
 * Injector of a list of SessionInterceptors into a ProxyServer.
 */
public class ProxyServerSessionInterceptorInjector {
    private ProxyServer proxyServer;
    private List<SessionInterceptor> sessionInterceptors;

//========================================
// Getters and Setters
//----------------------------------------

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public void setProxyServer(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    public List<SessionInterceptor> getSessionInterceptors() {
        return sessionInterceptors;
    }

    public void setSessionInterceptors(List<SessionInterceptor> sessionInterceptors) {
        this.sessionInterceptors = sessionInterceptors;
    }

//========================================
// Lifecycle
//----------------------------------------

    /**
     * Perform the injection now.
     */
    public void init() {
        for (SessionInterceptor oneInterceptor : this.sessionInterceptors) {
            this.proxyServer.getSessionInterceptorControl().addInterceptorAtEnd(oneInterceptor);
        }
    }
}
