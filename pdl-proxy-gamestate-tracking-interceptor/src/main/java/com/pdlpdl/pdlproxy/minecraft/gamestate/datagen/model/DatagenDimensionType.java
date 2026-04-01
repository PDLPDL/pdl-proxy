package com.pdlpdl.pdlproxy.minecraft.gamestate.datagen.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class DatagenDimensionType {
    @JsonProperty("min_y")
    private int minY;

    @JsonProperty("height")
    private int height;

    @JsonProperty("logical_height")
    private int logicalHeight;

    @JsonAnySetter
    private Map<String, Object> additionalProperties;

//========================================
// Accessors
//----------------------------------------

    public int getMinY() {
        return minY;
    }

    public void setMinY(int minY) {
        this.minY = minY;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getLogicalHeight() {
        return logicalHeight;
    }

    public void setLogicalHeight(int logicalHeight) {
        this.logicalHeight = logicalHeight;
    }

    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

//========================================
// toString
//----------------------------------------

    @Override
    public String toString() {
        return "DatagenDimensionType{" +
            "minY=" + minY +
            ", height=" + height +
            ", logicalHeight=" + logicalHeight +
            ", additionalProperties=" + additionalProperties +
            '}';
    }
}
