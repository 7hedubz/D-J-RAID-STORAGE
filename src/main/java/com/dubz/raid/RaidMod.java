package com.dubz.raid;

import com.dubz.raid.network.ModPayloads;
import com.dubz.raid.registry.ModBlockEntities;
import com.dubz.raid.registry.ModBlocks;
import com.dubz.raid.registry.ModCreativeTabs;
import com.dubz.raid.registry.ModDataComponents;
import com.dubz.raid.registry.ModItems;
import com.dubz.raid.registry.ModMenus;
import com.dubz.raid.registry.ModSounds;
import com.dubz.raid.storage.CableItemHandler;
import com.dubz.raid.storage.RaidSettings;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(RaidMod.MOD_ID)
public class RaidMod {
    public static final String MOD_ID = "raid";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RaidMod(IEventBus modEventBus, ModContainer modContainer) {
        ModDataComponents.register(modEventBus);
        ModSounds.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        modEventBus.addListener(ModPayloads::register);
        modEventBus.addListener(this::registerCapabilities);
        NeoForge.EVENT_BUS.addListener(RaidCommands::register);
        NeoForge.EVENT_BUS.addListener(RaidSettings::onServerStarted);
        NeoForge.EVENT_BUS.addListener(RaidSettings::onPlayerLogin);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.Item.BLOCK, CableItemHandler::create, ModBlocks.DATA_CABLE.get());
        event.registerBlock(Capabilities.Item.BLOCK, CableItemHandler::create, ModBlocks.RAID_CHASSIS.get());
        event.registerBlock(Capabilities.Item.BLOCK, CableItemHandler::create, ModBlocks.HOPPER.get());
        event.registerBlock(Capabilities.Item.BLOCK, CableItemHandler::create,
                ModBlocks.STONE_CHEST.get(),
                ModBlocks.COPPER_CHEST.get(),
                ModBlocks.IRON_CHEST.get(),
                ModBlocks.GOLD_CHEST.get(),
                ModBlocks.QUARTZ_CHEST.get(),
                ModBlocks.AMETHYST_CHEST.get(),
                ModBlocks.DIAMOND_CHEST.get(),
                ModBlocks.NETHERITE_CHEST.get(),
                ModBlocks.ADMIN_CHEST.get());
    }
}
