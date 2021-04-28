package com.pdlpdl.pdlproxy.minecraft.allowlist.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class AllowListEntry implements Comparable {
    private final String playerName;

    @JsonCreator(mode= JsonCreator.Mode.PROPERTIES)
    public AllowListEntry(
            @JsonProperty("playerName") String playerName
    ) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof AllowListEntry) {
            return this.playerName.compareTo(((AllowListEntry) o).playerName);
        } else {
            return -1;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AllowListEntry that = (AllowListEntry) o;
        return playerName.equals(that.playerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerName);
    }

    @Override
    public String toString() {
        return "AllowListEntry{" +
                "playerName='" + playerName + '\'' +
                '}';
    }
}
