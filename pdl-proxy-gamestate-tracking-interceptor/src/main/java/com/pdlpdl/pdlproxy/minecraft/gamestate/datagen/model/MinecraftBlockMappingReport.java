package com.pdlpdl.pdlproxy.minecraft.gamestate.datagen.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MinecraftBlockMappingReport {
    private Map<String, MinecraftBlockMappingInfo> blockMap = new HashMap<>();

//========================================
// Getters and Setters
//========================================

    public Map<String, MinecraftBlockMappingInfo> getBlockMap() {
        return blockMap;
    }

    public void setBlockMap(Map<String, MinecraftBlockMappingInfo> blockMap) {
        this.blockMap = blockMap;
    }

    @JsonAnySetter
    public void addMapping(String name, MinecraftBlockMappingInfo minecraftBlockMappingInfo) {
        this.blockMap.put(name, minecraftBlockMappingInfo);
    }
}
