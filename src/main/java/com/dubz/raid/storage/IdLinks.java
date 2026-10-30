package com.dubz.raid.storage;

import com.dubz.raid.registry.ModDataComponents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IdLinks extends SavedData {
    public static final SavedDataType<IdLinks> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("raid", "id_links"),
            IdLinks::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("next").forGetter(data -> data.nextId),
                    Codec.list(Endpoint.CODEC).fieldOf("ends").forGetter(IdLinks::endpointList)
            ).apply(instance, IdLinks::from))
    );

    private int nextId = 1;
    private final List<Endpoint> ends = new ArrayList<>();
    private final Set<Integer> doomed = new HashSet<>();

    public IdLinks() {
    }

    private IdLinks(int nextId, List<Endpoint> ends) {
        this.nextId = Math.max(1, nextId);
        this.ends.addAll(ends);
    }

    public static IdLinks from(int nextId, List<Endpoint> ends) {
        return new IdLinks(nextId, ends);
    }

    public static IdLinks get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static IdLinks get(ServerLevel level) {
        return get(level.getServer());
    }

    private List<Endpoint> endpointList() {
        return List.copyOf(ends);
    }

    public int allocate() {
        int id = nextId++;
        setDirty();
        return id;
    }

    public void placed(ServerLevel level, BlockPos pos, int pairId) {
        if (pairId <= 0) {
            return;
        }
        BlockPos imm = pos.immutable();
        ResourceKey<Level> dim = level.dimension();
        ends.removeIf(end -> end.pairId == pairId && end.dim.equals(dim) && end.pos.equals(imm));
        ends.add(new Endpoint(dim, imm, pairId));
        setDirty();
    }

    public void unplace(ServerLevel level, BlockPos pos) {
        BlockPos imm = pos.immutable();
        ResourceKey<Level> dim = level.dimension();
        if (ends.removeIf(end -> end.dim.equals(dim) && end.pos.equals(imm))) {
            setDirty();
        }
    }

    public int pairAt(ServerLevel level, BlockPos pos) {
        BlockPos imm = pos.immutable();
        ResourceKey<Level> dim = level.dimension();
        for (Endpoint end : ends) {
            if (end.dim.equals(dim) && end.pos.equals(imm)) {
                return end.pairId;
            }
        }
        return 0;
    }

    public List<Endpoint> endsOf(int pairId) {
        List<Endpoint> out = new ArrayList<>();
        for (Endpoint end : ends) {
            if (end.pairId == pairId) {
                out.add(end);
            }
        }
        return out;
    }

    public boolean isDoomed(int pairId) {
        return pairId > 0 && doomed.contains(pairId);
    }

    public void onIrrevocableLoss(MinecraftServer server, int pairId) {
        if (pairId <= 0 || !doomed.add(pairId)) {
            return;
        }
        setDirty();
        for (Endpoint end : List.copyOf(endsOf(pairId))) {
            ServerLevel dim = server.getLevel(end.dim);
            if (dim == null || !dim.isLoaded(end.pos)) {
                continue;
            }
            explode(dim, end.pos.getX() + 0.5, end.pos.getY() + 0.5, end.pos.getZ() + 0.5);
            dim.removeBlock(end.pos, false);
            unplace(dim, end.pos);
        }
        for (ServerLevel dim : server.getAllLevels()) {
            for (Player player : List.copyOf(dim.players())) {
                if (stripFromPlayer(player, pairId)) {
                    explode(dim, player.getX(), player.getY(), player.getZ());
                }
            }
        }
    }

    public static boolean stripFromPlayer(Player player, int pairId) {
        boolean found = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (pairIdOf(stack) == pairId) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
                found = true;
            }
        }
        return found;
    }

    public static void explode(ServerLevel level, double x, double y, double z) {
        level.explode(null, x, y, z, 4.0F, Level.ExplosionInteraction.TNT);
    }

    public static int pairIdOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        Integer id = stack.get(ModDataComponents.ID_PAIR.get());
        return id == null ? 0 : id;
    }

    public static int pairIdOf(ItemEntity entity) {
        return pairIdOf(entity.getItem());
    }

    public record Endpoint(ResourceKey<Level> dim, BlockPos pos, int pairId) {
        public static final Codec<Endpoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dim").forGetter(Endpoint::dim),
                BlockPos.CODEC.fieldOf("pos").forGetter(Endpoint::pos),
                Codec.INT.fieldOf("id").forGetter(Endpoint::pairId)
        ).apply(instance, Endpoint::new));
    }
}
