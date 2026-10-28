package com.dubz.raid.item;

import com.dubz.raid.block.RaidChestBlock;
import com.dubz.raid.storage.ChestTier;
import com.dubz.raid.storage.RaidMath;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public class RaidChestItem extends BlockItem {
    public RaidChestItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(RaidLore.line("block.raid.chest.subtitle"));
        if (getBlock() instanceof RaidChestBlock chest) {
            if (chest.tier() == ChestTier.ADMIN) {
                tooltip.accept(RaidLore.bytesLiteral(RaidMath.formatAdminCommand(chest.tier().bytes())));
            } else {
                tooltip.accept(RaidLore.bytes("block.raid.chest.capacity", RaidMath.formatBytes(chest.tier().bytes())));
            }
        }
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
