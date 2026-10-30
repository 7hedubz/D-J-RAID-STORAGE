package com.dubz.raid.item;

import com.dubz.raid.registry.ModDataComponents;
import com.dubz.raid.storage.IdLinks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class IdCableItem extends BlockItem {
    public IdCableItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Player player) {
        if (!stack.has(ModDataComponents.ID_PAIR.get()) && player.level() instanceof ServerLevel server) {
            stack.set(ModDataComponents.ID_PAIR.get(), IdLinks.get(server).allocate());
        }
        super.onCraftedBy(stack, player);
    }

    @Override
    public Component getName(ItemStack stack) {
        Integer id = stack.get(ModDataComponents.ID_PAIR.get());
        if (id == null || id <= 0) {
            return super.getName(stack);
        }
        return Component.translatable("block.raid.id_cable.numbered", formatId(id));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        Integer id = stack.get(ModDataComponents.ID_PAIR.get());
        if (id == null || !IdLinks.get(level).isDoomed(id)) {
            return;
        }
        stack.setCount(0);
        IdLinks.explode(level, entity.getX(), entity.getY(), entity.getZ());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(RaidLore.line("block.raid.id_cable.subtitle"));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }

    public static String formatId(int id) {
        return id < 10000 ? String.format("%04d", id) : Integer.toString(id);
    }
}
