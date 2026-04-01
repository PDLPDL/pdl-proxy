package com.pdlpdl.pdlproxy.minecraft.tracing.util;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class TracingFilenameFormatterImpl implements TracingFilenameFormatter {
    public String formatTraceFileHostnamePort (String optPrefix, SocketAddress socketAddress, boolean useCompression) {
        StringBuilder result = new StringBuilder();
        if (optPrefix != null) {
            result.append(optPrefix);
        }

        String addressPart;
        if (socketAddress instanceof InetSocketAddress) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
            addressPart = inetSocketAddress.getHostName() + "." + inetSocketAddress.getPort();
        } else {
            addressPart = socketAddress.toString();
        }
        result.append(addressPart);

        if (useCompression) {
            result.append(".gz");
        }

        return result.toString();
    }

}
