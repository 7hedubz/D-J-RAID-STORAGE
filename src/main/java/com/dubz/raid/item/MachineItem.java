package com.dubz.raid.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public class MachineItem extends BlockItem {
    private final String subtitleKey;
    private final String tooltipKey;

    public MachineItem(Block block, Properties properties, String subtitleKey, String tooltipKey) {
        super(block, properties);
        this.subtitleKey = subtitleKey;
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(RaidLore.line(subtitleKey));
        tooltip.accept(RaidLore.line(tooltipKey));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
