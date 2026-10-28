package com.dubz.raid.blockentity;

import com.dubz.raid.registry.ModBlockEntities;
import com.dubz.raid.storage.DeviceVisuals;
import com.dubz.raid.storage.NetworkGraph;
import com.dubz.raid.storage.RaidMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class TreeFarmBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CYCLE = 1200;
    public static final TagKey<Item> SAPLINGS = TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, Identifier.fromNamespaceAndPath("raid", "tree_farm_saplings"));
    public static final TagKey<Item> NETHER_SAPLINGS = TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, Identifier.fromNamespaceAndPath("raid", "nether_tree_farm_saplings"));
    public static final TagKey<Item> END_SAPLINGS = TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, Identifier.fromNamespaceAndPath("raid", "end_tree_farm_saplings"));

    private final SimpleContainer sapling = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            TreeFarmBlockEntity.this.setChanged();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.typeHolder().is(TreeFarmBlockEntity.this.saplingTag());
        }
    };
    private boolean running;
    private int progress;

    public TreeFarmBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TREE_FARM.get(), pos, state);
    }

    public TagKey<Item> saplingTag() {
        return switch (kind()) {
            case NETHER -> NETHER_SAPLINGS;
            case END -> END_SAPLINGS;
            default -> SAPLINGS;
        };
    }

    public com.dubz.raid.block.TreeFarmBlock.Kind kind() {
        return getBlockState().getBlock() instanceof com.dubz.raid.block.TreeFarmBlock farm
                ? farm.kind() : com.dubz.raid.block.TreeFarmBlock.Kind.OVERWORLD;
    }

    public boolean nether() {
        return kind() == com.dubz.raid.block.TreeFarmBlock.Kind.NETHER;
    }

    public boolean end() {
        return kind() == com.dubz.raid.block.TreeFarmBlock.Kind.END;
    }

    public net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> guiSound(boolean open) {
        return switch (kind()) {
            case NETHER -> open ? com.dubz.raid.registry.ModSounds.NETHER_TREE_FARM_ON
                    : com.dubz.raid.registry.ModSounds.NETHER_TREE_FARM_OFF;
            case END -> open ? com.dubz.raid.registry.ModSounds.END_TREE_FARM_ON
                    : com.dubz.raid.registry.ModSounds.END_TREE_FARM_OFF;
            default -> open ? com.dubz.raid.registry.ModSounds.TREE_FARM_ON
                    : com.dubz.raid.registry.ModSounds.TREE_FARM_OFF;
        };
    }

    public Container sapling() {
        return sapling;
    }

    public boolean running() {
        return running;
    }

    public int progress() {
        return progress;
    }

    public void toggle(Player player) {
        running = !running;
        if (!running) {
            progress = 0;
        }
        player.sendOverlayMessage(Component.translatable(running ? "gui.raid.tree_farm.on" : "gui.raid.tree_farm.off"));
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TreeFarmBlockEntity entity) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        NetworkGraph.Lookup lookup = NetworkGraph.get(server).lookupDevice(pos);
        RaidChassisBlockEntity chassis = lookup.chassisPos() != null
                && server.getBlockEntity(lookup.chassisPos()) instanceof RaidChassisBlockEntity found ? found : null;
        ItemStack sap = entity.sapling.getItem(0);
        ItemStack log = logFor(sap);
        boolean noSapling = sap.isEmpty() || log.isEmpty();
        boolean full = DeviceVisuals.full(lookup, chassis, RaidMath.bytesFor(log, 1));
        if (entity.running && full) {
            entity.progress = 0;
        }
        boolean extra = (entity.running && noSapling) || (entity.running && full);
        DeviceVisuals.apply(server, pos, DeviceVisuals.status(entity.running, lookup, extra));
        if (!entity.running || extra || lookup.status() != com.dubz.raid.storage.NetworkStatus.OK || chassis == null) {
            return;
        }
        entity.progress++;
        if (entity.progress >= CYCLE) {
            entity.progress = 0;
            chassis.insert(log.copy(), lookup.capacity());
        }
        entity.setChanged();
    }

    public static ItemStack logFor(ItemStack sapling) {
        if (sapling.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Identifier key = BuiltInRegistries.ITEM.getKey(sapling.getItem());
        String path = key.getPath();
        String log = switch (path) {
            case "oak_sapling" -> "oak_log";
            case "spruce_sapling" -> "spruce_log";
            case "birch_sapling" -> "birch_log";
            case "jungle_sapling" -> "jungle_log";
            case "acacia_sapling" -> "acacia_log";
            case "dark_oak_sapling" -> "dark_oak_log";
            case "mangrove_propagule" -> "mangrove_log";
            case "cherry_sapling" -> "cherry_log";
            case "pale_oak_sapling" -> "pale_oak_log";
            case "poplar_sapling" -> "poplar_log";
            case "crimson_fungus" -> "crimson_stem";
            case "warped_fungus" -> "warped_stem";
            case "small_jellyshroom" -> "jellyshroom_log";
            case "end_lotus_seed" -> "end_lotus_log";
            case "stalagnate_seed" -> "stalagnate_log";
            case "wart_seed" -> "wart_log";
            default -> path.endsWith("_sapling")
                    ? path.substring(0, path.length() - "_sapling".length()) + "_log"
                    : path.endsWith("_seed")
                    ? path.substring(0, path.length() - "_seed".length()) + "_log"
                    : null;
        };
        if (log == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(key.getNamespace(), log));
        if (item == net.minecraft.world.item.Items.AIR) {
            item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(key.getNamespace(), log.replace("_log", "_stem")));
        }
        return item == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) {
            Containers.dropContents(this.level, pos, sapling);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!sapling.getItem(0).isEmpty()) {
            output.store("sapling", ItemStack.CODEC, sapling.getItem(0));
        }
        output.putBoolean("running", running);
        output.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        sapling.setItem(0, input.read("sapling", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        running = input.getBooleanOr("running", false);
        progress = input.getIntOr("progress", 0);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new com.dubz.raid.menu.TreeFarmMenu(id, inventory, this);
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
