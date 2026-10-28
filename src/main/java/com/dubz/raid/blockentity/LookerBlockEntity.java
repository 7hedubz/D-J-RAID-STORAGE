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
    public static final int FRAME_TICKS = 5;

    private boolean running;
    private int current;
    private int threshold = 1;
    private int stride = 1;
    private int seen;
    private int blinkIn;
    private int blinkLeft;
    private int detectAnim;
    private int pulseAnim;
    private int pulseLeft;
    private BlockState lastWatched;

    public LookerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOOKER.get(), pos, state);
        blinkIn = 80 + Math.floorMod(pos.hashCode(), 21);
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
            pulseLeft = 0;
            pulseAnim = 0;
            detectAnim = 0;
        }
        player.sendOverlayMessage(Component.translatable(running ? "gui.raid.looker.on" : "gui.raid.looker.off"));
        setChanged();
    }

    public void observe() {
        if (level == null || level.isClientSide() || !running || pulseLeft > 0) {
            return;
        }
        seen++;
        if (seen >= stride) {
            seen = 0;
            current++;
            if (current >= threshold) {
                startPulse();
            } else {
                detectAnim = FRAME_TICKS * 2;
            }
        }
        setChanged();
    }

    private void startPulse() {
        current = 0;
        seen = 0;
        detectAnim = 0;
        pulseLeft = 2;
        pulseAnim = FRAME_TICKS * 2;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LookerBlockEntity entity) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        boolean wasPowered = state.getValue(LookerBlock.POWERED);

        if (entity.pulseLeft > 0) {
            entity.pulseLeft--;
        }
        if (entity.pulseAnim > 0) {
            entity.pulseAnim--;
        }
        if (entity.detectAnim > 0) {
            entity.detectAnim--;
        }

        if (entity.running && entity.pulseLeft == 0) {
            BlockState watched = server.getBlockState(pos.relative(LookerBlock.detectFace(state)));
            if (entity.lastWatched == null) {
                entity.lastWatched = watched;
            } else if (!entity.lastWatched.equals(watched)) {
                entity.lastWatched = watched;
                entity.observe();
            }
        }
        if (entity.running && entity.pulseLeft == 0 && entity.current >= entity.threshold) {
            entity.startPulse();
        }

        LookerMode mode;
        boolean blink = false;
        boolean powered = entity.pulseLeft > 0;
        if (entity.pulseAnim > FRAME_TICKS) {
            mode = LookerMode.PULSE;
        } else if (entity.pulseAnim > 0) {
            mode = LookerMode.PULSE_B;
        } else if (entity.detectAnim > FRAME_TICKS) {
            mode = LookerMode.DETECT;
        } else if (entity.detectAnim > 0) {
            mode = LookerMode.DETECT_B;
        } else {
            mode = LookerMode.STANDBY;
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
                .setValue(LookerBlock.POWERED, powered);
        if (!next.equals(state)) {
            server.setBlock(pos, next, Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ON_PLACE);
        }
        if (powered != wasPowered) {
            LookerBlock.updateOutputNeighbors(server, pos, next);
        }
        entity.setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("running", running);
        output.putInt("current", current);
        output.putInt("threshold", threshold);
        output.putInt("stride", stride);
        output.putInt("seen", seen);
        output.putInt("pulseLeft", pulseLeft);
        output.putInt("pulseAnim", pulseAnim);
        output.putInt("detectAnim", detectAnim);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        running = input.getBooleanOr("running", false);
        current = input.getIntOr("current", 0);
        threshold = input.getIntOr("threshold", 1);
        stride = input.getIntOr("stride", 1);
        seen = input.getIntOr("seen", 0);
        pulseLeft = input.getIntOr("pulseLeft", 0);
        pulseAnim = input.getIntOr("pulseAnim", 0);
        detectAnim = input.getIntOr("detectAnim", 0);
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
