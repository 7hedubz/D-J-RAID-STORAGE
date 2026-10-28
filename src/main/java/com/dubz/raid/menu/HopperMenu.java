package com.dubz.raid.menu;

import com.dubz.raid.blockentity.RaidHopperBlockEntity;
import com.dubz.raid.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class HopperMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176;
    public static final int HEIGHT = 133;
    public static final int FILTER_X = 80;
    public static final int FILTER_Y = 20;
    public static final int INV_X = 8;
    public static final int INV_Y = 51;
    public static final int HOTBAR_Y = 109;

    private final RaidHopperBlockEntity hopper;

    public BlockPos pos() {
        return hopper.getBlockPos();
    }

    public HopperMenu(int id, Inventory inv, FriendlyByteBuf data) {
        this(id, inv, hopperAt(inv, data.readBlockPos()));
    }

    public HopperMenu(int id, Inventory inv, RaidHopperBlockEntity hopper) {
        super(ModMenus.HOPPER.get(), id);
        this.hopper = hopper;
        addSlot(new Slot(hopper.filter(), 0, FILTER_X, FILTER_Y) {
            @Override
            public int getMaxStackSize() {
                return 1;
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
    }

    private static RaidHopperBlockEntity hopperAt(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof RaidHopperBlockEntity hopper) {
            return hopper;
        }
        throw new IllegalStateException("No HOPPER at " + pos);
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
        return Container.stillValidBlockEntity(hopper, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            com.dubz.raid.registry.MachineSounds.play(player.level(), hopper.getBlockPos(), com.dubz.raid.registry.ModSounds.HOPPER_OFF);
        }
    }
}
