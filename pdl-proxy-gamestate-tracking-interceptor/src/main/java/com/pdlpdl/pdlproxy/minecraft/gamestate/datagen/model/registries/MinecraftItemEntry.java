package com.pdlpdl.pdlproxy.minecraft.gamestate.datagen.model.registries;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MinecraftItemEntry {
    @JsonProperty(value = "protocol_id")
    private int protocolId;

    public int getProtocolId() {
        return protocolId;
    }

    public void setProtocolId(int protocolId) {
        this.protocolId = protocolId;
    }
}
