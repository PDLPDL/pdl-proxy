package com.pdlpdl.pdlproxy.minecraft.tracing.util;

import java.net.SocketAddress;

public interface TracingFilenameFormatter {
    String formatTraceFileHostnamePort (String optPrefix, SocketAddress socketAddress, boolean useCompression);
}
