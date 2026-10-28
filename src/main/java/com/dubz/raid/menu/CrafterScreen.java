package com.dubz.raid.menu;

import com.dubz.raid.network.ModPayloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class CrafterScreen extends AbstractContainerScreen<CrafterMenu> {
    private Button toggle;

    public CrafterScreen(CrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, CrafterMenu.WIDTH, CrafterMenu.HEIGHT);
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        toggle = addRenderableWidget(Button.builder(toggleLabel(), button ->
                ClientPacketDistributor.sendToServer(new ModPayloads.ToggleCrafterPayload())
        ).bounds(leftPos + CrafterMenu.BUTTON_X, topPos + CrafterMenu.BUTTON_Y, CrafterMenu.BUTTON_W, CrafterMenu.BUTTON_H).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.raid.swap_sides"), button ->
                ClientPacketDistributor.sendToServer(new ModPayloads.FlipSidesPayload())
        ).bounds(leftPos + 8, topPos + CrafterMenu.BUTTON_Y, 72, 16).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x0 = leftPos;
        int y0 = topPos;
        graphics.fill(x0, y0, x0 + imageWidth, y0 + imageHeight, 0xFF1A1A1A);
        graphics.fill(x0 + 1, y0 + 1, x0 + imageWidth - 1, y0 + imageHeight - 1, 0xFF2F2F2F);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                well(graphics, leftPos + CrafterMenu.GRID_X + col * 18, topPos + CrafterMenu.GRID_Y + row * 18);
            }
        }
        well(graphics, leftPos + CrafterMenu.RESULT_X, topPos + CrafterMenu.RESULT_Y);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                well(graphics, leftPos + CrafterMenu.INV_X + col * 18, topPos + CrafterMenu.INV_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            well(graphics, leftPos + CrafterMenu.INV_X + col * 18, topPos + CrafterMenu.HOTBAR_Y);
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
