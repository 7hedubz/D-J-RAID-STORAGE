package com.dubz.raid.storage;

import com.dubz.raid.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;

import java.util.Locale;

public enum ChestTier implements StringRepresentable {
    STONE(512),
    COPPER(1024),
    IRON(2048),
    GOLD(3072),
    QUARTZ(4096),
    AMETHYST(5120),
    DIAMOND(6144),
    NETHERITE(9216),
    ADMIN(101376);

    public static final Codec<ChestTier> CODEC = StringRepresentable.fromEnum(ChestTier::values);

    private final int bytes;

    ChestTier(int bytes) {
        this.bytes = bytes;
    }

    public long bytes() {
        if (this == ADMIN) {
            return RaidSettings.adminBytes();
        }
        return bytes;
    }

    public int order() {
        return ordinal();
    }

    public ChestTier next() {
        int i = ordinal() + 1;
        return i < values().length ? values()[i] : this;
    }

    public boolean isMax() {
        return this == ADMIN;
    }

    public Block block() {
        return switch (this) {
            case STONE -> ModBlocks.STONE_CHEST.get();
            case COPPER -> ModBlocks.COPPER_CHEST.get();
            case IRON -> ModBlocks.IRON_CHEST.get();
            case GOLD -> ModBlocks.GOLD_CHEST.get();
            case QUARTZ -> ModBlocks.QUARTZ_CHEST.get();
            case AMETHYST -> ModBlocks.AMETHYST_CHEST.get();
            case DIAMOND -> ModBlocks.DIAMOND_CHEST.get();
            case NETHERITE -> ModBlocks.NETHERITE_CHEST.get();
            case ADMIN -> ModBlocks.ADMIN_CHEST.get();
        };
    }

    public static ChestTier fromBlock(Block block) {
        for (ChestTier tier : values()) {
            if (tier.block() == block) {
                return tier;
            }
        }
        return null;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
