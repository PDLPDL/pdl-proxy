package com.pdlpdl.pdlproxy.minecraft.gamestate.datagen.model.registries;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class MinecraftItemRegistry {
    @JsonProperty(value = "default")
    private String defaultItem;

    @JsonProperty(value = "protocol_id")
    private int protocolId;

    private Map<String, MinecraftItemEntry> entries = new HashMap<>();

    public String getDefaultItem() {
        return defaultItem;
    }

    public void setDefaultItem(String defaultItem) {
        this.defaultItem = defaultItem;
    }

    public int getProtocolId() {
        return protocolId;
    }

    public void setProtocolId(int protocolId) {
        this.protocolId = protocolId;
    }

    public Map<String, MinecraftItemEntry> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, MinecraftItemEntry> entries) {
        this.entries = entries;
    }

    @JsonAnySetter
    public void addEntry(String name, MinecraftItemEntry minecraftItemEntry) {
        this.entries.put(name, minecraftItemEntry);
    }
}
