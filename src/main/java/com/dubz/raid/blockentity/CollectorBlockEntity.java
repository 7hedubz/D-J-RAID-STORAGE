package com.dubz.raid.blockentity;

import com.dubz.raid.block.CollectorBlock;
import com.dubz.raid.block.DataCableBlock;
import com.dubz.raid.block.RaidConnect;
import com.dubz.raid.registry.ModBlockEntities;
import com.dubz.raid.storage.DeviceVisuals;
import com.dubz.raid.storage.NetworkGraph;
import com.dubz.raid.storage.NetworkStatus;
import com.dubz.raid.storage.RaidMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectorBlockEntity extends BlockEntity implements MenuProvider {
    private boolean running;
    private int cooldown;
    private int cursor;

    public CollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COLLECTOR.get(), pos, state);
    }

    public boolean running() {
        return running;
    }

    public void toggle(Player player) {
        running = !running;
        player.sendOverlayMessage(Component.translatable(running ? "gui.raid.collector.on" : "gui.raid.collector.off"));
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CollectorBlockEntity entity) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        NetworkGraph.Lookup lookup = NetworkGraph.get(server).lookupDevice(pos);
        RaidChassisBlockEntity chassis = lookup.chassisPos() != null
                && server.getBlockEntity(lookup.chassisPos()) instanceof RaidChassisBlockEntity found ? found : null;
        List<BlockPos> cauldrons = entity.findCauldrons(server, pos, state);
        boolean noCauldron = cauldrons.isEmpty();
        ItemStack filledProbe = new ItemStack(Items.WATER_BUCKET);
        boolean full = DeviceVisuals.full(lookup, chassis, RaidMath.bytesFor(filledProbe, 1));
        boolean filledPresent = false;
        for (BlockPos cauldron : cauldrons) {
            if (!bucketFor(server.getBlockState(cauldron)).isEmpty()) {
                filledPresent = true;
                break;
            }
        }
        boolean noBucket = chassis != null && filledPresent && chassis.storage().countOf(new ItemStack(Items.BUCKET)) <= 0;
        boolean extra = (entity.running && noCauldron) || (entity.running && full) || (entity.running && noBucket);
        DeviceVisuals.apply(server, pos, DeviceVisuals.status(entity.running, lookup, extra));
        if (!entity.running || extra || lookup.status() != NetworkStatus.OK || chassis == null) {
            return;
        }
        entity.cooldown--;
        if (entity.cooldown > 0) {
            return;
        }
        entity.cooldown = HopperBlockEntity.MOVE_ITEM_SPEED;
        entity.tryCollect(server, cauldrons, chassis, lookup.capacity());
    }

    private void tryCollect(ServerLevel level, List<BlockPos> cauldrons, RaidChassisBlockEntity chassis, long capacity) {
        if (cauldrons.isEmpty()) {
            return;
        }
        int start = Math.floorMod(cursor, cauldrons.size());
        for (int i = 0; i < cauldrons.size(); i++) {
            BlockPos cauldron = cauldrons.get((start + i) % cauldrons.size());
            ItemStack filled = bucketFor(level.getBlockState(cauldron));
            if (filled.isEmpty()) {
                continue;
            }
            long need = RaidMath.bytesFor(filled, 1);
            if (DeviceVisuals.full(new NetworkGraph.Lookup(NetworkStatus.OK, chassis.getBlockPos(), capacity), chassis, need)) {
                return;
            }
            ItemStack empty = chassis.extract(new ItemStack(Items.BUCKET), 1);
            if (empty.isEmpty()) {
                return;
            }
            level.setBlockAndUpdate(cauldron, Blocks.CAULDRON.defaultBlockState());
            long inserted = chassis.insert(filled, capacity);
            if (inserted < 1) {
                chassis.insert(empty, Math.max(capacity, chassis.storage().usedBytes() + 16));
            }
            cursor = Math.floorMod((start + i) + 1, Math.max(1, cauldrons.size()));
            setChanged();
            return;
        }
    }

    private List<BlockPos> findCauldrons(ServerLevel level, BlockPos pos, BlockState state) {
        List<BlockPos> found = new ArrayList<>();
        Direction left = CollectorBlock.cauldronFace(state);
        BlockPos start = pos.relative(left);
        if (isCauldron(level.getBlockState(start))) {
            found.add(start.immutable());
        }
        if (!(level.getBlockState(start).getBlock() instanceof DataCableBlock) && found.isEmpty()) {
            return found;
        }
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        if (level.getBlockState(start).getBlock() instanceof DataCableBlock) {
            queue.add(start);
            seen.add(start.immutable());
        }
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
                } else if (isCauldron(nearState) && dir.getAxis().isHorizontal()) {
                    found.add(near.immutable());
                }
            }
        }
        found.sort(Comparator.comparingLong(BlockPos::asLong));
        return found;
    }

    private static boolean isCauldron(BlockState state) {
        return state.getBlock() instanceof AbstractCauldronBlock;
    }

    private static ItemStack bucketFor(BlockState state) {
        if (state.is(Blocks.LAVA_CAULDRON)) {
            return new ItemStack(Items.LAVA_BUCKET);
        }
        if (state.hasProperty(LayeredCauldronBlock.LEVEL) && state.getValue(LayeredCauldronBlock.LEVEL) >= LayeredCauldronBlock.MAX_FILL_LEVEL) {
            if (state.is(Blocks.WATER_CAULDRON)) {
                return new ItemStack(Items.WATER_BUCKET);
            }
            if (state.is(Blocks.POWDER_SNOW_CAULDRON)) {
                return new ItemStack(Items.POWDER_SNOW_BUCKET);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("running", running);
        output.putInt("cooldown", cooldown);
        output.putInt("cursor", cursor);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        running = input.getBooleanOr("running", false);
        cooldown = input.getIntOr("cooldown", 0);
        cursor = input.getIntOr("cursor", 0);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.raid.collector");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new com.dubz.raid.menu.CollectorMenu(id, inventory, this);
    }
}
