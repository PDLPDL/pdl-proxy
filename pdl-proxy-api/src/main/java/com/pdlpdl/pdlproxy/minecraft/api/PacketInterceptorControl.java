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

public interface PacketInterceptorControl {
    /**
     *
     * @return
     */
    int getInterceptorCount();
    PacketInterceptor getInterceptor(int offset);

    Iterable<PacketInterceptor> getInterceptorIterable();

    void removeInterceptor(int offset);
    void removeInterceptor(PacketInterceptor packetInterceptor);

    /**
     * Add the given interceptor before the existing interceptor at the same position, shifting the existing (and
     *  subsequent ones) to the right one position.
     *
     * @param offset the position in the list of interceptors at which to insert the new interceptor; 0 is the first position.
     * @param insertInterceptor the interceptor to insert.
     */
    void insertInterceptorBefore(int offset, PacketInterceptor insertInterceptor);

    /**
     * Add the given interceptor after the existing interceptor at the same position.  Same result as
     *  insertInterceptorBefore(offset + 1, insertInterceptor).
     *
     * @param offset the position in the list of interceptors after which to insert the new interceptor; 0 is the first position.
     * @param insertInterceptor
     */
    void insertInterceptorAfter(int offset, PacketInterceptor insertInterceptor);
    void addInterceptorAtEnd(PacketInterceptor insertInterceptor);
}
