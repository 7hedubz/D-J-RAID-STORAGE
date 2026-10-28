package com.dubz.raid.registry;

import com.dubz.raid.RaidMod;
import com.dubz.raid.menu.ChestMenu;
import com.dubz.raid.menu.CollectorMenu;
import com.dubz.raid.menu.CrafterMenu;
import com.dubz.raid.menu.HopperMenu;
import com.dubz.raid.menu.LookerMenu;
import com.dubz.raid.menu.TreeFarmMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, RaidMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ChestMenu>> CHEST =
            MENUS.register("chest", () -> IMenuTypeExtension.create(ChestMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<HopperMenu>> HOPPER =
            MENUS.register("hopper", () -> IMenuTypeExtension.create(HopperMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CrafterMenu>> AUTO_CRAFTER =
            MENUS.register("auto_crafter", () -> IMenuTypeExtension.create(CrafterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<TreeFarmMenu>> TREE_FARM =
            MENUS.register("tree_farm", () -> IMenuTypeExtension.create(TreeFarmMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CollectorMenu>> COLLECTOR =
            MENUS.register("collector", () -> IMenuTypeExtension.create(CollectorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<LookerMenu>> LOOKER =
            MENUS.register("looker", () -> IMenuTypeExtension.create(LookerMenu::new));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
