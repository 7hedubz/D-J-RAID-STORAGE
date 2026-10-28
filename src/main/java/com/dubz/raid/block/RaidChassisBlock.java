package com.dubz.raid.block;

import com.dubz.raid.blockentity.RaidChassisBlockEntity;
import com.dubz.raid.storage.DisplayStatus;
import com.dubz.raid.storage.NetworkGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jspecify.annotations.Nullable;

public class RaidChassisBlock extends BaseEntityBlock {
    public RaidChassisBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(RaidConnect.FACING, Direction.NORTH)
                .setValue(DisplayStatus.PROPERTY, DisplayStatus.DISCONNECTED));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RaidChassisBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(RaidConnect.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RaidConnect.FACING, DisplayStatus.PROPERTY);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(RaidConnect.FACING, rotation.rotate(state.getValue(RaidConnect.FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(RaidConnect.FACING)));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide()) {
            return;
        }
        NetworkGraph graph = NetworkGraph.get((ServerLevel) level);
        graph.addChassis(pos, state.getValue(RaidConnect.FACING));
        if (!oldState.is(this) || oldState.getValue(RaidConnect.FACING) != state.getValue(RaidConnect.FACING)) {
            graph.refreshVisuals((ServerLevel) level);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        NetworkGraph graph = NetworkGraph.get(level);
        graph.remove(pos);
        graph.refreshVisuals(level);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RaidChassisBlockEntity chassis
                && !level.isClientSide()
                && player.preventsBlockDrops()
                && !chassis.isEmpty()) {
            ItemStack itemStack = new ItemStack(this);
            itemStack.applyComponents(blockEntity.collectComponents());
            ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, itemStack);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
