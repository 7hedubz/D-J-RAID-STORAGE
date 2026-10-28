package com.dubz.raid.blockentity;

import com.dubz.raid.registry.ModBlockEntities;
import com.dubz.raid.registry.ModDataComponents;
import com.dubz.raid.storage.DigitizedStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public class RaidChassisBlockEntity extends BlockEntity {
    private DigitizedStorage storage = new DigitizedStorage();
    private int revision;
    private final SnapshotJournal<DigitizedStorage> journal = new SnapshotJournal<>() {
        @Override
        protected DigitizedStorage createSnapshot() {
            return storage.copy();
        }

        @Override
        protected void revertToSnapshot(DigitizedStorage snapshot) {
            storage = snapshot;
        }

        @Override
        protected void onRootCommit(DigitizedStorage originalState) {
            bump();
        }
    };

    public RaidChassisBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RAID_CHASSIS.get(), pos, state);
    }

    public DigitizedStorage storage() {
        return storage;
    }

    public int revision() {
        return revision;
    }

    public boolean isEmpty() {
        return storage.isEmpty();
    }

    public long insert(ItemStack stack, long capacity) {
        long inserted = storage.insert(stack, capacity);
        if (inserted > 0) {
            bump();
        }
        return inserted;
    }

    public long insert(ItemStack stack, long capacity, TransactionContext transaction) {
        journal.updateSnapshots(transaction);
        return storage.insert(stack, capacity);
    }

    public ItemStack extract(ItemStack template, long amount) {
        ItemStack out = storage.extract(template, amount);
        if (!out.isEmpty()) {
            bump();
        }
        return out;
    }

    private void bump() {
        revision++;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("storage", DigitizedStorage.CODEC, storage);
        output.putInt("revision", revision);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.storage = input.read("storage", DigitizedStorage.CODEC).orElseGet(DigitizedStorage::new);
        this.revision = input.getIntOr("revision", 0);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!storage.isEmpty()) {
            components.set(ModDataComponents.STORAGE.get(), storage.copy());
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        DigitizedStorage stored = components.get(ModDataComponents.STORAGE.get());
        if (stored != null) {
            this.storage = stored.copy();
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("storage");
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
