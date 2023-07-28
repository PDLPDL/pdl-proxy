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

import com.pdlpdl.pdlproxy.minecraft.api.PacketInterceptor;
import com.pdlpdl.pdlproxy.minecraft.api.PacketInterceptorControl;

import java.util.ArrayList;

public class PacketInterceptorControlImpl implements PacketInterceptorControl {

    private final Object lock = new Object();

    private ArrayList<PacketInterceptor> packetInterceptors = new ArrayList<>();

    @Override
    public int getInterceptorCount() {
        synchronized (this.lock) {
            return this.packetInterceptors.size();
        }
    }

    @Override
    public PacketInterceptor getInterceptor(int offset) {
        synchronized (this.lock) {
            return this.packetInterceptors.get(offset);
        }
    }

    @Override
    public Iterable<PacketInterceptor> getInterceptorIterable() {
        synchronized (this.lock) {
            return this.packetInterceptors;
        }
    }

    @Override
    public void removeInterceptor(PacketInterceptor rmPacketInterceptor) {
        synchronized (this.lock) {
            this.packetInterceptors.remove(rmPacketInterceptor);
        }
    }

    @Override
    public void removeInterceptor(int offset) {
        synchronized (this.lock) {
            this.packetInterceptors.remove(offset);
        }
    }

    @Override
    public void insertInterceptorBefore(int offset, PacketInterceptor insertInterceptor) {
        synchronized (this.lock) {
            if (offset < 0) {
                offset = 0;
            }

            this.packetInterceptors.add(offset, insertInterceptor);
        }
    }

    @Override
    public void insertInterceptorAfter(int offset, PacketInterceptor insertInterceptor) {
        synchronized (this.lock) {
            if (offset >= this.packetInterceptors.size()) {
                offset = this.packetInterceptors.size() - 1;
            }
            this.packetInterceptors.add(offset + 1, insertInterceptor);
        }
    }

    @Override
    public void addInterceptorAtEnd(PacketInterceptor addInterceptor) {
        synchronized (this.lock) {
            this.packetInterceptors.add(addInterceptor);
        }
    }
}
