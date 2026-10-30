package com.dubz.raid.blockentity;

import com.dubz.raid.registry.ModBlockEntities;
import com.dubz.raid.registry.ModDataComponents;
import com.dubz.raid.storage.IdLinks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class IdCableBlockEntity extends BlockEntity {
    private int pairId;
    private boolean linked;
    private int checkTimer;

    public IdCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ID_CABLE.get(), pos, state);
    }

    public int pairId() {
        return pairId;
    }

    public boolean linked() {
        return linked;
    }

    public void setPairId(int pairId) {
        this.pairId = pairId;
        setChanged();
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, IdCableBlockEntity be) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        IdLinks links = IdLinks.get(server);
        if (be.pairId > 0 && links.isDoomed(be.pairId)) {
            links.explodePlaced(server, pos, be.pairId);
            return;
        }
        be.checkTimer++;
        if (be.checkTimer < 20) {
            return;
        }
        be.checkTimer = 0;
        boolean now = be.pairId > 0 && links.pairHasValidArrays(server.getServer(), be.pairId);
        if (now != be.linked) {
            be.linked = now;
            be.setChanged();
            server.sendBlockUpdated(pos, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("pairId", pairId);
        output.putBoolean("linked", linked);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.pairId = input.getIntOr("pairId", 0);
        this.linked = input.getBooleanOr("linked", false);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (pairId > 0) {
            components.set(ModDataComponents.ID_PAIR.get(), pairId);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        Integer id = components.get(ModDataComponents.ID_PAIR.get());
        if (id != null) {
            this.pairId = id;
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("pairId");
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
