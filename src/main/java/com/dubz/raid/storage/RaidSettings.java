package com.dubz.raid.storage;

import com.dubz.raid.network.ModPayloads;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class RaidSettings extends SavedData {
    public static final long DEFAULT_ADMIN_BYTES = 101376L;

    public static final SavedDataType<RaidSettings> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("raid", "settings"),
            RaidSettings::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.optionalFieldOf("admin_bytes", DEFAULT_ADMIN_BYTES).forGetter(s -> s.adminBytes)
            ).apply(instance, RaidSettings::new))
    );

    private static long liveAdminBytes = DEFAULT_ADMIN_BYTES;

    private long adminBytes;

    public RaidSettings() {
        this(DEFAULT_ADMIN_BYTES);
    }

    public RaidSettings(long adminBytes) {
        this.adminBytes = Math.max(1, adminBytes);
        liveAdminBytes = this.adminBytes;
    }

    public static long adminBytes() {
        return liveAdminBytes;
    }

    public static void applyClient(long bytes) {
        liveAdminBytes = Math.max(1, bytes);
    }

    public static RaidSettings get(MinecraftServer server) {
        RaidSettings settings = server.overworld().getDataStorage().computeIfAbsent(TYPE);
        liveAdminBytes = settings.adminBytes;
        return settings;
    }

    public static void setAdminBytes(MinecraftServer server, long bytes) {
        RaidSettings settings = get(server);
        settings.adminBytes = Math.max(1, bytes);
        liveAdminBytes = settings.adminBytes;
        settings.setDirty();
        sync(server);
        for (ServerLevel level : server.getAllLevels()) {
            NetworkGraph.get(level).refreshVisuals(level);
        }
    }

    public static void sync(MinecraftServer server) {
        PacketDistributor.sendToAllPlayers(new ModPayloads.AdminBytesPayload(liveAdminBytes));
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ModPayloads.AdminBytesPayload(liveAdminBytes));
    }

    public static void onServerStarted(ServerStartedEvent event) {
        get(event.getServer());
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            get(player.level().getServer());
            sync(player);
        }
    }
}
