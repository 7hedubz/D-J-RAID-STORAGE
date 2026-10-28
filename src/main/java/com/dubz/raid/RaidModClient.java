package com.dubz.raid;

import com.dubz.raid.client.RaidCrafterRenderer;
import com.dubz.raid.menu.ChestScreen;
import com.dubz.raid.menu.CrafterScreen;
import com.dubz.raid.menu.HopperScreen;
import com.dubz.raid.registry.ModBlockEntities;
import com.dubz.raid.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = RaidMod.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = RaidMod.MOD_ID, value = Dist.CLIENT)
public class RaidModClient {
    public RaidModClient(ModContainer container) {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.CHEST.get(), ChestScreen::new);
        event.register(ModMenus.HOPPER.get(), HopperScreen::new);
        event.register(ModMenus.AUTO_CRAFTER.get(), CrafterScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.AUTO_CRAFTER.get(), RaidCrafterRenderer::new);
    }
}
