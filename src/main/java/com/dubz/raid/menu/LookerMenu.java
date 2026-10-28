package com.dubz.raid.menu;

import com.dubz.raid.blockentity.LookerBlockEntity;
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

public class LookerMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176;
    public static final int HEIGHT = 220;
    public static final int INV_X = 8;
    public static final int INV_Y = 138;
    public static final int HOTBAR_Y = 196;

    private final LookerBlockEntity looker;
    private int runningFlag;
    private int current;
    private int threshold;
    private int stride;
    private int swappedFlag;

    public LookerMenu(int id, Inventory inv, FriendlyByteBuf data) {
        this(id, inv, at(inv, data.readBlockPos()));
    }

    public LookerMenu(int id, Inventory inv, LookerBlockEntity looker) {
        super(ModMenus.LOOKER.get(), id);
        this.looker = looker;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, INV_X + col * 18, HOTBAR_Y));
        }
        addDataSlot(bind(() -> looker.running() ? 1 : 0, v -> runningFlag = v));
        addDataSlot(bind(looker::current, v -> current = v));
        addDataSlot(bind(looker::threshold, v -> threshold = v));
        addDataSlot(bind(looker::stride, v -> stride = v));
        addDataSlot(bind(() -> com.dubz.raid.block.RaidConnect.swapped(looker.getBlockState()) ? 1 : 0, v -> swappedFlag = v));
    }

    private static DataSlot bind(java.util.function.IntSupplier get, java.util.function.IntConsumer set) {
        return new DataSlot() {
            @Override
            public int get() {
                return get.getAsInt();
            }

            @Override
            public void set(int value) {
                set.accept(value);
            }
        };
    }

    public boolean running() {
        return runningFlag != 0 || looker.running();
    }

    public int current() {
        return current;
    }

    public int threshold() {
        return threshold;
    }

    public int stride() {
        return stride;
    }

    public LookerBlockEntity looker() {
        return looker;
    }

    public boolean swapped() {
        return swappedFlag != 0 || com.dubz.raid.block.RaidConnect.swapped(looker.getBlockState());
    }

    public BlockPos pos() {
        return looker.getBlockPos();
    }

    public void toggle(Player player) {
        looker.toggle(player);
    }

    private static LookerBlockEntity at(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof LookerBlockEntity looker) {
            return looker;
        }
        throw new IllegalStateException("No LOOKER at " + pos);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(looker, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            com.dubz.raid.registry.MachineSounds.play(player.level(), looker.getBlockPos(), com.dubz.raid.registry.ModSounds.LOOKER_OFF);
        }
    }
}
