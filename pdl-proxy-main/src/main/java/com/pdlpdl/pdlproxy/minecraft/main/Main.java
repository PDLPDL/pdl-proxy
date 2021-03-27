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

package com.pdlpdl.pdlproxy.minecraft.main;

import com.github.steveice10.packetlib.Session;
import com.pdlpdl.pdlproxy.minecraft.api.SessionInterceptorControl;
import com.pdlpdl.pdlproxy.minecraft.impl.ProxyServerImpl;
import com.pdlpdl.pdlproxy.minecraft.main.poc.ProofOfConceptProxyServerImplConfigurer;
import com.pdlpdl.pdlproxy.minecraft.tracing.interceptor.PacketTracingSessionInterceptor;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.function.Consumer;

public class Main {
    public static final String DEFAULT_LISTEN_HOST = "0.0.0.0";
    public static final int DEFAULT_LIST_PORT = 7777;

    public static final String DEFAULT_FORWARD_HOST = "localhost";
    public static final int DEFAULT_FORWARD_PORT = 25565;

    public static final String DEFAULT_TRACING_DIR = "./trace.d";

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(Main.class);

    private Logger log = DEFAULT_LOGGER;

    private String listenHost = DEFAULT_LISTEN_HOST;
    private int listenPort = DEFAULT_LIST_PORT;
    private String forwardHost = DEFAULT_FORWARD_HOST;
    private int forwardPort = DEFAULT_FORWARD_PORT;

    private boolean enableTracing = false;
    private String tracingDir = DEFAULT_TRACING_DIR;
    private boolean enableTraceCompression = false;

    private boolean enablePoc = false;

    public static void main(String[] args) {
        Main instance = new Main();
        instance.instanceMain(args);
    }

    public void instanceMain(String[] args) {
        this.parseCommandLineArguments(args);

        this.runProxy();
    }

//========================================
// Command-Line Arguments
//----------------------------------------

    private void parseCommandLineArguments(String[] args) {
        CommandLineParser commandLineParser = new DefaultParser();

        Options commandLineOptions = this.prepareCommandLineOptions();

        try {
            CommandLine commandLine = commandLineParser.parse(commandLineOptions, args);

            if (commandLine.hasOption("h")) {
                this.displayHelp(System.out, commandLineOptions);
                System.exit(0);
            }

            //
            // Host Addresses
            //
            if (commandLine.hasOption("F")) {
                this.splitHostAddressArg(
                        commandLine.getOptionValue("F"), (host) -> this.forwardHost = host, (port) -> this.forwardPort = port);
            }

            if (commandLine.hasOption("L")) {
                this.splitHostAddressArg(
                        commandLine.getOptionValue("L"), (host) -> this.listenHost = host, (port) -> this.listenPort = port);
            }

            //
            // Tracing
            //
            this.enableTracing = commandLine.hasOption("t");
            if (commandLine.hasOption("d")) {
                this.tracingDir = commandLine.getOptionValue("d");
            }
            this.enableTraceCompression = commandLine.hasOption("c");

            //
            // Proof-Of-Concept
            //
            this.enablePoc = commandLine.hasOption("P");

        } catch (ParseException parseExc) {
            this.displayHelp(System.out, commandLineOptions);
            this.log.error("Command-line argument failure", parseExc);
        }
    }

    private Options prepareCommandLineOptions() {
        Options result = new Options();

        result.addOption("t", "trace", false, "Enable tracing of packets to file");
        result.addOption("d", "trace-output-dir", true, "Output directory into which tracing files will be written (see --trace)");
        result.addOption("c", "compress", false, "Compress the tracing files (see --trace)");
        result.addOption("P", "POC", false, "Enable Proof-Of-Concept Code");
        result.addOption("L", "listen-address", true, "Address on which to listen for connections in [<host>][:<port>] format");
        result.addOption("F", "forward-address", true, "Address of server to which client connections are forwarded, in [<host>][:<port>] format");
        result.addOption("h", "help", false, "Display command-line help");

        return result;
    }

    private void displayHelp(PrintStream printStream, Options options) {
        HelpFormatter helpFormatter = new HelpFormatter();

        PrintWriter printWriter = new PrintWriter(printStream);
        helpFormatter.printHelp(printWriter, 120, "Main [options]", null, options, 4, 4, null);

        printWriter.flush();
    }

    private void splitHostAddressArg(String arg, Consumer<String> hostConsumer, Consumer<Integer> portConsumer) {
        String[] parts = arg.split(":", 2);

        String hostPart = parts[0].trim();
        if (! hostPart.isEmpty()) {
            hostConsumer.accept(hostPart);
        }

        if (parts.length > 1) {
            String portPart = parts[1].trim();
            if (! portPart.isEmpty()) {
                portConsumer.accept(Integer.parseInt(portPart));
            }
        }
    }

//========================================
//
//----------------------------------------

    private void runProxy() {
        ProxyServerImpl proxyServer = new ProxyServerImpl();

        proxyServer.setServerListenHost(this.listenHost);
        proxyServer.setServerListenPort(this.listenPort);

        proxyServer.setDownstreamServerHostname(this.forwardHost);
        proxyServer.setDownstreamServerPort(this.forwardPort);

        if (this.enableTracing) {
            SessionInterceptorControl sessionInterceptorControl = proxyServer.getSessionInterceptorControl();
            installTracingInterceptor(sessionInterceptorControl);
        }

        //
        // POC Code Injection
        //
        if (this.enablePoc) {
            ProofOfConceptProxyServerImplConfigurer proofOfConceptProxyServerImplConfigurer =
                    new ProofOfConceptProxyServerImplConfigurer();

            proofOfConceptProxyServerImplConfigurer.configureProofsOfConcept(proxyServer);
        }

        proxyServer.start();
    }

    private void installTracingInterceptor(SessionInterceptorControl sessionInterceptorControl) {
        PacketTracingSessionInterceptor interceptor = new PacketTracingSessionInterceptor();

        interceptor.setPacketTracingInterceptorConfigurer(
                (packetTracingInterceptor -> {
                    packetTracingInterceptor.setUseCompression(this.enableTraceCompression);

                    packetTracingInterceptor.setTraceFileNameFormatter(
                            this::prepareTraceFilePath
                    );
                })
        );
        sessionInterceptorControl.insertInterceptorAfter(-1, interceptor);
    }

    private String prepareTraceFilePath(Session session, Boolean useCompression) {
        File traceDirectory = new File(this.tracingDir);

        try {
            boolean dirMade = traceDirectory.mkdirs();
            this.log.debug("Mkdirs for tracing directory result={}", dirMade);
        } catch (Exception exc) {
            this.log.info("Mkdirs for tracing directory failed; ignoring", exc);
        }

        String basename = "trace." + session.getHost() + "." + session.getPort();

        File traceFile = new File(traceDirectory, basename);

        // TODO: use numeric extensions to avoid overwriting existing files

        String result = traceFile.getPath();
        if (useCompression) {
            result += ".gz";
        }

        return result;
    }
}
