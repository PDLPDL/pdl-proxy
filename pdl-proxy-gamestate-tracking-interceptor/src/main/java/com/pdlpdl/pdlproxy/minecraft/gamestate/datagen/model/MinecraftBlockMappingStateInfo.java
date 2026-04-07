package com.pdlpdl.pdlproxy.minecraft.gamestate.datagen.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class MinecraftBlockMappingStateInfo {
    /**
     * Block ID for this state
     */
    private int id;

    /**
     * Name and Value pairs for properties that defines their values for this state id.
     */
    private Map<String, Object> properties;

    /**
     * Is this the default state for the block of this name?
     */
    @JsonProperty(value = "default", required = false, defaultValue = "false")
    private boolean defaultInd;

//========================================
// Getters and Setters
//========================================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public boolean getDefaultInd() {
        return defaultInd;
    }

    public void setDefaultInd(boolean defaultInd) {
        this.defaultInd = defaultInd;
    }
}
