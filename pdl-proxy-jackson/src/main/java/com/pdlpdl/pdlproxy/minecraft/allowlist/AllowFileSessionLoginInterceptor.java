package com.pdlpdl.pdlproxy.minecraft.allowlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.packet.login.server.LoginDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.login.server.LoginSuccessPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.PacketSendingEvent;
import com.github.steveice10.packetlib.event.session.PacketSentEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.pdlpdl.pdlproxy.minecraft.allowlist.api.AllowListControl;
import com.pdlpdl.pdlproxy.minecraft.allowlist.model.AllowListEntry;
import com.pdlpdl.pdlproxy.minecraft.allowlist.model.AllowListFile;
import com.pdlpdl.pdlproxy.minecraft.api.SessionLoginInterceptor;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Simple file-based implementation of a SessionLoginInterceptor, reading and writing allow entries to a json-formatted
 * file.
 *
 * To use (incomplete snippet here):
 *
 *      ProxyServer proxyServer;
 *      File allowFile;
 *
 *      AllowFileSessionLoginInterceptor allowFileSessionLoginInterceptor =
 *          new AllowFileSessionLoginInterceptor(allowFile);
 *
 *      proxyServer.addSessionLoginInterceptor(allowFileSessionLoginInterceptor);
 */
public class AllowFileSessionLoginInterceptor implements AllowListControl, SessionLoginInterceptor {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(AllowFileSessionLoginInterceptor.class);

    private Logger log = DEFAULT_LOGGER;

    private final Object lock = new Object();
    private final File allowFile;

    private ObjectMapper objectMapper;

    /**
     * When disabled, all logins are allowed.
     */
    private boolean enabled = true;

    private boolean loaded = false;
    // TODO: use UUID instead?  Allow both?  UUID can be problematic if ever running offline.
    private Set<AllowListEntry> allowListEntries = new TreeSet<>();
    private long updateNumber = 0;

//========================================
// Constructor
//----------------------------------------

