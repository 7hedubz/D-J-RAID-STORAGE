package com.dubz.raid.menu;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class SideSwapGui {
    public static final int W = 72;
    public static final int H = 16;
    public static final int Y = 6;
    public static final int LABEL_Y = 24;

    private SideSwapGui() {
    }

    public static int x(int imageWidth) {
        return imageWidth - 8 - W;
    }

    public static void drawLabel(GuiGraphicsExtractor graphics, Font font, int leftPos, int topPos, int imageWidth, boolean inOnLeft) {
        Component text = Component.translatable(inOnLeft ? "gui.raid.sides.in_out" : "gui.raid.sides.out_in");
        int tx = leftPos + x(imageWidth) + (W - font.width(text)) / 2;
        graphics.text(font, text, tx, topPos + LABEL_Y, 0xFFC0C0C0, false);
    }
}
