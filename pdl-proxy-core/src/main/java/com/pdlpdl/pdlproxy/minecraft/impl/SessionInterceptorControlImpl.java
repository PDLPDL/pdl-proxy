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

import com.pdlpdl.pdlproxy.minecraft.api.SessionInterceptor;
import com.pdlpdl.pdlproxy.minecraft.api.SessionInterceptorControl;

import java.util.ArrayList;

public class SessionInterceptorControlImpl implements SessionInterceptorControl {

    private final Object lock = new Object();

    private final ArrayList<SessionInterceptor> store = new ArrayList<>();

    @Override
    public int getInterceptorCount() {
        synchronized (this.lock) {
            return this.store.size();
        }
    }

    @Override
    public SessionInterceptor getInterceptor(int offset) {
        synchronized (this.lock) {
            return this.store.get(offset);
        }
    }

    @Override
    public Iterable<SessionInterceptor> getInterceptorIterable() {
        synchronized (this.lock) {
            // Copy the list for concurrency safety.
            return new ArrayList<>(this.store);
        }
    }

    @Override
    public void removeInterceptor(int offset) {
        synchronized (this.lock) {
            this.store.remove(offset);
        }
    }

    @Override
    public void insertInterceptorAfter(int offset, SessionInterceptor insertInterceptor) {
        synchronized (this.lock) {
            if (offset >= this.store.size()) {
                offset = this.store.size() - 1;
            }

            this.store.add(offset + 1, insertInterceptor);
        }
    }

    @Override
    public void addInterceptorAtEnd(SessionInterceptor addInterceptor) {
        synchronized (this.lock) {
            this.store.add(addInterceptor);
        }
    }
}
