package com.dubz.raid.block;

import com.dubz.raid.storage.NetworkGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public class DataCableBlock extends Block {
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION;

    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape NORTH_ARM = Block.box(5, 5, 0, 11, 11, 5);
    private static final VoxelShape SOUTH_ARM = Block.box(5, 5, 11, 11, 11, 16);
    private static final VoxelShape WEST_ARM = Block.box(0, 5, 5, 5, 11, 11);
    private static final VoxelShape EAST_ARM = Block.box(11, 5, 5, 16, 11, 11);
    private static final VoxelShape DOWN_ARM = Block.box(5, 0, 5, 11, 5, 11);
    private static final VoxelShape UP_ARM = Block.box(5, 11, 5, 11, 16, 11);

    public DataCableBlock(Properties properties) {
        super(properties);
        BlockState state = this.stateDefinition.any();
        for (BooleanProperty property : PROPERTY_BY_DIRECTION.values()) {
            state = state.setValue(property, false);
        }
        this.registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(
                PROPERTY_BY_DIRECTION.get(Direction.NORTH),
                PROPERTY_BY_DIRECTION.get(Direction.EAST),
                PROPERTY_BY_DIRECTION.get(Direction.SOUTH),
                PROPERTY_BY_DIRECTION.get(Direction.WEST),
                PROPERTY_BY_DIRECTION.get(Direction.UP),
                PROPERTY_BY_DIRECTION.get(Direction.DOWN)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connections(context.getLevel(), context.getClickedPos(), this.defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (level instanceof ServerLevel server) {
            NetworkGraph.get(server).addCable(pos);
        }
        return state.setValue(PROPERTY_BY_DIRECTION.get(directionToNeighbour),
                RaidConnect.cableConnects(neighbourState, directionToNeighbour));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this)) {
            level.invalidateCapabilities(pos);
            NetworkGraph graph = NetworkGraph.get((ServerLevel) level);
            graph.addCable(pos);
            graph.refreshVisuals((ServerLevel) level);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        level.invalidateCapabilities(pos);
        NetworkGraph graph = NetworkGraph.get(level);
        graph.remove(pos);
        graph.refreshVisuals(level);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        if (state.getValue(PROPERTY_BY_DIRECTION.get(Direction.NORTH))) {
            shape = Shapes.or(shape, NORTH_ARM);
        }
        if (state.getValue(PROPERTY_BY_DIRECTION.get(Direction.SOUTH))) {
            shape = Shapes.or(shape, SOUTH_ARM);
        }
        if (state.getValue(PROPERTY_BY_DIRECTION.get(Direction.WEST))) {
            shape = Shapes.or(shape, WEST_ARM);
        }
        if (state.getValue(PROPERTY_BY_DIRECTION.get(Direction.EAST))) {
            shape = Shapes.or(shape, EAST_ARM);
        }
        if (state.getValue(PROPERTY_BY_DIRECTION.get(Direction.UP))) {
            shape = Shapes.or(shape, UP_ARM);
        }
        if (state.getValue(PROPERTY_BY_DIRECTION.get(Direction.DOWN))) {
            shape = Shapes.or(shape, DOWN_ARM);
        }
        return shape;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    private static BlockState connections(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction dir : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(dir));
            state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), RaidConnect.cableConnects(neighbor, dir));
        }
        return state;
    }
}
