package com.dubz.raid.storage;

import com.dubz.raid.blockentity.RaidChassisBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Hopper (and other automation) insert target on a data cable.
 * Digitizes into the connected RAID Chassis when the cable graph is valid and has room.
 */
public final class CableItemHandler implements ResourceHandler<ItemResource> {
    private final Level level;
    private final BlockPos pos;

    public CableItemHandler(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    public static @Nullable ResourceHandler<ItemResource> create(Level level, BlockPos pos, BlockState state,
                                                                @Nullable BlockEntity blockEntity, @Nullable Direction side) {
        return new CableItemHandler(level, pos);
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public ItemResource getResource(int index) {
        Objects.checkIndex(index, size());
        return ItemResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        Objects.checkIndex(index, size());
        return 0;
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        Objects.checkIndex(index, size());
        Target target = resolve();
        if (target == null || (!resource.isEmpty() && !RaidMath.canDigitize(resource.toStack()))) {
            return 0;
        }
        long remaining = target.capacity - target.chassis.storage().usedBytes();
        return Math.max(0, remaining);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmpty(resource);
        return resolve() != null && RaidMath.canDigitize(resource.toStack());
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount <= 0) {
            return 0;
        }
        Target target = resolve();
        if (target == null || !RaidMath.canDigitize(resource.toStack())) {
            return 0;
        }
        ItemStack stack = resource.toStack(amount);
        long inserted = target.chassis.insert(stack, target.capacity, transaction);
        return (int) Math.min(Integer.MAX_VALUE, inserted);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return 0;
    }

    private @Nullable Target resolve() {
        if (!(level instanceof ServerLevel server)) {
            return null;
        }
        NetworkGraph.Lookup lookup = NetworkGraph.get(server).lookupCable(pos);
        if (lookup.status() != NetworkStatus.OK || lookup.chassisPos() == null || lookup.capacity() <= 0) {
            lookup = NetworkGraph.get(server).lookupDevice(pos);
        }
        if (lookup.status() != NetworkStatus.OK || lookup.chassisPos() == null || lookup.capacity() <= 0) {
            return null;
        }
        if (!(server.getBlockEntity(lookup.chassisPos()) instanceof RaidChassisBlockEntity chassis)) {
            return null;
        }
        if (chassis.storage().usedBytes() > lookup.capacity()) {
            return null;
        }
        return new Target(chassis, lookup.capacity());
    }

    private record Target(RaidChassisBlockEntity chassis, long capacity) {
    }
}
