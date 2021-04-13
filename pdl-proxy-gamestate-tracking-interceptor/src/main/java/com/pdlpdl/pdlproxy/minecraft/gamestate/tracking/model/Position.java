package com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model;

import com.artnaseef.immutable.utils.MutationUtilsImmutableProperties;

@MutationUtilsImmutableProperties(properties = {"x", "y", "z"})
public class Position {
    private final double x;
    private final double y;
    private final double z;

    public Position(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    @Override
    public String toString() {
        return "Position{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}
