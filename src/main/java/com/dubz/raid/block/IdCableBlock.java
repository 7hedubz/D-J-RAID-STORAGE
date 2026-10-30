package com.dubz.raid.block;

import com.dubz.raid.blockentity.IdCableBlockEntity;
import com.dubz.raid.registry.ModDataComponents;
import com.dubz.raid.storage.IdLinks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Explosion;
import org.jspecify.annotations.Nullable;

public class IdCableBlock extends DataCableBlock implements EntityBlock {
    public IdCableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IdCableBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        Integer id = stack.get(ModDataComponents.ID_PAIR.get());
        if (level.getBlockEntity(pos) instanceof IdCableBlockEntity be && id != null && id > 0) {
            be.setPairId(id);
        }
        if (!level.isClientSide() && id != null && id > 0) {
            IdLinks.get((ServerLevel) level).placed((ServerLevel) level, pos, id);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.getAbilities().instabuild
                && level.getBlockEntity(pos) instanceof IdCableBlockEntity be && be.pairId() > 0) {
            IdLinks.get((ServerLevel) level).onIrrevocableLoss(level.getServer(), be.pairId());
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        super.wasExploded(level, pos, explosion);
        IdLinks.get(level).onIrrevocableLoss(level.getServer(), IdLinks.get(level).pairAt(level, pos));
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        IdLinks.get(level).unplace(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
