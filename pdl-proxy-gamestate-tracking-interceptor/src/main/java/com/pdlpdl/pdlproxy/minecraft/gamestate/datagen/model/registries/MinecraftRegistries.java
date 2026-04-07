package com.pdlpdl.pdlproxy.minecraft.gamestate.datagen.model.registries;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Note there are many more registries in the file than listed here.  Add them as-needed.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MinecraftRegistries {
    @JsonProperty(value = "minecraft:item")
    private MinecraftItemRegistry items;

    public MinecraftItemRegistry getItems() {
        return items;
    }

    public void setItems(MinecraftItemRegistry items) {
        this.items = items;
    }
}
