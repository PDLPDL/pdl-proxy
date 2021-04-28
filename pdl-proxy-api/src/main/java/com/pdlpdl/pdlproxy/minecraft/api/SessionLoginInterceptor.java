package com.pdlpdl.pdlproxy.minecraft.api;

import com.github.steveice10.packetlib.event.session.PacketSendingEvent;

public interface SessionLoginInterceptor {
    /**
     * Called in the packet pipeline for LoginSuccessPacket about to be sent by the proxy to the Player client.  Cancel
     * the packet and disconnect the session to abort the session outright.
     *
     * @param packetSendingEvent event for the LoginSuccessPacket.
     */
    void onPlayerLoginSuccessSending(PacketSendingEvent packetSendingEvent);
}
