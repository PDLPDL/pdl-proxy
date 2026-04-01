package com.pdlpdl.pdlproxy.minecraft.gamestate.datagen.model;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedList;

public class DatagenDimensionList extends LinkedList<String> {

    public DatagenDimensionList() {
    }

    public DatagenDimensionList(@NotNull Collection<? extends String> collection) {
        super(collection);
    }
}
