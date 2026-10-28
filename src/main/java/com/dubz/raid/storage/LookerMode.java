package com.dubz.raid.storage;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.Locale;

public enum LookerMode implements StringRepresentable {
    STANDBY,
    DETECT,
    DETECT_B,
    PULSE,
    PULSE_B,
    ERROR;

    public static final EnumProperty<LookerMode> PROPERTY = EnumProperty.create("mode", LookerMode.class);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
