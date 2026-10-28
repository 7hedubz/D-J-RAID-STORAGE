package com.dubz.raid.block;

import com.dubz.raid.blockentity.TreeFarmBlockEntity;
import com.dubz.raid.registry.MachineSounds;
import com.dubz.raid.storage.DisplayStatus;
import com.dubz.raid.storage.NetworkGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class TreeFarmBlock extends BaseEntityBlock {
    public enum Kind { OVERWORLD, NETHER, END }

    private final Kind kind;

    public TreeFarmBlock(Properties properties) {
        this(properties, Kind.OVERWORLD);
    }

    public TreeFarmBlock(Properties properties, boolean nether) {
        this(properties, nether ? Kind.NETHER : Kind.OVERWORLD);
    }

    public TreeFarmBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(RaidConnect.FACING, Direction.NORTH)
                .setValue(DisplayStatus.PROPERTY, DisplayStatus.DISCONNECTED));
    }

    public Kind kind() {
        return kind;
    }

    public boolean nether() {
        return kind == Kind.NETHER;
    }

    public boolean end() {
        return kind == Kind.END;
    }

    public static Direction cableFace(BlockState state) {
        return state.getValue(RaidConnect.FACING).getClockWise();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TreeFarmBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, com.dubz.raid.registry.ModBlockEntities.TREE_FARM.get(), TreeFarmBlockEntity::serverTick);
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
        graph.addTreeFarm(pos, state.getValue(RaidConnect.FACING));
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
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TreeFarmBlockEntity farm) {
            MachineSounds.play(level, pos, farm.guiSound(true));
            player.openMenu(farm, pos);
        }
    }
}
