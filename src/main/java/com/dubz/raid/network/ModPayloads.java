package com.dubz.raid.network;

import com.dubz.raid.RaidMod;
import com.dubz.raid.menu.ChestMenu;
import com.dubz.raid.storage.DigitizedStorage;
import com.dubz.raid.storage.NetworkStatus;
import com.dubz.raid.storage.RaidSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

public final class ModPayloads {
    private ModPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToServer(ExtractPayload.TYPE, ExtractPayload.STREAM_CODEC, ExtractPayload::handle);
        registrar.playToServer(InsertCarriedPayload.TYPE, InsertCarriedPayload.STREAM_CODEC, InsertCarriedPayload::handle);
        registrar.playToClient(StorageSyncPayload.TYPE, StorageSyncPayload.STREAM_CODEC, StorageSyncPayload::handle);
        registrar.playToClient(AdminBytesPayload.TYPE, AdminBytesPayload.STREAM_CODEC, AdminBytesPayload::handle);
    }

    public record ExtractPayload(BlockPos chestPos, ItemStack template, long amount, boolean toHand) implements CustomPacketPayload {
        public static final Type<ExtractPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(RaidMod.MOD_ID, "extract"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ExtractPayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, ExtractPayload::chestPos,
                ItemStack.STREAM_CODEC, ExtractPayload::template,
                ByteBufCodecs.VAR_LONG, ExtractPayload::amount,
                ByteBufCodecs.BOOL, ExtractPayload::toHand,
                ExtractPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ExtractPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player
                        && player.containerMenu instanceof ChestMenu menu
                        && menu.chestPos().equals(payload.chestPos)) {
                    menu.handleExtract(player, payload.template, payload.amount, payload.toHand);
                }
            });
        }
    }

    public record InsertCarriedPayload(BlockPos chestPos) implements CustomPacketPayload {
        public static final Type<InsertCarriedPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(RaidMod.MOD_ID, "insert_carried"));
        public static final StreamCodec<RegistryFriendlyByteBuf, InsertCarriedPayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, InsertCarriedPayload::chestPos,
                InsertCarriedPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(InsertCarriedPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player
                        && player.containerMenu instanceof ChestMenu menu
                        && menu.chestPos().equals(payload.chestPos)) {
                    menu.handleInsertCarried(player);
                }
            });
        }
    }

    public record StorageSyncPayload(
            NetworkStatus status,
            long used,
            long capacity,
            List<ItemStack> items,
            List<Long> counts
    ) implements CustomPacketPayload {
        public static final Type<StorageSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(RaidMod.MOD_ID, "storage_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StorageSyncPayload> STREAM_CODEC = StreamCodec.of(
                StorageSyncPayload::encode,
                StorageSyncPayload::decode
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private static void encode(RegistryFriendlyByteBuf buf, StorageSyncPayload payload) {
            buf.writeEnum(payload.status);
            buf.writeVarLong(payload.used);
            buf.writeVarLong(payload.capacity);
            buf.writeVarInt(payload.items.size());
            for (int i = 0; i < payload.items.size(); i++) {
                ItemStack.STREAM_CODEC.encode(buf, payload.items.get(i));
                buf.writeVarLong(payload.counts.get(i));
            }
        }

        private static StorageSyncPayload decode(RegistryFriendlyByteBuf buf) {
            NetworkStatus status = buf.readEnum(NetworkStatus.class);
            long used = buf.readVarLong();
            long capacity = buf.readVarLong();
            int size = buf.readVarInt();
            List<ItemStack> items = new ArrayList<>(size);
            List<Long> counts = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                items.add(ItemStack.STREAM_CODEC.decode(buf));
                counts.add(buf.readVarLong());
            }
            return new StorageSyncPayload(status, used, capacity, items, counts);
        }

        public static StorageSyncPayload from(NetworkStatus status, long used, long capacity, DigitizedStorage storage) {
            List<ItemStack> items = new ArrayList<>();
            List<Long> counts = new ArrayList<>();
            if (storage != null) {
                for (DigitizedStorage.Entry entry : storage.entries()) {
                    items.add(entry.stack.copy());
                    counts.add(entry.count);
                }
            }
            return new StorageSyncPayload(status, used, capacity, items, counts);
        }

        public static void handle(StorageSyncPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player().containerMenu instanceof ChestMenu menu) {
                    menu.acceptSync(payload);
                }
            });
        }
    }

    public record AdminBytesPayload(long bytes) implements CustomPacketPayload {
        public static final Type<AdminBytesPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(RaidMod.MOD_ID, "admin_bytes"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AdminBytesPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, AdminBytesPayload::bytes,
                AdminBytesPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(AdminBytesPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> RaidSettings.applyClient(payload.bytes));
        }
    }
}
