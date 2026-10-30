package com.dubz.raid.block;

import com.dubz.raid.blockentity.IdCableBlockEntity;
import com.dubz.raid.registry.ModBlockEntities;
import com.dubz.raid.registry.ModDataComponents;
import com.dubz.raid.storage.IdLinks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.ID_CABLE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> IdCableBlockEntity.serverTick(lvl, pos, st, (IdCableBlockEntity) be);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof IdCableBlockEntity be) || !be.linked()) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            int flipX = random.nextInt(2) * 2 - 1;
            int flipZ = random.nextInt(2) * 2 - 1;
            double x = pos.getX() + 0.5 + 0.15 * flipX;
            double y = pos.getY() + 0.2 + random.nextFloat() * 0.6;
            double z = pos.getZ() + 0.5 + 0.15 * flipZ;
            double xa = random.nextFloat() * 0.5 * flipX;
            double ya = (random.nextFloat() - 0.5) * 0.125;
            double za = random.nextFloat() * 0.5 * flipZ;
            level.addParticle(ParticleTypes.PORTAL, x, y, z, xa, ya, za);
        }
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
        int id = IdLinks.get(level).pairAt(level, pos);
        if (id > 0) {
            IdLinks.get(level).onIrrevocableLoss(level.getServer(), id);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        IdLinks.get(level).unplace(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
