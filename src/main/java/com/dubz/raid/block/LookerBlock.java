package com.dubz.raid.block;

import com.dubz.raid.blockentity.LookerBlockEntity;
import com.dubz.raid.registry.MachineSounds;
import com.dubz.raid.registry.ModSounds;
import com.dubz.raid.storage.LookerMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class LookerBlock extends BaseEntityBlock {
    public static final BooleanProperty BLINK = BooleanProperty.create("blink");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public LookerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(RaidConnect.FACING, Direction.NORTH)
                .setValue(LookerMode.PROPERTY, LookerMode.STANDBY)
                .setValue(BLINK, false)
                .setValue(POWERED, false));
    }

    public static Direction detectFace(BlockState state) {
        return state.getValue(RaidConnect.FACING).getCounterClockWise();
    }

    public static Direction outputFace(BlockState state) {
        return state.getValue(RaidConnect.FACING).getClockWise();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LookerBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, com.dubz.raid.registry.ModBlockEntities.LOOKER.get(), LookerBlockEntity::serverTick);
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
        builder.add(RaidConnect.FACING, LookerMode.PROPERTY, BLINK, POWERED);
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
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks,
                                     BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // Same contract as ObserverBlock: the querying neighbour sits on the output
        // face and asks with the opposite direction (the watch/detect face).
        return state.getValue(POWERED) && direction == detectFace(state) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected boolean shouldRedstoneWireConnectTo(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction == detectFace(state);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Pulse length is owned by LookerBlockEntity (exactly 2 ticks).
    }

    public static void updateOutputNeighbors(Level level, BlockPos pos, BlockState state) {
        Direction out = outputFace(state);
        BlockPos target = pos.relative(out);
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, out, null);
        level.neighborChanged(target, state.getBlock(), orientation);
        level.updateNeighborsAtExceptFromFacing(target, state.getBlock(), out.getOpposite(), orientation);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        open(level, pos, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        open(level, pos, player);
        return InteractionResult.SUCCESS;
    }

    private void open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LookerBlockEntity looker) {
            MachineSounds.play(level, pos, ModSounds.LOOKER_ON);
            player.openMenu(looker, pos);
        }
    }
}
