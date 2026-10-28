package com.dubz.raid.menu;

import com.dubz.raid.blockentity.RaidCrafterBlockEntity;
import com.dubz.raid.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CrafterMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176;
    public static final int HEIGHT = 166;
    public static final int GRID_X = 30;
    public static final int GRID_Y = 17;
    public static final int RESULT_X = 124;
    public static final int RESULT_Y = 35;
    public static final int BUTTON_X = 116;
    public static final int BUTTON_Y = 58;
    public static final int BUTTON_W = 48;
    public static final int BUTTON_H = 16;
    public static final int INV_X = 8;
    public static final int INV_Y = 84;
    public static final int HOTBAR_Y = 142;

    private final RaidCrafterBlockEntity crafter;
    private int runningFlag;

    public CrafterMenu(int id, Inventory inv, FriendlyByteBuf data) {
        this(id, inv, crafterAt(inv, data.readBlockPos()));
    }

    public CrafterMenu(int id, Inventory inv, RaidCrafterBlockEntity crafter) {
        super(ModMenus.AUTO_CRAFTER.get(), id);
        this.crafter = crafter;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + row * 3;
                addSlot(new PatternSlot(index, GRID_X + col * 18, GRID_Y + row * 18));
            }
        }
        addSlot(new ResultSlot(RESULT_X, RESULT_Y));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, INV_X + col * 18, HOTBAR_Y));
        }
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return crafter.running() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                runningFlag = value;
            }
        });
    }

    public RaidCrafterBlockEntity crafter() {
        return crafter;
    }

    public BlockPos pos() {
        return crafter.getBlockPos();
    }

    public boolean running() {
        return runningFlag != 0 || crafter.running();
    }

    public void toggle(Player player) {
        crafter.toggle(player);
    }

    private static RaidCrafterBlockEntity crafterAt(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof RaidCrafterBlockEntity crafter) {
            return crafter;
        }
        throw new IllegalStateException("No AUTO-CRAFTER at " + pos);
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        if (slotIndex >= 0 && slotIndex < 9) {
            ItemStack carried = getCarried();
            if (carried.isEmpty()) {
                crafter.clearSlot(slotIndex);
            } else {
                crafter.setSlot(slotIndex, carried);
            }
            broadcastChanges();
            return;
        }
        if (slotIndex == 9) {
            return;
        }
        super.clicked(slotIndex, buttonNum, containerInput, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= 0 && index < 9) {
            crafter.clearSlot(index);
            broadcastChanges();
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(crafter, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            com.dubz.raid.registry.MachineSounds.play(player.level(), crafter.getBlockPos(), com.dubz.raid.registry.ModSounds.CRAFTER_OFF);
        }
    }

    private class PatternSlot extends Slot {
        private final int patternIndex;

        PatternSlot(int patternIndex, int x, int y) {
            super(new SimpleContainer(1), 0, x, y);
            this.patternIndex = patternIndex;
        }

        @Override
        public ItemStack getItem() {
            return crafter.patternSlot(patternIndex);
        }

        @Override
        public void set(ItemStack stack) {
            if (stack.isEmpty()) {
                crafter.clearSlot(patternIndex);
            } else {
                crafter.setSlot(patternIndex, stack);
            }
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return true;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public ItemStack remove(int amount) {
            ItemStack old = crafter.patternSlot(patternIndex).copy();
            crafter.clearSlot(patternIndex);
            return old;
        }

        @Override
        public boolean hasItem() {
            return !crafter.patternSlot(patternIndex).isEmpty();
        }
    }

    private class ResultSlot extends Slot {
        private ItemStack cached = ItemStack.EMPTY;

        ResultSlot(int x, int y) {
            super(new SimpleContainer(1), 0, x, y);
        }

        @Override
        public ItemStack getItem() {
            if (crafter.getLevel() != null && !crafter.getLevel().isClientSide()) {
                cached = crafter.previewResult();
            }
            return cached;
        }

        @Override
        public void set(ItemStack stack) {
            cached = stack.copy();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public ItemStack remove(int amount) {
            return ItemStack.EMPTY;
        }
    }
}
