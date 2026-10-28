package com.dubz.raid.registry;

import com.dubz.raid.RaidMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RaidMod.MOD_ID);

    public static final Supplier<CreativeModeTab> RAID = TABS.register("raid", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.raid"))
            .icon(() -> ModBlocks.RAID_CHASSIS.get().asItem().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(ModBlocks.RAID_CHASSIS.get());
                output.accept(ModBlocks.DATA_CABLE.get());
                output.accept(ModBlocks.HOPPER.get());
                output.accept(ModBlocks.AUTO_CRAFTER.get());
                output.accept(ModBlocks.TREE_FARM.get());
                output.accept(ModBlocks.NETHER_TREE_FARM.get());
                if (net.neoforged.fml.ModList.get().isLoaded("betterend")) {
                    output.accept(ModBlocks.END_TREE_FARM.get());
                }
                output.accept(ModBlocks.COLLECTOR.get());
                output.accept(ModBlocks.LOOKER.get());
                output.accept(ModBlocks.STONE_CHEST.get());
                output.accept(ModBlocks.COPPER_CHEST.get());
                output.accept(ModBlocks.IRON_CHEST.get());
                output.accept(ModBlocks.GOLD_CHEST.get());
                output.accept(ModBlocks.QUARTZ_CHEST.get());
                output.accept(ModBlocks.AMETHYST_CHEST.get());
                output.accept(ModBlocks.DIAMOND_CHEST.get());
                output.accept(ModBlocks.NETHERITE_CHEST.get());
                output.accept(ModBlocks.ADMIN_CHEST.get());
                output.accept(ModItems.COPPER_UPGRADE.get());
                output.accept(ModItems.IRON_UPGRADE.get());
                output.accept(ModItems.GOLD_UPGRADE.get());
                output.accept(ModItems.QUARTZ_UPGRADE.get());
                output.accept(ModItems.AMETHYST_UPGRADE.get());
                output.accept(ModItems.DIAMOND_UPGRADE.get());
                output.accept(ModItems.NETHERITE_UPGRADE.get());
            })
            .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
