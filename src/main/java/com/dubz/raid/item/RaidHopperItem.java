package com.dubz.raid.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public class RaidHopperItem extends BlockItem {
    public RaidHopperItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(RaidLore.line("block.raid.hopper.subtitle"));
        tooltip.accept(RaidLore.line("block.raid.hopper.tooltip"));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
