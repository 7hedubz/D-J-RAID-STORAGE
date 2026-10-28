package com.dubz.raid.menu;

import com.dubz.raid.blockentity.TreeFarmBlockEntity;
import com.dubz.raid.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TreeFarmMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176;
    public static final int HEIGHT = 166;
    public static final int SLOT_X = 80;
    public static final int SLOT_Y = 20;
    public static final int BAR_X = 8;
    public static final int BAR_Y = 42;
    public static final int BAR_W = 160;
    public static final int BAR_H = 12;
    public static final int BUTTON_X = 116;
    public static final int BUTTON_Y = 18;
    public static final int INV_X = 8;
    public static final int INV_Y = 84;
    public static final int HOTBAR_Y = 142;

    private final TreeFarmBlockEntity farm;
    private int runningFlag;
    private int progress;

    public TreeFarmMenu(int id, Inventory inv, FriendlyByteBuf data) {
        this(id, inv, farmAt(inv, data.readBlockPos()));
    }

    public TreeFarmMenu(int id, Inventory inv, TreeFarmBlockEntity farm) {
        super(ModMenus.TREE_FARM.get(), id);
        this.farm = farm;
        addSlot(new Slot(farm.sapling(), 0, SLOT_X, SLOT_Y) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.typeHolder().is(farm.saplingTag());
            }
        });
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
                return farm.running() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                runningFlag = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return farm.progress();
            }

            @Override
            public void set(int value) {
                progress = value;
            }
        });
    }

    public TreeFarmBlockEntity farm() {
        return farm;
    }

    public boolean running() {
        return runningFlag != 0 || farm.running();
    }

    public int progress() {
        return progress;
    }

    public void toggle(Player player) {
        farm.toggle(player);
    }

    private static TreeFarmBlockEntity farmAt(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof TreeFarmBlockEntity farm) {
            return farm;
        }
        throw new IllegalStateException("No TREE-FARM at " + pos);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();
            if (index == 0) {
                if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(farm, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            com.dubz.raid.registry.MachineSounds.play(player.level(), farm.getBlockPos(),
                    farm.nether() ? com.dubz.raid.registry.ModSounds.NETHER_TREE_FARM_OFF
                            : com.dubz.raid.registry.ModSounds.TREE_FARM_OFF);
        }
    }
}
