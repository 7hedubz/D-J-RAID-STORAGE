package com.dubz.raid.storage;

import com.dubz.raid.blockentity.IdCableBlockEntity;
import com.dubz.raid.blockentity.RaidChassisBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class LinkedRaid {
    private LinkedRaid() {
    }

    public static List<RaidChassisBlockEntity> pullOrder(ServerLevel local, BlockPos localChassis) {
        List<ChassisRef> refs = linkedChassis(local, localChassis);
        List<RaidChassisBlockEntity> out = new ArrayList<>();
        for (ChassisRef ref : refs) {
            out.add(ref.chassis);
        }
        return out;
    }

    public static long countOf(ServerLevel level, BlockPos localChassis, ItemStack template) {
        long total = 0;
        for (RaidChassisBlockEntity chassis : pullOrder(level, localChassis)) {
            total += chassis.storage().countOf(template);
        }
        return total;
    }

    public static ItemStack extract(ServerLevel level, BlockPos localChassis, ItemStack template, long amount) {
        for (RaidChassisBlockEntity chassis : pullOrder(level, localChassis)) {
            ItemStack out = chassis.extract(template, amount);
            if (!out.isEmpty()) {
                return out;
            }
        }
        return ItemStack.EMPTY;
    }

    public static DigitizedStorage mergedContents(ServerLevel level, BlockPos localChassis) {
        List<DigitizedStorage.Entry> merged = new ArrayList<>();
        for (RaidChassisBlockEntity chassis : pullOrder(level, localChassis)) {
            boolean local = chassis.getBlockPos().equals(localChassis) && chassis.getLevel() == level;
            for (DigitizedStorage.Entry entry : chassis.storage().entries()) {
                boolean found = false;
                for (DigitizedStorage.Entry existing : merged) {
                    if (ItemStack.isSameItemSameComponents(existing.stack, entry.stack)) {
                        existing.count += entry.count;
                        if (local) {
                            existing.localCount += entry.count;
                        }
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    merged.add(new DigitizedStorage.Entry(entry.stack, entry.count, local ? entry.count : 0));
                }
            }
        }
        return new DigitizedStorage(merged);
    }

    private static List<ChassisRef> linkedChassis(ServerLevel local, BlockPos localChassis) {
        MinecraftServer server = local.getServer();
        Set<String> seenChassis = new LinkedHashSet<>();
        List<ChassisRef> found = new ArrayList<>();
        List<ChassisRef> queue = new ArrayList<>();
        addChassis(queue, seenChassis, local, localChassis);
        for (int i = 0; i < queue.size(); i++) {
            ChassisRef ref = queue.get(i);
            found.add(ref);
            NetworkGraph graph = NetworkGraph.get(ref.level);
            for (BlockPos cable : graph.cablesFromDevice(ref.pos)) {
                if (!(ref.level.getBlockEntity(cable) instanceof IdCableBlockEntity idCable)) {
                    continue;
                }
                int pairId = idCable.pairId();
                if (pairId <= 0) {
                    continue;
                }
                for (IdLinks.Endpoint end : IdLinks.get(server).endsOf(pairId)) {
                    ServerLevel dim = server.getLevel(end.dim());
                    if (dim == null) {
                        continue;
                    }
                    ensureChunk(dim, end.pos());
                    NetworkGraph.Lookup lookup = NetworkGraph.get(dim).lookupCable(end.pos());
                    if (lookup.status() != NetworkStatus.OK || lookup.chassisPos() == null) {
                        continue;
                    }
                    ensureChunk(dim, lookup.chassisPos());
                    addChassis(queue, seenChassis, dim, lookup.chassisPos());
                }
            }
        }
        return sortPullOrder(local, localChassis, found);
    }

    private static void ensureChunk(ServerLevel level, BlockPos pos) {
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static void addChassis(List<ChassisRef> queue, Set<String> seen, ServerLevel level, BlockPos pos) {
        String key = level.dimension().identifier() + "@" + pos.asLong();
        if (!seen.add(key)) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof RaidChassisBlockEntity chassis)) {
            return;
        }
        queue.add(new ChassisRef(level, pos.immutable(), chassis));
    }

    private static List<ChassisRef> sortPullOrder(ServerLevel local, BlockPos localPos, List<ChassisRef> all) {
        ResourceKey<Level> here = local.dimension();
        List<ChassisRef> result = new ArrayList<>();
        ChassisRef localRef = null;
        List<ChassisRef> sameDim = new ArrayList<>();
        List<ChassisRef> overworld = new ArrayList<>();
        List<ChassisRef> nether = new ArrayList<>();
        List<ChassisRef> end = new ArrayList<>();
        List<ChassisRef> other = new ArrayList<>();
        for (ChassisRef ref : all) {
            if (ref.level.dimension().equals(here) && ref.pos.equals(localPos)) {
                localRef = ref;
            } else if (ref.level.dimension().equals(here)) {
                sameDim.add(ref);
            } else if (ref.level.dimension().equals(Level.OVERWORLD)) {
                overworld.add(ref);
            } else if (ref.level.dimension().equals(Level.NETHER)) {
                nether.add(ref);
            } else if (ref.level.dimension().equals(Level.END)) {
                end.add(ref);
            } else {
                other.add(ref);
            }
        }
        sameDim.sort(Comparator.comparingDouble(ref -> ref.pos.distSqr(localPos)));
        Comparator<ChassisRef> origin = Comparator.comparingDouble(ref -> ref.pos.distSqr(BlockPos.ZERO));
        overworld.sort(origin);
        nether.sort(origin);
        end.sort(origin);
        other.sort(origin.thenComparing(ref -> ref.level.dimension().identifier().toString()));
        if (localRef != null) {
            result.add(localRef);
        }
        result.addAll(sameDim);
        if (!here.equals(Level.OVERWORLD)) {
            result.addAll(overworld);
        }
        if (!here.equals(Level.NETHER)) {
            result.addAll(nether);
        }
        if (!here.equals(Level.END)) {
            result.addAll(end);
        }
        result.addAll(other);
        return result;
    }

    private record ChassisRef(ServerLevel level, BlockPos pos, RaidChassisBlockEntity chassis) {
    }
}
