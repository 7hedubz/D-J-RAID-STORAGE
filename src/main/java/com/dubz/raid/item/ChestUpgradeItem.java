package com.dubz.raid.item;

import com.dubz.raid.block.RaidChestBlock;
import com.dubz.raid.block.RaidConnect;
import com.dubz.raid.storage.ChestTier;
import com.dubz.raid.storage.DisplayStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

public class ChestUpgradeItem extends Item {
    private final ChestTier from;
    private final ChestTier to;

    public ChestUpgradeItem(Properties properties, ChestTier from, ChestTier to) {
        super(properties);
        this.from = from;
        this.to = to;
    }

    public ChestTier from() {
        return from;
    }

    public ChestTier to() {
        return to;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof RaidChestBlock chest) || chest.tier() != from) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            BlockState next = to.block().defaultBlockState()
                    .setValue(RaidConnect.FACING, state.getValue(RaidConnect.FACING))
                    .setValue(DisplayStatus.PROPERTY, state.getValue(DisplayStatus.PROPERTY));
            level.setBlock(pos, next, Block.UPDATE_ALL);
            Player player = context.getPlayer();
            if (player == null || !player.hasInfiniteMaterials()) {
                context.getItemInHand().shrink(1);
            }
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.4f, 1.4f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(RaidLore.line("item.raid.upgrade.tooltip", from.getSerializedName(), to.getSerializedName()));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
