package com.dubz.raid.block;

import com.dubz.raid.storage.DisplayStatus;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public final class RaidConnect {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private RaidConnect() {
    }

    public static boolean isCable(BlockState state) {
        return state.getBlock() instanceof DataCableBlock;
    }

    public static boolean isDevice(BlockState state) {
        return state.getBlock() instanceof RaidChestBlock
                || state.getBlock() instanceof RaidChassisBlock
                || state.getBlock() instanceof RaidHopperBlock;
    }

    public static Direction facing(BlockState state) {
        return state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
    }

    public static DisplayStatus display(BlockState state) {
        return state.hasProperty(DisplayStatus.PROPERTY) ? state.getValue(DisplayStatus.PROPERTY) : DisplayStatus.DISCONNECTED;
    }

    public static Direction devicePort(BlockState state) {
        if (state.getBlock() instanceof RaidHopperBlock) {
            return RaidHopperBlock.cableFace(state);
        }
        return facing(state).getOpposite();
    }

    public static boolean isItemInventory(BlockState state) {
        Block block = state.getBlock();
        return block instanceof HopperBlock
                || block instanceof ChestBlock
                || block instanceof BarrelBlock
                || block instanceof ShulkerBoxBlock
                || block instanceof AbstractFurnaceBlock
                || block instanceof BrewingStandBlock
                || block instanceof DispenserBlock
                || block instanceof CrafterBlock;
    }

    /** Cable at `from` looking `dir` at `neighbor`. */
    public static boolean cableConnects(BlockState neighbor, Direction dirFromCable) {
        if (isCable(neighbor)) {
            return true;
        }
        if (neighbor.getBlock() instanceof RaidHopperBlock) {
            Direction hopperFace = dirFromCable.getOpposite();
            return hopperFace == RaidHopperBlock.cableFace(neighbor)
                    || hopperFace == RaidHopperBlock.outputFace(neighbor);
        }
        if (isDevice(neighbor)) {
            return devicePort(neighbor) == dirFromCable.getOpposite();
        }
        if (isItemInventory(neighbor)) {
            return acceptsInsertOnFace(neighbor, dirFromCable.getOpposite());
        }
        return false;
    }

    /** Furnace-style machines only accept inserts on non-output faces (not the bottom). */
    public static boolean acceptsInsertOnFace(BlockState state, Direction faceIntoBlock) {
        if (state.getBlock() instanceof AbstractFurnaceBlock) {
            return faceIntoBlock != Direction.DOWN;
        }
        return isItemInventory(state);
    }
}
