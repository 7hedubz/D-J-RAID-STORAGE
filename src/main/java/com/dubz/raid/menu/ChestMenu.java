package com.dubz.raid.menu;

import com.dubz.raid.block.RaidChestBlock;
import com.dubz.raid.blockentity.RaidChassisBlockEntity;
import com.dubz.raid.network.ModPayloads;
import com.dubz.raid.registry.ModMenus;
import com.dubz.raid.registry.ModSounds;
import com.dubz.raid.storage.DigitizedStorage;
import com.dubz.raid.storage.LinkedRaid;
import com.dubz.raid.storage.NetworkGraph;
import com.dubz.raid.storage.NetworkStatus;
import com.dubz.raid.storage.RaidMath;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChestMenu extends AbstractContainerMenu {
    private final BlockPos chestPos;
    private final Level level;
    private final Player player;

    private NetworkStatus status = NetworkStatus.DISCONNECTED;
    private long used;
    private long capacity;
    private final List<DigitizedStorage.Entry> clientEntries = new ArrayList<>();
    private String filter = "";
    private int lastRevision = Integer.MIN_VALUE;
    private long lastSyncedUsed = Long.MIN_VALUE;
    private long lastSyncedCapacity = Long.MIN_VALUE;
    private NetworkStatus lastSyncedStatus;

    public ChestMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, extraData.readBlockPos());
    }

    public ChestMenu(int containerId, Inventory inv, BlockPos chestPos) {
        super(ModMenus.CHEST.get(), containerId);
        this.chestPos = chestPos;
        this.player = inv.player;
        this.level = inv.player.level();

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(inv, col + row * 9 + 9,
                        ChestGuiLayout.INV_X + col * 18,
                        ChestGuiLayout.INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            addSlot(new Slot(inv, col, ChestGuiLayout.INV_X + col * 18, ChestGuiLayout.HOTBAR_Y));
        }
    }

    public BlockPos chestPos() {
        return chestPos;
    }

    public NetworkStatus status() {
        return status;
    }

    public long used() {
        return used;
    }

    public long storedBytes() {
        return used;
    }

    public long capacity() {
        return capacity;
    }

    public void setFilter(String filter) {
        this.filter = filter == null ? "" : filter;
    }

    private static boolean matchesFilter(ItemStack stack, String needle) {
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        if (name.contains(needle)) {
            return true;
        }
        String id = stack.typeHolder().unwrapKey()
                .map(key -> key.identifier().toString().toLowerCase(Locale.ROOT))
                .orElse("");
        if (id.contains(needle)) {
            return true;
        }
        return stack.typeHolder().tags().anyMatch(tag -> {
            String loc = tag.location().toString().toLowerCase(Locale.ROOT);
            String path = tag.location().getPath().toLowerCase(Locale.ROOT);
            return loc.contains(needle) || path.contains(needle) || ("#" + loc).contains(needle);
        }) || RaidItemGroups.matchesGroup(stack, needle);
    }

    public List<DigitizedStorage.Entry> visibleEntries() {
        if (filter.isBlank()) {
            return List.copyOf(clientEntries);
        }
        String needle = filter.toLowerCase(Locale.ROOT);
        List<DigitizedStorage.Entry> out = new ArrayList<>();
        for (DigitizedStorage.Entry entry : clientEntries) {
            if (matchesFilter(entry.stack, needle)) {
                out.add(entry);
            }
        }
        return out;
    }

    public void acceptSync(ModPayloads.StorageSyncPayload payload) {
        this.status = payload.status();
        this.used = payload.used();
        this.capacity = payload.capacity();
        this.clientEntries.clear();
        for (int i = 0; i < payload.items().size(); i++) {
            this.clientEntries.add(new DigitizedStorage.Entry(payload.items().get(i), payload.counts().get(i)));
        }
    }

    public void handleInsertCarried(ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ItemStack carried = getCarried();
        if (carried.isEmpty() || !RaidMath.canDigitize(carried)) {
            return;
        }
        RaidChassisBlockEntity chassis = resolveChassis(serverLevel);
        if (chassis == null || status != NetworkStatus.OK) {
            return;
        }
        long inserted = chassis.insert(carried, capacity);
        if (inserted > 0) {
            carried.shrink((int) inserted);
            setCarried(carried);
            sendSync(player, chassis);
        }
    }

    public void handleExtract(ServerPlayer player, ItemStack template, long amount, boolean toHand) {
        if (!(level instanceof ServerLevel serverLevel) || amount <= 0) {
            return;
        }
        RaidChassisBlockEntity chassis = resolveChassis(serverLevel);
        if (chassis == null) {
            return;
        }
        if (status != NetworkStatus.OK && status != NetworkStatus.LOCKED) {
            return;
        }
        if (toHand) {
            ItemStack carried = getCarried();
            if (!carried.isEmpty() && !ItemStack.isSameItemSameComponents(carried, template)) {
                return;
            }
            int max = template.getMaxStackSize();
            int room = carried.isEmpty() ? max : max - carried.getCount();
            int want = (int) Math.min(amount, room);
            if (want <= 0) {
                return;
            }
            ItemStack extracted = LinkedRaid.extract(serverLevel, chassis.getBlockPos(), template, want);
            if (extracted.isEmpty()) {
                return;
            }
            if (carried.isEmpty()) {
                setCarried(extracted);
            } else {
                carried.grow(extracted.getCount());
                setCarried(carried);
            }
            sendSync(player, chassis);
            return;
        }
        long remaining = amount;
        while (remaining > 0) {
            int want = (int) Math.min(template.getMaxStackSize(), remaining);
            ItemStack extracted = LinkedRaid.extract(serverLevel, chassis.getBlockPos(), template, want);
            if (extracted.isEmpty()) {
                break;
            }
            remaining -= extracted.getCount();
            if (!player.getInventory().add(extracted)) {
                net.minecraft.world.Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), extracted);
                break;
            }
        }
        sendSync(player, chassis);
    }

    private RaidChassisBlockEntity resolveChassis(ServerLevel serverLevel) {
        NetworkGraph.Lookup lookup = NetworkGraph.get(serverLevel).lookupChest(chestPos);
        this.capacity = lookup.capacity();
        if (lookup.status() == NetworkStatus.DISCONNECTED || lookup.status() == NetworkStatus.ERROR) {
            this.status = lookup.status();
            this.used = 0;
            return null;
        }
        if (serverLevel.getBlockEntity(lookup.chassisPos()) instanceof RaidChassisBlockEntity chassis) {
            this.used = chassis.storage().usedBytes();
            this.status = used > capacity ? NetworkStatus.LOCKED : NetworkStatus.OK;
            return chassis;
        }
        this.status = NetworkStatus.DISCONNECTED;
        this.used = 0;
        return null;
    }

    private void sendSync(ServerPlayer player, RaidChassisBlockEntity chassis) {
        PacketDistributor.sendToPlayer(player, ModPayloads.StorageSyncPayload.from(
                status,
                used,
                capacity,
                chassis == null || !(level instanceof ServerLevel serverLevel)
                        ? null
                        : LinkedRaid.mergedContents(serverLevel, chassis.getBlockPos())
        ));
        lastRevision = chassis == null ? Integer.MIN_VALUE : chassis.revision();
        lastSyncedUsed = used;
        lastSyncedCapacity = capacity;
        lastSyncedStatus = status;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            RaidChassisBlockEntity chassis = resolveChassis(serverLevel);
            sendSync(serverPlayer, chassis);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return ItemStack.EMPTY;
        }
        if (status != NetworkStatus.OK || !RaidMath.canDigitize(stack)) {
            return ItemStack.EMPTY;
        }
        RaidChassisBlockEntity chassis = resolveChassis(serverLevel);
        if (chassis == null) {
            return ItemStack.EMPTY;
        }
        long inserted = chassis.insert(stack, capacity);
        if (inserted <= 0) {
            return ItemStack.EMPTY;
        }
        stack.shrink((int) inserted);
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        sendSync(serverPlayer, chassis);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(chestPos.getX() + 0.5, chestPos.getY() + 0.5, chestPos.getZ() + 0.5) <= 64.0
                && player.level().getBlockState(chestPos).getBlock() instanceof RaidChestBlock;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!level.isClientSide()) {
            level.playSound(null, chestPos, ModSounds.CHEST_CLOSE.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }
}
