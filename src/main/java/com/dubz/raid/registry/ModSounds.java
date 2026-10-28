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
    public static final DeferredHolder<SoundEvent, SoundEvent> CHEST_OPEN = event("block.chest.open");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHEST_CLOSE = event("block.chest.close");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOPPER_ON = event("block.hopper.on");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOPPER_OFF = event("block.hopper.off");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRAFTER_ON = event("block.crafter.on");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRAFTER_OFF = event("block.crafter.off");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREE_FARM_ON = event("block.tree_farm.on");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREE_FARM_OFF = event("block.tree_farm.off");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLLECTOR_ON = event("block.collector.on");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLLECTOR_OFF = event("block.collector.off");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOKER_ON = event("block.looker.on");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOKER_OFF = event("block.looker.off");

    private static DeferredHolder<SoundEvent, SoundEvent> event(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(RaidMod.MOD_ID, path)));
    }

    public static SoundType cable() {
        return new SoundType(1.0F, 1.0F, CABLE_BREAK.get(), CABLE_STEP.get(), CABLE_PLACE.get(), CABLE_HIT.get(), CABLE_FALL.get());
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
