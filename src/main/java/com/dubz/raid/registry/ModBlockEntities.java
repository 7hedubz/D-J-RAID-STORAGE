package com.dubz.raid.registry;

import com.dubz.raid.RaidMod;
import com.dubz.raid.blockentity.RaidChassisBlockEntity;
import com.dubz.raid.blockentity.RaidHopperBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, RaidMod.MOD_ID);

    public static final Supplier<BlockEntityType<RaidChassisBlockEntity>> RAID_CHASSIS =
            BLOCK_ENTITIES.register("raid_chassis", () -> new BlockEntityType<>(
                    RaidChassisBlockEntity::new,
                    ModBlocks.RAID_CHASSIS.get()
            ));

    public static final Supplier<BlockEntityType<RaidHopperBlockEntity>> HOPPER =
            BLOCK_ENTITIES.register("hopper", () -> new BlockEntityType<>(
                    RaidHopperBlockEntity::new,
                    ModBlocks.HOPPER.get()
            ));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
