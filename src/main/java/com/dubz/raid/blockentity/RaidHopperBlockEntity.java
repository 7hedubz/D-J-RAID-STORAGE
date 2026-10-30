package com.dubz.raid.blockentity;

import com.dubz.raid.block.DataCableBlock;
import com.dubz.raid.block.RaidConnect;
import com.dubz.raid.block.RaidHopperBlock;
import com.dubz.raid.menu.HopperMenu;
import com.dubz.raid.registry.ModBlockEntities;
import com.dubz.raid.storage.LinkedRaid;
import com.dubz.raid.storage.OutputRouter;
import com.dubz.raid.storage.NetworkGraph;
import com.dubz.raid.storage.NetworkStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class RaidHopperBlockEntity extends BlockEntity implements MenuProvider {
    private final SimpleContainer filter = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            RaidHopperBlockEntity.this.setChanged();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    };
    private int cooldown;
    private int rr;

    public RaidHopperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HOPPER.get(), pos, state);
    }

    public Container filter() {
        return filter;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) {
            Containers.dropContents(this.level, pos, filter);
        }
        super.preRemoveSideEffects(pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RaidHopperBlockEntity entity) {
        if (level.isClientSide()) {
            return;
        }
        entity.cooldown--;
        if (entity.cooldown > 0) {
            return;
        }
        entity.cooldown = HopperBlockEntity.MOVE_ITEM_SPEED;
        entity.tryMove((ServerLevel) level, pos, state);
    }

    private void tryMove(ServerLevel level, BlockPos pos, BlockState state) {
        ItemStack slot = filter.getItem(0);
        if (slot.isEmpty()) {
            return;
        }
        NetworkGraph.Lookup lookup = NetworkGraph.get(level).lookupDevice(pos);
        if (lookup.status() == NetworkStatus.ERROR) {
            return;
        }
        if (slot.getCount() > 1 && pushOne(level, pos, state, slot)) {
            slot.shrink(1);
            filter.setChanged();
            return;
        }
        if (lookup.status() == NetworkStatus.DISCONNECTED || lookup.chassisPos() == null
                || !(level.getBlockEntity(lookup.chassisPos()) instanceof RaidChassisBlockEntity chassis)) {
            return;
        }
        ItemStack extracted = LinkedRaid.extract(level, chassis.getBlockPos(), slot, 1);
        if (extracted.isEmpty()) {
            return;
        }
        if (pushOne(level, pos, state, extracted)) {
            return;
        }
        long back = chassis.insert(extracted, Math.max(lookup.capacity(), chassis.storage().usedBytes() + 16));
        if (back < extracted.getCount()) {
            extracted.shrink((int) back);
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), extracted);
        }
    }

    private boolean pushOne(ServerLevel level, BlockPos pos, BlockState state, ItemStack stack) {
        Direction out = RaidHopperBlock.outputFace(state);
        List<OutputRouter.Dest> dests = OutputRouter.collect(level, pos, out, RaidHopperBlockEntity::looksPushable);
        if (dests.isEmpty()) {
            return false;
        }
        int start = OutputRouter.nextIndex(rr, dests.size());
        for (int i = 0; i < dests.size(); i++) {
            OutputRouter.Dest dest = dests.get((start + i) % dests.size());
            BlockState destState = level.getBlockState(dest.pos());
            if (insertAt(level, dest.pos(), destState, dest.side(), stack)) {
                rr = start + i + 1;
                return true;
            }
        }
        return false;
    }

    private static boolean looksPushable(BlockState destState) {
        if (RaidConnect.isCable(destState) || destState.getBlock() instanceof RaidHopperBlock
                || destState.getBlock() instanceof com.dubz.raid.block.RaidCrafterBlock) {
            return false;
        }
        if (destState.getBlock() instanceof com.dubz.raid.block.RaidChassisBlock
                || destState.getBlock() instanceof com.dubz.raid.block.RaidChestBlock) {
            return true;
        }
        return RaidConnect.isItemInventory(destState);
    }

    private boolean insertAt(ServerLevel level, BlockPos dest, BlockState destState, Direction side, ItemStack stack) {
        if (RaidConnect.isCable(destState) || destState.getBlock() instanceof RaidHopperBlock
                || destState.getBlock() instanceof com.dubz.raid.block.RaidCrafterBlock) {
            return false;
        }
        if (destState.getBlock() instanceof com.dubz.raid.block.RaidChassisBlock
                || destState.getBlock() instanceof com.dubz.raid.block.RaidChestBlock) {
            ResourceHandler<ItemResource> raid = com.dubz.raid.storage.CableItemHandler.create(
                    level, dest, destState, level.getBlockEntity(dest), side);
            if (raid == null) {
                return false;
            }
            ItemResource resource = ItemResource.of(stack);
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = raid.insert(resource, 1, transaction);
                if (inserted != 1) {
                    return false;
                }
                transaction.commit();
                return true;
            }
        }
        if (!RaidConnect.acceptsInsertOnFace(destState, side)) {
            return false;
        }
        ResourceHandler<ItemResource> handler = level.getCapability(
                Capabilities.Item.BLOCK, dest, destState, level.getBlockEntity(dest), side);
        if (handler == null) {
            return false;
        }
        ItemResource resource = ItemResource.of(stack);
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = handler.insert(resource, 1, transaction);
            if (inserted != 1) {
                return false;
            }
            transaction.commit();
            return true;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ItemStack stack = filter.getItem(0);
        if (!stack.isEmpty()) {
            output.store("filter", ItemStack.CODEC, stack);
        }
        output.putInt("cooldown", cooldown);
        output.putInt("rr", rr);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        filter.setItem(0, input.read("filter", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        cooldown = input.getIntOr("cooldown", 0);
        rr = input.getIntOr("rr", 0);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.raid.hopper");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new HopperMenu(id, inventory, this);
    }
}