    public AllowFileSessionLoginInterceptor(File allowFile) {
        this.allowFile = allowFile;

        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

//========================================
// Getters, Setters, and Grant/Revoke
//----------------------------------------

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public void grant(AllowListEntry newEntry) {
        this.checkLoad();

        synchronized (this.lock) {
            if (this.allowListEntries.add(newEntry)) {
                // New entry, save now.
                this.save();
            }
        }
    }

    @Override
    public void revoke(AllowListEntry revokeEntry) {
        this.checkLoad();

        synchronized (this.lock) {
            if (this.allowListEntries.remove(revokeEntry)) {
                this.save();
            }
        }
    }

    @Override
    public List<AllowListEntry> getEntries() {
        this.checkLoad();

        synchronized (this.lock) {
            return new LinkedList<>(this.allowListEntries);
        }
    }

//========================================
// Login Interceptor
//----------------------------------------

    @Override
    public void onPlayerLoginSuccessSending(PacketSendingEvent packetSendingEvent) {
        //
        // Get the player info
        //
        LoginSuccessPacket loginSuccessPacket = packetSendingEvent.getPacket();
        GameProfile gameProfile = loginSuccessPacket.getProfile();
        String playerName = gameProfile.getName();

        //
        // If Disabled, just skip
        //
        if (! this.enabled) {
            this.log.info("ALLOW LIST disabled; allowing session for player: player-name={}", playerName);
            return;
        }

        //
        // Load the allow list if it hasn't been loaded previously.
        //
        this.checkLoad();

        //
        // Check if the player is on the list.
        //
        AllowListEntry checkEntry = new AllowListEntry(playerName);
        if (! this.allowListEntries.contains(checkEntry)) {
            this.log.info("REJECTED login from player: player-name={}", playerName);
            this.log.debug("ALLOW LIST = {}", this.allowListEntries);

            packetSendingEvent.setCancelled(true);

            this.disconnectAndSendReason(packetSendingEvent.getSession(), "Player is not on the server allow list: player-name=" + playerName);
        }
    }

//========================================
//
//----------------------------------------

    /**
     * Disconnect the session after sending a reason to the client.  Note that the session.disconnect() method must not
     * be called until the reason packet has had a chance to be sent; otherwise, it won't make it.
     *
     * @param session
     * @param reason
     */
    private void disconnectAndSendReason(Session session, String reason) {
        Component textComponent = Component.text(reason);

        // Prepare the disconnect reason for the client.
        LoginDisconnectPacket loginDisconnectPacket = new LoginDisconnectPacket(textComponent);

        CountDownLatch finishDisconnectLatch = new CountDownLatch(1);

        // Listen to the session for the disconnect packet being delivered.  Defer the final session.disconnect() until
        //  this is observed to minimize the chance the client will not get the disconnect reason.
        session.addListener(new SessionAdapter() {
            @Override
            public void packetSent(PacketSentEvent event) {
                if (event.getPacket() == loginDisconnectPacket) {
                    finishDisconnectLatch.countDown();
                }
            }

            @Override
            public void disconnected(DisconnectedEvent event) {
                // No reason left to wait
                finishDisconnectLatch.countDown();
            }
        });

        // Send the disconnect packet now.
        session.send(loginDisconnectPacket);

        // Start the thread that finishes the disconnect on the session.
        Thread finishDisconnectThread =
                new Thread(() -> this.finishDisconnectOnLatch(finishDisconnectLatch, session, reason));

        finishDisconnectThread.start();
    }

    private void finishDisconnectOnLatch(CountDownLatch latch, Session session, String reason) {
        try {
            latch.await(3000, TimeUnit.MILLISECONDS);
        } catch (Exception exc) {
            this.log.info("Exception on wait to finish disconnect", exc);
        }

        // Finalize the disconnect.
        session.disconnect(reason);
    }

    private void checkLoad() {
        synchronized (this.lock) {
            if (! this.loaded) {
                this.load();
            }
        }
    }

    private void load() {
        try {
            AllowListFile allowListFile = this.objectMapper.readValue(this.allowFile, AllowListFile.class);

            this.updateNumber = allowListFile.getUpdateNumber();
            this.log.info("Loading allow list: update-number={}", this.updateNumber);

            int count = 0;

            if (allowListFile.getAllowListEntries() != null) {
                for (AllowListEntry oneEntry : allowListFile.getAllowListEntries()) {
                    this.log.debug("Loaded allow list entry: {}", oneEntry);
                    this.allowListEntries.add(oneEntry);
                    count++;
                }
            }

            this.log.info("ALLOW LIST: finished loading entries: loaded-count={}; total-count={}",
                    count, this.allowListEntries.size());

            this.loaded = true;
        } catch (IOException ioExc) {
            this.log.error("FAILED to load ALLOW LIST", ioExc);
        }
    }

    // TODO: consider asynchronous saving to allow in-memory operation to continue without delay.  Note this is not
    // TODO: highly critical as changes to the allow-list are not expected to be frequent.
    private void save() {
        File updateFile = new File(this.allowFile.getPath() + ".upd");

        AllowListFile allowListFile;
        synchronized (this.lock) {
            this.updateNumber++;
            allowListFile = new AllowListFile(this.allowListEntries, this.updateNumber);
        }

        this.log.info("Writing allow list: update-number={}; count={}",
                allowListFile.getUpdateNumber(),
                allowListFile.getAllowListEntries().size());

        try {
            // 2-step update.  Write to the ".upd" version of the file, then rename.
            this.objectMapper.writeValue(updateFile, allowListFile);
            if (! updateFile.renameTo(this.allowFile)) {
                this.log.error("FAILED to rename .upd file to update ALLOW LIST; updates will be effective until restart");
            }
        } catch (IOException ioException) {
            this.log.error("FAILED to write ALLOW LIST; updates will be effective until restart", ioException);
        }
    }
}
