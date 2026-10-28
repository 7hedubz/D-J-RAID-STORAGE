package com.dubz.raid.item;

import com.dubz.raid.registry.ModDataComponents;
import com.dubz.raid.storage.DigitizedStorage;
import com.dubz.raid.storage.RaidMath;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public class RaidChassisItem extends BlockItem {
    public RaidChassisItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(RaidLore.line("block.raid.raid_chassis.desc"));
        DigitizedStorage storage = stack.get(ModDataComponents.STORAGE.get());
        if (storage != null && !storage.isEmpty()) {
            tooltip.accept(RaidLore.line(
                    "block.raid.raid_chassis.stored",
                    RaidMath.formatBytes(storage.usedBytes()),
                    storage.entries().size()
            ));
        }
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
