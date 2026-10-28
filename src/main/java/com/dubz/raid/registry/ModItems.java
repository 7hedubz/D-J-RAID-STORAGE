package com.dubz.raid.registry;

import com.dubz.raid.RaidMod;
import com.dubz.raid.item.ChestUpgradeItem;
import com.dubz.raid.storage.ChestTier;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RaidMod.MOD_ID);

    public static final DeferredItem<ChestUpgradeItem> COPPER_UPGRADE = upgrade("copper_upgrade", ChestTier.STONE, ChestTier.COPPER);
    public static final DeferredItem<ChestUpgradeItem> IRON_UPGRADE = upgrade("iron_upgrade", ChestTier.COPPER, ChestTier.IRON);
    public static final DeferredItem<ChestUpgradeItem> GOLD_UPGRADE = upgrade("gold_upgrade", ChestTier.IRON, ChestTier.GOLD);
    public static final DeferredItem<ChestUpgradeItem> QUARTZ_UPGRADE = upgrade("quartz_upgrade", ChestTier.GOLD, ChestTier.QUARTZ);
    public static final DeferredItem<ChestUpgradeItem> AMETHYST_UPGRADE = upgrade("amethyst_upgrade", ChestTier.QUARTZ, ChestTier.AMETHYST);
    public static final DeferredItem<ChestUpgradeItem> DIAMOND_UPGRADE = upgrade("diamond_upgrade", ChestTier.AMETHYST, ChestTier.DIAMOND);
    public static final DeferredItem<ChestUpgradeItem> NETHERITE_UPGRADE = upgrade("netherite_upgrade", ChestTier.DIAMOND, ChestTier.NETHERITE);

    private static DeferredItem<ChestUpgradeItem> upgrade(String name, ChestTier from, ChestTier to) {
        return ITEMS.registerItem(name, props -> new ChestUpgradeItem(props, from, to));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
