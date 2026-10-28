package com.dubz.raid.registry;

import com.dubz.raid.RaidMod;
import com.dubz.raid.storage.DigitizedStorage;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, RaidMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DigitizedStorage>> STORAGE =
            COMPONENTS.registerComponentType("storage", builder -> builder
                    .persistent(DigitizedStorage.CODEC)
                    .networkSynchronized(DigitizedStorage.STREAM_CODEC));

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}
