package com.dubz.raid.item;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public final class RaidLore {
    public static final int COLOR = 0x240DBD;

    private RaidLore() {
    }

    public static Component line(String key, Object... args) {
        return Component.translatable(key, args).withStyle(Style.EMPTY.withColor(COLOR));
    }

    public static Component literal(String text) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(COLOR));
    }
}
