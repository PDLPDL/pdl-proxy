package com.pdlpdl.pdlproxy.minecraft.gamestate.tracking.model;

import com.artnaseef.immutable.utils.MutationUtilsImmutableProperties;

@MutationUtilsImmutableProperties(properties = {"pitch", "yaw", "roll"})
public class Rotation {
    private final double pitch;
    private final double yaw;
    private final double roll;

    public Rotation(double pitch, double yaw, double roll) {
        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
    }

    public double getPitch() {
        return pitch;
    }

    public double getYaw() {
        return yaw;
    }

    public double getRoll() {
        return roll;
    }

    @Override
    public String toString() {
        return "Rotation{" +
                "pitch=" + pitch +
                ", yaw=" + yaw +
                ", roll=" + roll +
                '}';
    }
}
