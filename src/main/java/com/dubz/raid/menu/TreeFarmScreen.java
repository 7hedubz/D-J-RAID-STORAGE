package com.dubz.raid.menu;

import com.dubz.raid.blockentity.TreeFarmBlockEntity;
import com.dubz.raid.network.ModPayloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class TreeFarmScreen extends AbstractContainerScreen<TreeFarmMenu> {
    private Button toggle;

    public TreeFarmScreen(TreeFarmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TreeFarmMenu.WIDTH, TreeFarmMenu.HEIGHT);
        this.inventoryLabelY = 90;
    }

    @Override
    protected void init() {
        super.init();
        toggle = addRenderableWidget(Button.builder(toggleLabel(), button ->
                ClientPacketDistributor.sendToServer(new ModPayloads.ToggleTreeFarmPayload())
        ).bounds(leftPos + TreeFarmMenu.BUTTON_X, topPos + TreeFarmMenu.BUTTON_Y, 48, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.raid.swap_sides"), button ->
                ClientPacketDistributor.sendToServer(new ModPayloads.FlipSidesPayload())
        ).bounds(leftPos + SideSwapGui.x(imageWidth), topPos + SideSwapGui.Y, SideSwapGui.W, SideSwapGui.H).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x0 = leftPos;
        int y0 = topPos;
        graphics.fill(x0, y0, x0 + imageWidth, y0 + imageHeight, 0xFF1A1A1A);
        graphics.fill(x0 + 1, y0 + 1, x0 + imageWidth - 1, y0 + imageHeight - 1, 0xFF2F2F2F);
        int sx = leftPos + TreeFarmMenu.SLOT_X;
        int sy = topPos + TreeFarmMenu.SLOT_Y;
        graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF000000);
        graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF141414);
        int bx = leftPos + TreeFarmMenu.BAR_X;
        int by = topPos + TreeFarmMenu.BAR_Y;
        int bw = TreeFarmMenu.BAR_W;
        int bh = TreeFarmMenu.BAR_H;
        graphics.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, 0xFF000000);
        graphics.fill(bx, by, bx + bw, by + bh, 0xFF141414);
        int remaining = Math.max(0, TreeFarmBlockEntity.CYCLE - menu.progress());
        int filled = menu.running() ? (int) (bw * (1.0 - remaining / (double) TreeFarmBlockEntity.CYCLE)) : 0;
        if (filled > 0) {
            graphics.fill(bx, by, bx + filled, by + bh, 0xFF3D8C40);
        }
        int sec = remaining / 20;
        String clock = (sec / 60) + ":" + String.format("%02d", sec % 60);
        graphics.text(font, clock, bx + (bw - font.width(clock)) / 2, by + 2, 0xFFFFFFFF, true);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                well(graphics, leftPos + TreeFarmMenu.INV_X + col * 18, topPos + TreeFarmMenu.INV_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            well(graphics, leftPos + TreeFarmMenu.INV_X + col * 18, topPos + TreeFarmMenu.HOTBAR_Y);
        }
        if (toggle != null) {
            toggle.setMessage(toggleLabel());
        }
    }

    private Component toggleLabel() {
        return Component.translatable(menu.running() ? "gui.raid.auto_crafter.toggle_on" : "gui.raid.auto_crafter.toggle_off");
    }

    private static void well(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF000000);
        graphics.fill(x, y, x + 16, y + 16, 0xFF141414);
    }
}
