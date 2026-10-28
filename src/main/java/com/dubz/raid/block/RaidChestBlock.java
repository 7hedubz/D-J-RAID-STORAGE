package com.dubz.raid.block;

import com.dubz.raid.item.ChestUpgradeItem;
import com.dubz.raid.menu.ChestMenu;
import com.dubz.raid.registry.ModSounds;
import com.dubz.raid.storage.ChestTier;
import com.dubz.raid.storage.DisplayStatus;
import com.dubz.raid.storage.NetworkGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public class RaidChestBlock extends Block {
    private final ChestTier tier;

    public RaidChestBlock(Properties properties, ChestTier tier) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(RaidConnect.FACING, Direction.NORTH)
                .setValue(DisplayStatus.PROPERTY, DisplayStatus.DISCONNECTED));
    }

    public ChestTier tier() {
        return tier;
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
        graph.addChest(pos, tier, state.getValue(RaidConnect.FACING));
        if (!oldState.is(this) || oldState.getValue(RaidConnect.FACING) != state.getValue(RaidConnect.FACING)) {
            graph.refreshVisuals((ServerLevel) level);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        BlockState replacement = level.getBlockState(pos);
        if (!(replacement.getBlock() instanceof RaidChestBlock)) {
            NetworkGraph graph = NetworkGraph.get(level);
            graph.remove(pos);
            graph.refreshVisuals(level);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isCrouching() && stack.getItem() instanceof ChestUpgradeItem) {
            return InteractionResult.PASS;
        }
        open(level, pos, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        open(level, pos, player);
        return InteractionResult.SUCCESS;
    }

    private void open(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return;
        }
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ChestMenu(id, inv, pos),
                Component.translatable(getDescriptionId())
        ), pos);
        level.playSound(null, pos, ModSounds.CHEST_OPEN.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
    }
}
