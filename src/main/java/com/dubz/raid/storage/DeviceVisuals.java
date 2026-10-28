package com.dubz.raid.storage;

import com.dubz.raid.blockentity.RaidChassisBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class DeviceVisuals {
    private DeviceVisuals() {
    }

    public static DisplayStatus status(boolean running, NetworkGraph.Lookup lookup, boolean extraError) {
        if (lookup.status() == NetworkStatus.ERROR) {
            return DisplayStatus.ERROR;
        }
        boolean noRaid = lookup.status() != NetworkStatus.OK;
        if (running && (noRaid || extraError)) {
            return DisplayStatus.ERROR;
        }
        if (noRaid) {
            return DisplayStatus.DISCONNECTED;
        }
        return DisplayStatus.OK;
    }

    public static boolean full(NetworkGraph.Lookup lookup, RaidChassisBlockEntity chassis, long needBytes) {
        if (chassis == null || lookup.capacity() <= 0) {
            return true;
        }
        long used = chassis.storage().usedBytes();
        if (used > lookup.capacity()) {
            return true;
        }
        return lookup.capacity() - used < needBytes;
    }

    public static void apply(ServerLevel level, BlockPos pos, DisplayStatus want) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(DisplayStatus.PROPERTY) && state.getValue(DisplayStatus.PROPERTY) != want) {
            level.setBlock(pos, state.setValue(DisplayStatus.PROPERTY, want),
                    Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ON_PLACE);
        }
    }
}
