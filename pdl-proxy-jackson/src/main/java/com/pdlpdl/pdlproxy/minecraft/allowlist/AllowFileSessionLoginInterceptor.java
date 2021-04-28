package com.pdlpdl.pdlproxy.minecraft.allowlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.packet.login.server.LoginSuccessPacket;
import com.github.steveice10.packetlib.event.session.PacketSendingEvent;
import com.pdlpdl.pdlproxy.minecraft.allowlist.api.AllowListControl;
import com.pdlpdl.pdlproxy.minecraft.allowlist.model.AllowListEntry;
import com.pdlpdl.pdlproxy.minecraft.allowlist.model.AllowListFile;
import com.pdlpdl.pdlproxy.minecraft.api.SessionLoginInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

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

    public void grant(AllowListEntry newEntry) {
        this.checkLoad();

        synchronized (this.lock) {
            if (this.allowListEntries.add(newEntry)) {
                // New entry, save now.
                this.save();
            }
        }
    }

    public void revoke(AllowListEntry revokeEntry) {
        this.checkLoad();

        synchronized (this.lock) {
            if (this.allowListEntries.remove(revokeEntry)) {
                this.save();
            }
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
            this.log.info("ALLOW LIST disabled; allowing {}", playerName);
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
            packetSendingEvent.getSession().disconnect("Player is not on the allow-list");
        }
    }

//========================================
//
//----------------------------------------

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

            if (allowListFile.getAllowListEntries() != null) {
                for (AllowListEntry oneEntry : allowListFile.getAllowListEntries()) {
                    this.log.debug("Loaded allow list entry: {}", oneEntry);
                    this.allowListEntries.add(oneEntry);
                }
            }

            this.loaded = true;
        } catch (IOException ioExc) {
            this.log.error("FAILED to load ALLOW LIST", ioExc);
        }
    }

    // TODO: consider asynchronous saving to allow in-memory operation to continue without delay.  Note this is not
    // TODO: highly critical as changes to the allow-list are not expected to be frequent.
    private void save() {
        File updateFile = new File(this.allowFile.getPath() + ".upd");
        this.updateNumber++;

        this.log.info("Writing allow list; update-number={}", this.updateNumber);

        new AllowListFile(this.allowListEntries, this.updateNumber);

        try {
            // 2-step update.  Write to the ".upd" version of the file, then rename.
            this.objectMapper.writeValue(updateFile, this.allowListEntries);
            if (! updateFile.renameTo(this.allowFile)) {
                this.log.error("FAILED to rename .upd file to update ALLOW LIST; updates will be effective until restart");
            }
        } catch (IOException ioException) {
            this.log.error("FAILED to write ALLOW LIST; updates will be effective until restart", ioException);
        }
    }
}
