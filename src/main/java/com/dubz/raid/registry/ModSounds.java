package com.dubz.raid.registry;

import com.dubz.raid.RaidMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, RaidMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> CABLE_BREAK = event("block.cable.break");
    public static final DeferredHolder<SoundEvent, SoundEvent> CABLE_STEP = event("block.cable.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> CABLE_PLACE = event("block.cable.place");
    public static final DeferredHolder<SoundEvent, SoundEvent> CABLE_HIT = event("block.cable.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> CABLE_FALL = event("block.cable.fall");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHEST_OPEN = chestEvent("block.chest.open");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHEST_CLOSE = chestEvent("block.chest.close");

    private static DeferredHolder<SoundEvent, SoundEvent> event(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(RaidMod.MOD_ID, path)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> chestEvent(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createFixedRangeEvent(Identifier.fromNamespaceAndPath(RaidMod.MOD_ID, path), 16.0F));
    }

    public static SoundType cable() {
        return new SoundType(1.0F, 1.0F, CABLE_BREAK.get(), CABLE_STEP.get(), CABLE_PLACE.get(), CABLE_HIT.get(), CABLE_FALL.get());
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
