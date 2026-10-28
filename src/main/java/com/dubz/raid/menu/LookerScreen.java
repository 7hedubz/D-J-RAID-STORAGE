package com.dubz.raid.menu;

import com.dubz.raid.network.ModPayloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class LookerScreen extends AbstractContainerScreen<LookerMenu> {
    private Button toggle;

    public LookerScreen(LookerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, LookerMenu.WIDTH, LookerMenu.HEIGHT);
        this.inventoryLabelY = 126;
    }

    @Override
    protected void init() {
        super.init();
        toggle = addRenderableWidget(Button.builder(toggleLabel(), b ->
                ClientPacketDistributor.sendToServer(new ModPayloads.ToggleLookerPayload())
        ).bounds(leftPos + 8, topPos + 16, 48, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.raid.swap_sides"), b ->
                ClientPacketDistributor.sendToServer(new ModPayloads.FlipSidesPayload())
        ).bounds(leftPos + 60, topPos + 16, 72, 16).build());
        addStepper(8, 46, 0);
        addStepper(8, 78, 1);
        addStepper(8, 110, 2);
    }

    private void addStepper(int x, int y, int field) {
        addRenderableWidget(Button.builder(Component.literal("-"), b -> send(field, -1))
                .bounds(leftPos + x, topPos + y, 16, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> send(field, 1))
                .bounds(leftPos + x + 18, topPos + y, 16, 16).build());
    }

    private void send(int field, int delta) {
        int current = menu.current();
        int threshold = menu.threshold();
        int stride = menu.stride();
        switch (field) {
            case 0 -> current = Math.max(0, current + delta);
            case 1 -> threshold = Math.max(1, threshold + delta);
            default -> stride = Math.max(1, stride + delta);
        }
        ClientPacketDistributor.sendToServer(new ModPayloads.SetLookerPayload(current, threshold, stride));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x0 = leftPos;
        int y0 = topPos;
        graphics.fill(x0, y0, x0 + imageWidth, y0 + imageHeight, 0xFF1A1A1A);
        graphics.fill(x0 + 1, y0 + 1, x0 + imageWidth - 1, y0 + imageHeight - 1, 0xFF2F2F2F);
        graphics.text(font, Component.translatable("gui.raid.looker.current"), leftPos + 8, topPos + 36, 0xFFFFFFFF, false);
        graphics.text(font, String.valueOf(menu.current()), leftPos + 46, topPos + 50, 0xFFFFFF00, false);
        graphics.text(font, Component.translatable("gui.raid.looker.threshold"), leftPos + 8, topPos + 68, 0xFFFFFFFF, false);
        graphics.text(font, String.valueOf(menu.threshold()), leftPos + 46, topPos + 82, 0xFFFFFF00, false);
        graphics.text(font, Component.translatable("gui.raid.looker.stride"), leftPos + 8, topPos + 100, 0xFFFFFFFF, false);
        graphics.text(font, String.valueOf(menu.stride()), leftPos + 46, topPos + 114, 0xFFFFFF00, false);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                well(graphics, leftPos + LookerMenu.INV_X + col * 18, topPos + LookerMenu.INV_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            well(graphics, leftPos + LookerMenu.INV_X + col * 18, topPos + LookerMenu.HOTBAR_Y);
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
