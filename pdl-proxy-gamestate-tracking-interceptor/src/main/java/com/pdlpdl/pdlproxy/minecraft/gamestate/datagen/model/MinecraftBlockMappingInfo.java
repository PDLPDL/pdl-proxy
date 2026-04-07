package com.pdlpdl.pdlproxy.minecraft.gamestate.datagen.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MinecraftBlockMappingInfo {
    /**
     * Properties of the block and possible values.  For example, "snowy" for dirt block.
     */
    private Map<String, Object> properties;

    /**
     * List of block states.
     */
    private List<MinecraftBlockMappingStateInfo> states;

//========================================
// Getters and Setters
//========================================

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public List<MinecraftBlockMappingStateInfo> getStates() {
        return states;
    }

    public void setStates(List<MinecraftBlockMappingStateInfo> states) {
        this.states = states;
    }
}
