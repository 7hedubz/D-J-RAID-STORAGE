package com.dubz.raid.block;

import com.dubz.raid.storage.DisplayStatus;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.AbstractCauldronBlock;
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
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty SWAPPED =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("swapped");

    private RaidConnect() {
    }

    public static boolean swapped(BlockState state) {
        return state.hasProperty(SWAPPED) && state.getValue(SWAPPED);
    }

    /** Your right when looking at the front. */
    public static Direction viewerRight(BlockState state) {
        return facing(state).getCounterClockWise();
    }

    /** Your left when looking at the front. */
    public static Direction viewerLeft(BlockState state) {
        return facing(state).getClockWise();
    }

    /** RAID-in / LOOKER emit default: viewer right. */
    public static Direction raidIn(BlockState state) {
        return swapped(state) ? viewerLeft(state) : viewerRight(state);
    }

    public static Direction otherSide(BlockState state) {
        return swapped(state) ? viewerRight(state) : viewerLeft(state);
    }

    public static void toggleSides(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(SWAPPED)) {
            return;
        }
        BlockState next = state.setValue(SWAPPED, !state.getValue(SWAPPED));
        if (state.getBlock() instanceof LookerBlock) {
            LookerBlock.updateOutputNeighbors(level, pos, state);
        }
        level.setBlock(pos, next, Block.UPDATE_ALL);
        if (next.getBlock() instanceof LookerBlock) {
            LookerBlock.updateOutputNeighbors(level, pos, next);
        }
    }

    public static boolean isCable(BlockState state) {
        return state.getBlock() instanceof DataCableBlock;
    }

    public static boolean isDevice(BlockState state) {
        return state.getBlock() instanceof RaidChestBlock
                || state.getBlock() instanceof RaidChassisBlock
                || state.getBlock() instanceof RaidHopperBlock
                || state.getBlock() instanceof RaidCrafterBlock
                || state.getBlock() instanceof TreeFarmBlock
                || state.getBlock() instanceof CollectorBlock;
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
        if (state.getBlock() instanceof RaidCrafterBlock) {
            return RaidCrafterBlock.cableFace(state);
        }
        if (state.getBlock() instanceof TreeFarmBlock) {
            return TreeFarmBlock.cableFace(state);
        }
        if (state.getBlock() instanceof CollectorBlock) {
            return CollectorBlock.cableFace(state);
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
        if (neighbor.getBlock() instanceof RaidCrafterBlock) {
            Direction crafterFace = dirFromCable.getOpposite();
            return crafterFace == RaidCrafterBlock.cableFace(neighbor)
                    || crafterFace == RaidCrafterBlock.outputFace(neighbor);
        }
        if (neighbor.getBlock() instanceof CollectorBlock) {
            Direction face = dirFromCable.getOpposite();
            return face == CollectorBlock.cableFace(neighbor) || face == CollectorBlock.cauldronFace(neighbor);
        }
        if (neighbor.getBlock() instanceof AbstractCauldronBlock) {
            return dirFromCable.getAxis().isHorizontal();
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
