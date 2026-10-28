package com.dubz.raid.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class MachineSounds {
    private MachineSounds() {
    }

    public static void play(Level level, BlockPos pos, DeferredHolder<SoundEvent, SoundEvent> sound) {
        if (level instanceof ServerLevel server) {
            server.playSound(null, pos, sound.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }
}
