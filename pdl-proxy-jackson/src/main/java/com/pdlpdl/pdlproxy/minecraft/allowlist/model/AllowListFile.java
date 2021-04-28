package com.pdlpdl.pdlproxy.minecraft.allowlist.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class AllowListFile {
    private final List<AllowListEntry> allowListEntries;
    private final long updateNumber;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public AllowListFile(
            @JsonProperty("allowListEntries") Collection<AllowListEntry> allowListEntries,
            @JsonProperty("updateNumber") long updateNumber
    ) {

        if (allowListEntries == null) {
            this.allowListEntries = Collections.unmodifiableList(new LinkedList<>());
        } else {
            this.allowListEntries = Collections.unmodifiableList(new LinkedList<>(allowListEntries));
        }

        this.updateNumber = updateNumber;
    }

    public List<AllowListEntry> getAllowListEntries() {
        return allowListEntries;
    }

    public long getUpdateNumber() {
        return updateNumber;
    }
}
