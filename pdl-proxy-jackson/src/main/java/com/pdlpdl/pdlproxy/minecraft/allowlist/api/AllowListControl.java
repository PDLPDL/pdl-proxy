package com.pdlpdl.pdlproxy.minecraft.allowlist.api;

import com.pdlpdl.pdlproxy.minecraft.allowlist.model.AllowListEntry;

public interface AllowListControl {
    /**
     * Grant access for the player indicated by the given entry.
     *
     * @param newEntry entry naming the player to allow access to the server.
     */
    void grant(AllowListEntry newEntry);

    /**
     * Revoke access from the player indicated by the given entry.
     *
     * @param revokeEntry entry naming the player to prevent from accessing the server.
     */
    void revoke(AllowListEntry revokeEntry);
}
