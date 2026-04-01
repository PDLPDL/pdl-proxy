package com.pdlpdl.pdlproxy.minecraft.impl;

import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Utility to wait for minecraft protocol state change.  Note that this may not be needed.  It was added when the
 *  lack of synchronization between the upstream-client-session state and the downstream-server-session state was a
 *  concern and there were unknowns.
 *
 * At this time, the protocol handling both upstream and downstream should be clean enough that this is not needed.
 *
 * In the worst case, this acts as a no-op, or perhaps slows down finding a bug in protocol handling.
 */
public class MinecraftProtocolStateWaiter {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MinecraftProtocolStateWaiter.class);

    private Logger log = DEFAULT_LOGGER;

    private final MinecraftProtocol minecraftProtocol;

    public MinecraftProtocolStateWaiter(MinecraftProtocol minecraftProtocol) {
        this.minecraftProtocol = minecraftProtocol;
    }

    /**
     *
     * @param targetStateSet
     * @param expiration amount of time before timeout, in milliseconds.
     */
    public void waitForOutboundStateWithTimeout(Set<ProtocolState> targetStateSet, long expiration) {
        this.log.debug("BLOCKING ON WAIT-FOR-STATE");

        long startTimestamp = System.nanoTime();
        long curTimestamp = startTimestamp;
        long expirationDeadlineNano = startTimestamp + (expiration * 1000000L);

        ProtocolState curState = this.minecraftProtocol.getOutboundState();
        boolean first = true;
        while ((! targetStateSet.contains(curState)) && (curTimestamp < expirationDeadlineNano)) {
            if (first) {
                this.log.debug("waiting for state change: target={}; cur={}", targetStateSet, curState);
                first = false;
            }
            try {
                Thread.sleep(10);

                curState = this.minecraftProtocol.getOutboundState();
                curTimestamp = System.nanoTime();
            } catch (InterruptedException intExc) {
                this.log.warn("Interrupted while waiting for outbound state to change to {}", targetStateSet, intExc);
                throw new RuntimeException("wait for outbound state change interrupted", intExc);
            }
        }

        if (! targetStateSet.contains(curState)) {
            this.log.error("Timeout waiting for Minecraft session to reach target state: target-state={}; cur-state={}", targetStateSet, curState);
            throw new RuntimeException("Timeout waiting for Minecraft session to reach target state: target-state=" + targetStateSet + "; cur-state=" + curState);
        }

        this.log.debug("Wait for state change successful: target-state={}; wait-duration={}ms", targetStateSet, (curTimestamp - startTimestamp) / 1000000L);
    }
}
