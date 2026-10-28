package com.dubz.raid.menu;

import com.dubz.raid.blockentity.CollectorBlockEntity;
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

public class CollectorMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176;
    public static final int HEIGHT = 166;
    public static final int BUTTON_X = 64;
    public static final int BUTTON_Y = 20;
    public static final int INV_X = 8;
    public static final int INV_Y = 84;
    public static final int HOTBAR_Y = 142;

    private final CollectorBlockEntity collector;
    private int runningFlag;

    public CollectorMenu(int id, Inventory inv, FriendlyByteBuf data) {
        this(id, inv, at(inv, data.readBlockPos()));
    }

    public CollectorMenu(int id, Inventory inv, CollectorBlockEntity collector) {
        super(ModMenus.COLLECTOR.get(), id);
        this.collector = collector;
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
                return collector.running() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                runningFlag = value;
            }
        });
    }

    public boolean running() {
        return runningFlag != 0 || collector.running();
    }

    public void toggle(Player player) {
        collector.toggle(player);
    }

    private static CollectorBlockEntity at(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof CollectorBlockEntity collector) {
            return collector;
        }
        throw new IllegalStateException("No COLLECTOR at " + pos);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(collector, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            com.dubz.raid.registry.MachineSounds.play(player.level(), collector.getBlockPos(), com.dubz.raid.registry.ModSounds.COLLECTOR_OFF);
        }
    }
}
