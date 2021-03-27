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
import com.github.steveice10.packetlib.packet.Packet;
import com.pdlpdl.minecraft.packetlog.api.Clock;
import com.pdlpdl.minecraft.packetlog.clock.SystemNanoBasedClock;
import com.pdlpdl.minecraft.packetlog.io.ClockBasedPacketFileWriter;
import com.pdlpdl.minecraft.packetlog.log.PacketLogger;
import com.pdlpdl.pdlproxy.minecraft.api.PacketInterceptor;
import com.pdlpdl.pdlproxy.minecraft.api.ProxyPacketControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.BiFunction;
import java.util.zip.GZIPOutputStream;

public class PacketTracingInterceptor implements PacketInterceptor {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(PacketTracingInterceptor.class);

    private Logger log = DEFAULT_LOGGER;

    //
    // CONFIGURATION
    //
    private Clock clock = new SystemNanoBasedClock();
    private boolean useCompression = false;
    private BiFunction<Session, Boolean, String> traceFileNameFormatter =
            (session, useCompression) -> "trace." + session.getHost() + "." + session.getPort() + (useCompression ? ".gz" : "");


    //
    // RUNTIME STATE
    //
    private PacketLogger packetLogger;

//========================================
// Getters and Setters
//----------------------------------------

    public Clock getClock() {
        return clock;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public BiFunction<Session, Boolean, String> getTraceFileNameFormatter() {
        return traceFileNameFormatter;
    }

    /**
     * Configure a formatter that takes a Session and returns the path to the trace file to create.  The default
     * formats the filename "trace." + REMOTE_HOSTNAME + "." + REMOTE_PORT.
     *
     * @param traceFileNameFormatter
     */
    public void setTraceFileNameFormatter(BiFunction<Session, Boolean, String> traceFileNameFormatter) {
        this.traceFileNameFormatter = traceFileNameFormatter;
    }

    public boolean isUseCompression() {
        return useCompression;
    }

    /**
     * Specify whether to GZIP compress the output or not.  Note that compression adds buffering that may lead to
     * undesirable results on abnormal shutdown or attempts to read the file while it is still being written.
     *
     * @param useCompression true => GZIP compress the output; false => do not compress the output.
     */
    public void setUseCompression(boolean useCompression) {
        this.useCompression = useCompression;
    }

//========================================
// Interceptor Operations
//----------------------------------------

    @Override
    public void onInterceptorInstalled(Session session) {
        try {
            String path = this.traceFileNameFormatter.apply(session, this.useCompression);

            if (path != null) {
                this.log.debug("Starting session tracing to file: remote-host={}; remote-port={}; use-compression={}; trace-file={}",
                        session.getHost(), session.getPort(), this.useCompression, path);

                //
                // Prepare the packet logger that will be use to write to the file.
                //
                this.preparePacketLogger(path);
            } else {
                this.log.debug("Skipping session tracing to file: remote-host={}; remote-port={}; use-compression={}",
                        session.getHost(), session.getPort(), this.useCompression);
            }
        } catch (IOException ioExc) {
            this.log.error("Failed to open tracing file for new session; tracing aborted", ioExc);
        }
    }

    @Override
    public void onInterceptorRemoved(Session session) {
        this.shutdown();
    }

    @Override
    public void onClientPacketReceived(Packet clientPacket, ProxyPacketControl proxyPacketControl) {
    }

    @Override
    public void onServerPacketReceived(Packet serverPacket, ProxyPacketControl proxyPacketControl) {
    }

    @Override
    public void onPacketSentToClient(Packet clientBoundPacket) {
        //
        // Log "sent" packets so that all packets are captured  - including those added by other interceptors.
        //
        if (this.packetLogger != null) {
            this.packetLogger.logPacket(clientBoundPacket);
        }
    }

    @Override
    public void onPacketSentToServer(Packet serverBoundPacket) {
        //
        // Log "sent" packets so that all packets are captured  - including those added by other interceptors.
        //
        if (this.packetLogger != null) {
            this.packetLogger.logPacket(serverBoundPacket);
        }
    }

    public void shutdown() {
        if (this.packetLogger != null) {
            try {
                this.packetLogger.close();
            } catch (IOException ioExc) {
                ioExc.printStackTrace();
            }
        }
    }

//========================================
// Internals
//----------------------------------------

    /**
     * Prepare the Packet Logger that will write the packets to file.
     *
     * @param path location of the file to which the logger will write the packets.
     * @throws IOException
     */
    private void preparePacketLogger(String path) throws IOException {
        File tracingFile = new File(path);
        OutputStream outputStream = new FileOutputStream(tracingFile);

        //
        // Wrap the output in a GZIP Output Stream if compression is enabled.
        //
        if (this.useCompression) {
            outputStream = new GZIPOutputStream(outputStream);
        }

        //
        // Prepare the logger using the Output Stream.
        //
        ClockBasedPacketFileWriter writer = new ClockBasedPacketFileWriter(clock, outputStream);
        this.packetLogger = new PacketLogger(writer);

        this.packetLogger.init();
    }
}
