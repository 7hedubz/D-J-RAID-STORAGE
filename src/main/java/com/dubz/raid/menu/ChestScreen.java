package com.dubz.raid.menu;

import com.dubz.raid.network.ModPayloads;
import com.dubz.raid.storage.DigitizedStorage;
import com.dubz.raid.storage.NetworkStatus;
import com.dubz.raid.storage.RaidMath;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;

public class ChestScreen extends AbstractContainerScreen<ChestMenu> {
    private EditBox search;
    private int scroll;
    private boolean draggingScroll;

    public ChestScreen(ChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ChestGuiLayout.WIDTH, ChestGuiLayout.HEIGHT);
        this.inventoryLabelY = ChestGuiLayout.INV_LABEL_Y;
        this.titleLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font,
                leftPos + ChestGuiLayout.SEARCH_X,
                topPos + ChestGuiLayout.SEARCH_Y,
                ChestGuiLayout.SEARCH_W,
                ChestGuiLayout.SEARCH_H,
                Component.translatable("gui.raid.search"));
        search.setBordered(true);
        search.setMaxLength(40);
        search.setHint(Component.translatable("gui.raid.search"));
        search.setTextColor(0xFFE0E0E0);
        search.setResponder(value -> {
            menu.setFilter(value);
            scroll = 0;
        });
        addRenderableWidget(search);
        scroll = 0;
        Minecraft minecraft = this.minecraft;
        if (minecraft != null && minecraft.level != null && minecraft.player != null) {
            CreativeModeTabs.tryRebuildTabContents(
                    minecraft.level.enabledFeatures(),
                    minecraft.player.hasInfiniteMaterials(),
                    minecraft.level.registryAccess());
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x0 = leftPos;
        int y0 = topPos;
        int x1 = leftPos + imageWidth;
        int y1 = topPos + imageHeight;
        graphics.fill(x0, y0, x1, y1, 0xFF1A1A1A);
        graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xFF2F2F2F);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotWell(graphics,
                        leftPos + ChestGuiLayout.INV_X + col * 18,
                        topPos + ChestGuiLayout.INV_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotWell(graphics,
                    leftPos + ChestGuiLayout.INV_X + col * 18,
                    topPos + ChestGuiLayout.HOTBAR_Y);
        }
    }

    private static void drawSlotWell(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF000000);
        graphics.fill(x, y, x + 16, y + 16, 0xFF141414);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFC0C0C0, false);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        drawStatus(graphics);
        drawCapacityBar(graphics);
        renderGrid(graphics, mouseX, mouseY);
        drawScrollbar(graphics);
    }

    private void drawStatus(GuiGraphicsExtractor graphics) {
        Component text = switch (menu.status()) {
            case ERROR -> Component.translatable("gui.raid.error");
            case DISCONNECTED -> Component.translatable("gui.raid.disconnected");
            case LOCKED -> Component.translatable("gui.raid.locked");
            case OK -> {
                long remaining = menu.capacity() - menu.storedBytes();
                yield menu.capacity() > 0 && remaining < 16
                        ? Component.translatable("gui.raid.low_space")
                        : null;
            }
        };
        if (text == null) {
            return;
        }
        int x = leftPos + (imageWidth - font.width(text)) / 2;
        graphics.text(font, text, x, topPos + ChestGuiLayout.STATUS_Y, 0xFFFF5555, true);
    }

    private void drawCapacityBar(GuiGraphicsExtractor graphics) {
        long used = menu.storedBytes();
        long capacity = menu.capacity();
        boolean full = capacity <= 0 ? used > 0 : used >= capacity;
        String bytes = RaidMath.formatUsedCapacity(used, capacity);
        int textColor = full ? 0xFFFF5555 : 0xFFFFFFFF;

        int barX = leftPos + ChestGuiLayout.BAR_X;
        int barY = topPos + ChestGuiLayout.BAR_Y;
        int barW = ChestGuiLayout.BAR_W;
        int barH = ChestGuiLayout.BAR_H;
        graphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF000000);
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF1A1A1A);
        int inner = barW;
        int filled = 0;
        if (capacity > 0 && used >= capacity) {
            filled = inner;
        } else if (capacity > 0) {
            filled = (int) Math.min(inner, used * inner / capacity);
            if (used > 0 && filled < 1) {
                filled = 1;
            }
        } else if (used > 0) {
            filled = inner;
        }
        if (filled > 0) {
            int fillColor = full ? 0xFFCC3333 : 0xFF3D9B6A;
            graphics.fill(barX, barY, barX + filled, barY + barH, fillColor);
        }
        int textX = barX + (barW - font.width(bytes)) / 2;
        int textY = barY + (barH - 8) / 2;
        graphics.text(font, bytes, textX, textY, textColor, true);
    }

    private int contentHeight(List<RaidItemGroups.Row> rows) {
        int height = 0;
        for (RaidItemGroups.Row row : rows) {
            height += row.height();
        }
        return height;
    }

    private int maxScroll() {
        int extra = contentHeight(RaidItemGroups.rows(menu.visibleEntries())) - ChestGuiLayout.GRID_H;
        return Math.max(0, extra);
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics) {
        int x = leftPos + ChestGuiLayout.SCROLL_X;
        int y = topPos + ChestGuiLayout.SCROLL_Y;
        int w = ChestGuiLayout.SCROLL_W;
        int h = ChestGuiLayout.SCROLL_H;
        graphics.fill(x, y, x + w, y + h, 0xFF101010);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF2A2A2A);
        int max = maxScroll();
        int thumbH = max <= 0 ? h - 2 : Math.max(16, h * h / (h + max));
        int thumbY = y + 1;
        if (max > 0) {
            thumbY = y + 1 + (h - 2 - thumbH) * scroll / max;
        }
        graphics.fill(x + 2, thumbY, x + w - 2, thumbY + thumbH, max <= 0 ? 0xFF3A3A3A : 0xFF8A8A8A);
    }

    private boolean overScrollbar(double mouseX, double mouseY) {
        int x = leftPos + ChestGuiLayout.SCROLL_X;
        int y = topPos + ChestGuiLayout.SCROLL_Y;
        return mouseX >= x && mouseX < x + ChestGuiLayout.SCROLL_W
                && mouseY >= y && mouseY < y + ChestGuiLayout.SCROLL_H;
    }

    private void scrollToMouse(double mouseY) {
        int max = maxScroll();
        if (max <= 0) {
            scroll = 0;
            return;
        }
        int y = topPos + ChestGuiLayout.SCROLL_Y;
        int h = ChestGuiLayout.SCROLL_H;
        double t = (mouseY - y) / h;
        scroll = MthClamp((int) Math.round(t * max), 0, max);
    }

    private static int MthClamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        DigitizedStorage.Entry hovered = hoveredEntry(mouseX, mouseY);
        if (hovered != null) {
            ItemStack stack = hovered.stack.copy();
            stack.setCount(1);
            graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
        }
    }

    private void renderGrid(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (menu.status() == NetworkStatus.ERROR || menu.status() == NetworkStatus.DISCONNECTED) {
            return;
        }
        List<RaidItemGroups.Row> rows = RaidItemGroups.rows(menu.visibleEntries());
        scroll = Math.min(scroll, maxScroll());
        DigitizedStorage.Entry hovered = hoveredEntry(mouseX, mouseY);
        int gridTop = topPos + ChestGuiLayout.GRID_Y;
        int gridBottom = gridTop + ChestGuiLayout.GRID_H;
        int y = gridTop - scroll;
        for (RaidItemGroups.Row row : rows) {
            int height = row.height();
            int rowBottom = y + height;
            if (rowBottom > gridTop && y < gridBottom) {
                if (row.isHeader()) {
                    int barY = Math.max(y, gridTop);
                    int barBottom = Math.min(rowBottom, gridBottom);
                    graphics.fill(leftPos + ChestGuiLayout.GRID_X - 1, barY,
                            leftPos + ChestGuiLayout.GRID_X + ChestGuiLayout.BAR_W + 1, barBottom, 0xFF1A1520);
                    if (y >= gridTop && rowBottom <= gridBottom) {
                        graphics.text(font, row.header(), leftPos + ChestGuiLayout.GRID_X, y + 2, 0xFFFFC070, false);
                    }
                } else if (y >= gridTop && rowBottom <= gridBottom) {
                    for (int col = 0; col < row.items().size(); col++) {
                        int x = leftPos + ChestGuiLayout.GRID_X + col * 18;
                        drawSlotWell(graphics, x, y);
                        DigitizedStorage.Entry entry = row.items().get(col);
                        ItemStack stack = entry.stack.copy();
                        stack.setCount(1);
                        graphics.item(stack, x, y);
                        graphics.itemDecorations(font, stack, x, y, formatCount(entry.count));
                        if (hovered != null && ItemStack.isSameItemSameComponents(entry.stack, hovered.stack)) {
                            graphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
                        }
                    }
                }
            }
            y += height;
        }
    }

    private DigitizedStorage.Entry hoveredEntry(int mouseX, int mouseY) {
        int gx = mouseX - leftPos - ChestGuiLayout.GRID_X;
        int gy = mouseY - topPos - ChestGuiLayout.GRID_Y;
        if (gx < 0 || gy < 0 || gx >= ChestGuiLayout.COLS * 18 || gy >= ChestGuiLayout.GRID_H) {
            return null;
        }
        List<RaidItemGroups.Row> rows = RaidItemGroups.rows(menu.visibleEntries());
        int rel = gy + scroll;
        int y = 0;
        for (RaidItemGroups.Row row : rows) {
            int height = row.height();
            if (rel >= y && rel < y + height) {
                if (row.isHeader()) {
                    return null;
                }
                int col = gx / 18;
                if (col < 0 || col >= row.items().size()) {
                    return null;
                }
                return row.items().get(col);
            }
            y += height;
        }
        return null;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (search != null && search.canConsumeInput()) {
            if (event.isEscape()) {
                setFocused(null);
                return true;
            }
            search.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    private void focusSearch() {
        setFocused(search);
        if (search != null && !search.isFocused()) {
            search.setFocused(true);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (overScrollbar(event.x(), event.y())) {
            draggingScroll = true;
            scrollToMouse(event.y());
            setFocused(null);
            return true;
        }
        boolean holding = !menu.getCarried().isEmpty();
        if (holding && inTerminal(event.x(), event.y())) {
            setFocused(null);
            ClientPacketDistributor.sendToServer(new ModPayloads.InsertCarriedPayload(menu.chestPos()));
            return true;
        }
        if (search != null && search.isMouseOver(event.x(), event.y())) {
            if (search.mouseClicked(event, doubled)) {
                focusSearch();
                return true;
            }
        }
        setFocused(null);
        DigitizedStorage.Entry hovered = hoveredEntry((int) event.x(), (int) event.y());
        if (hovered != null && (menu.status() == NetworkStatus.OK || menu.status() == NetworkStatus.LOCKED)) {
            int button = event.button();
            boolean shift = minecraft != null && minecraft.hasShiftDown();
            if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
                ClientPacketDistributor.sendToServer(new ModPayloads.ExtractPayload(
                        menu.chestPos(), hovered.stack.copy(), hovered.count, false));
            } else if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                long amount = shift ? hovered.stack.getMaxStackSize() : 1;
                ClientPacketDistributor.sendToServer(new ModPayloads.ExtractPayload(
                        menu.chestPos(), hovered.stack.copy(), amount, false));
            }
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    private boolean inTerminal(double mouseX, double mouseY) {
        return mouseX >= leftPos
                && mouseX < leftPos + imageWidth
                && mouseY >= topPos
                && mouseY < topPos + ChestGuiLayout.INV_Y;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingScroll) {
            scrollToMouse(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScroll = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = maxScroll();
        if (scrollY > 0) {
            scroll = Math.max(0, scroll - ChestGuiLayout.HEADER_H);
        } else if (scrollY < 0) {
            scroll = Math.min(max, scroll + ChestGuiLayout.HEADER_H);
        }
        return true;
    }

    private static String formatCount(long count) {
        if (count < 1000) {
            return Long.toString(count);
        }
        if (count < 1_000_000) {
            return (count / 1000) + "k";
        }
        return (count / 1_000_000) + "M";
    }
}
