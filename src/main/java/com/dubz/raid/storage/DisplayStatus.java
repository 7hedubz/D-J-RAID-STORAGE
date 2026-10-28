package com.dubz.raid.storage;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.Locale;

public enum DisplayStatus implements StringRepresentable {
    OK,
    DISCONNECTED,
    ERROR;

    public static final EnumProperty<DisplayStatus> PROPERTY = EnumProperty.create("status", DisplayStatus.class);

    public static DisplayStatus from(NetworkStatus status) {
        return switch (status) {
            case ERROR -> ERROR;
            case DISCONNECTED -> DISCONNECTED;
            default -> OK;
        };
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
