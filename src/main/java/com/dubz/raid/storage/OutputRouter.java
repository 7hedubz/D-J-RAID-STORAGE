package com.dubz.raid.storage;

import com.dubz.raid.block.DataCableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class OutputRouter {
    public record Dest(BlockPos pos, Direction side) {
    }

    private OutputRouter() {
    }

    public static List<Dest> collect(ServerLevel level, BlockPos machine, Direction out, Predicate<BlockState> acceptable) {
        List<Dest> dests = new ArrayList<>();
        BlockPos next = machine.relative(out);
        BlockState nextState = level.getBlockState(next);
        if (nextState.getBlock() instanceof DataCableBlock) {
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            Set<BlockPos> seen = new HashSet<>();
            queue.add(next);
            seen.add(next.immutable());
            while (!queue.isEmpty()) {
                BlockPos cable = queue.removeFirst();
                for (Direction dir : Direction.values()) {
                    BlockPos near = cable.relative(dir);
                    if (!level.isLoaded(near) || !seen.add(near.immutable())) {
                        continue;
                    }
                    BlockState nearState = level.getBlockState(near);
                    if (nearState.getBlock() instanceof DataCableBlock) {
                        queue.add(near);
                        continue;
                    }
                    if (acceptable.test(nearState)) {
                        dests.add(new Dest(near.immutable(), dir.getOpposite()));
                    }
                }
            }
        } else if (acceptable.test(nextState)) {
            dests.add(new Dest(next.immutable(), out.getOpposite()));
        }
        dests.sort(Comparator.comparingLong(d -> d.pos.asLong()));
        return dests;
    }

    public static int nextIndex(int cursor, int size) {
        if (size <= 0) {
            return 0;
        }
        return Math.floorMod(cursor, size);
    }
}
