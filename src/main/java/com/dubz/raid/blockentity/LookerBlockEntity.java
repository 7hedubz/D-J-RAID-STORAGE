package com.dubz.raid.blockentity;

import com.dubz.raid.block.LookerBlock;
import com.dubz.raid.registry.ModBlockEntities;
import com.dubz.raid.storage.LookerMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class LookerBlockEntity extends BlockEntity implements MenuProvider {
    private boolean running;
    private int current;
    private int threshold = 1;
    private int stride = 1;
    private int seen;
    private int blinkIn;
    private int blinkLeft;
    private int detectLeft;
    private BlockState lastWatched;

    public LookerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOOKER.get(), pos, state);
        blinkIn = 80 + pos.hashCode() % 21;
    }

    public boolean running() {
        return running;
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

    public void setCurrent(int value) {
        current = Math.max(0, value);
        setChanged();
    }

    public void setThreshold(int value) {
        threshold = Math.max(1, value);
        setChanged();
    }

    public void setStride(int value) {
        stride = Math.max(1, value);
        setChanged();
    }

    public void toggle(Player player) {
        running = !running;
        if (!running) {
            current = 0;
            seen = 0;
        }
        player.sendOverlayMessage(Component.translatable(running ? "gui.raid.looker.on" : "gui.raid.looker.off"));
        setChanged();
    }

    public void observe() {
        if (level == null || level.isClientSide() || !running) {
            return;
        }
        seen++;
        if (seen >= stride) {
            seen = 0;
            current++;
            detectLeft = 10;
        }
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LookerBlockEntity entity) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        BlockState watched = server.getBlockState(pos.relative(LookerBlock.detectFace(state)));
        if (entity.lastWatched == null) {
            entity.lastWatched = watched;
        } else if (!entity.lastWatched.equals(watched)) {
            entity.lastWatched = watched;
            entity.observe();
        }
        LookerMode mode = LookerMode.STANDBY;
        boolean blink = false;
        boolean powered = false;
        if (entity.current > entity.threshold) {
            mode = LookerMode.ERROR;
        } else if (entity.running && entity.current >= entity.threshold && entity.threshold > 0 && entity.current > 0) {
            entity.current = 0;
            entity.seen = 0;
            powered = true;
            mode = LookerMode.PULSE;
            server.scheduleTick(pos, state.getBlock(), 2);
            LookerBlock.updateOutputNeighbors(server, pos, state);
        } else if (entity.detectLeft > 0) {
            entity.detectLeft--;
            mode = LookerMode.DETECT;
        } else {
            if (entity.blinkLeft > 0) {
                entity.blinkLeft--;
                blink = true;
            } else if (--entity.blinkIn <= 0) {
                entity.blinkLeft = 4;
                entity.blinkIn = 80 + server.getRandom().nextInt(21);
                blink = true;
            }
        }
        BlockState next = state
                .setValue(LookerMode.PROPERTY, mode)
                .setValue(LookerBlock.BLINK, blink)
                .setValue(LookerBlock.POWERED, powered || state.getValue(LookerBlock.POWERED) && mode == LookerMode.PULSE);
        if (powered) {
            next = next.setValue(LookerBlock.POWERED, true).setValue(LookerMode.PROPERTY, LookerMode.PULSE);
        }
        if (!next.equals(state)) {
            server.setBlock(pos, next, Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ON_PLACE);
            if (powered) {
                LookerBlock.updateOutputNeighbors(server, pos, next);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("running", running);
        output.putInt("current", current);
        output.putInt("threshold", threshold);
        output.putInt("stride", stride);
        output.putInt("seen", seen);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        running = input.getBooleanOr("running", false);
        current = input.getIntOr("current", 0);
        threshold = input.getIntOr("threshold", 1);
        stride = input.getIntOr("stride", 1);
        seen = input.getIntOr("seen", 0);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.raid.looker");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new com.dubz.raid.menu.LookerMenu(id, inventory, this);
    }
}
