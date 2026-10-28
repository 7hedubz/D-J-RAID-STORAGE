package com.dubz.raid.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class HopperScreen extends AbstractContainerScreen<HopperMenu> {
    public HopperScreen(HopperMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, HopperMenu.WIDTH, HopperMenu.HEIGHT);
        this.inventoryLabelY = 39;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x0 = leftPos;
        int y0 = topPos;
        graphics.fill(x0, y0, x0 + imageWidth, y0 + imageHeight, 0xFF1A1A1A);
        graphics.fill(x0 + 1, y0 + 1, x0 + imageWidth - 1, y0 + imageHeight - 1, 0xFF2F2F2F);
        int sx = leftPos + HopperMenu.FILTER_X;
        int sy = topPos + HopperMenu.FILTER_Y;
        graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF000000);
        graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF141414);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = leftPos + HopperMenu.INV_X + col * 18;
                int y = topPos + HopperMenu.INV_Y + row * 18;
                graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF000000);
                graphics.fill(x, y, x + 16, y + 16, 0xFF141414);
            }
        }
        for (int col = 0; col < 9; col++) {
            int x = leftPos + HopperMenu.INV_X + col * 18;
            int y = topPos + HopperMenu.HOTBAR_Y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF000000);
            graphics.fill(x, y, x + 16, y + 16, 0xFF141414);
        }
    }
}
