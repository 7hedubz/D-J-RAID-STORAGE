package com.dubz.raid.blockentity;

import com.dubz.raid.block.DataCableBlock;
import com.dubz.raid.block.RaidChassisBlock;
import com.dubz.raid.block.RaidChestBlock;
import com.dubz.raid.block.RaidConnect;
import com.dubz.raid.block.RaidCrafterBlock;
import com.dubz.raid.block.RaidHopperBlock;
import com.dubz.raid.registry.ModBlockEntities;
import com.dubz.raid.storage.DeviceVisuals;
import com.dubz.raid.storage.OutputRouter;
import com.dubz.raid.storage.NetworkGraph;
import com.dubz.raid.storage.NetworkStatus;
import com.dubz.raid.storage.RaidMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RaidCrafterBlockEntity extends BlockEntity implements MenuProvider {
    private final NonNullList<ItemStack> pattern = NonNullList.withSize(9, ItemStack.EMPTY);
    private boolean running;
    private int cooldown;
    private int stockMask;
    private boolean recipeInvalid;
    private int rr;

    public RaidCrafterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTO_CRAFTER.get(), pos, state);
    }

    public ItemStack patternSlot(int slot) {
        return pattern.get(slot);
    }

    public boolean stocked(int slot) {
        return (stockMask & (1 << slot)) != 0;
    }

    public boolean recipeInvalid() {
        return recipeInvalid;
    }

    public boolean running() {
        return running;
    }

    public void setSlot(int slot, ItemStack stack) {
        ItemStack ghost = stack.copy();
        ghost.setCount(1);
        pattern.set(slot, ghost);
        bump();
        refreshRecipeFlag();
    }

    public void clearSlot(int slot) {
        pattern.set(slot, ItemStack.EMPTY);
        bump();
        refreshRecipeFlag();
    }

    public void toggle(Player player) {
        running = !running;
        refreshRecipeFlag();
        if (running && recipeInvalid) {
            player.sendOverlayMessage(Component.translatable("gui.raid.auto_crafter.no_recipe"));
        } else {
            player.sendOverlayMessage(Component.translatable(running ? "gui.raid.auto_crafter.on" : "gui.raid.auto_crafter.off"));
        }
        bump();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RaidCrafterBlockEntity entity) {
        if (level.isClientSide()) {
            return;
        }
        entity.cooldown--;
        if (level instanceof ServerLevel server) {
            entity.updateFace(server, pos);
        }
        if (entity.cooldown > 0) {
            return;
        }
        entity.cooldown = HopperBlockEntity.MOVE_ITEM_SPEED;
        entity.tryCraft((ServerLevel) level, pos, state);
    }

    private void updateFace(ServerLevel level, BlockPos pos) {
        refreshRecipeFlag();
        NetworkGraph.Lookup lookup = NetworkGraph.get(level).lookupDevice(pos);
        RaidChassisBlockEntity chassis = lookup.chassisPos() != null
                && level.getBlockEntity(lookup.chassisPos()) instanceof RaidChassisBlockEntity found ? found : null;
        ItemStack preview = previewResult();
        boolean full = DeviceVisuals.full(lookup, chassis, RaidMath.bytesFor(preview, Math.max(1, preview.getCount())));
        boolean noOutput = running && !hasValidOutput(level, pos, level.getBlockState(pos));
        DeviceVisuals.apply(level, pos, DeviceVisuals.status(running, lookup, recipeInvalid || (running && full) || noOutput));
    }

    private void tryCraft(ServerLevel level, BlockPos pos, BlockState state) {
        refreshRecipeFlag();
        NetworkGraph.Lookup lookup = NetworkGraph.get(level).lookupDevice(pos);
        int mask = 0;
        if (lookup.status() == NetworkStatus.OK && lookup.chassisPos() != null
                && level.getBlockEntity(lookup.chassisPos()) instanceof RaidChassisBlockEntity chassis) {
            for (int i = 0; i < 9; i++) {
                ItemStack ghost = pattern.get(i);
                if (!ghost.isEmpty() && chassis.storage().countOf(ghost) > 0) {
                    mask |= 1 << i;
                }
            }
        }
        if (mask != stockMask) {
            stockMask = mask;
            bump();
        }
        if (!running || recipeInvalid) {
            return;
        }
        if (!hasValidOutput(level, pos, state)) {
            return;
        }
        if (lookup.status() != NetworkStatus.OK || lookup.chassisPos() == null
                || !(level.getBlockEntity(lookup.chassisPos()) instanceof RaidChassisBlockEntity chassis)) {
            return;
        }
        Optional<RecipeHolder<CraftingRecipe>> recipe = currentRecipe(level);
        if (recipe.isEmpty()) {
            return;
        }
        if (!hasIngredients(chassis)) {
            return;
        }
        List<ItemStack> taken = new ArrayList<>();
        List<ItemStack> grid = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            ItemStack ghost = pattern.get(i);
            if (ghost.isEmpty()) {
                grid.add(ItemStack.EMPTY);
                continue;
            }
            ItemStack extracted = chassis.extract(ghost, 1);
            if (extracted.isEmpty()) {
                refund(chassis, lookup.capacity(), taken);
                return;
            }
            taken.add(extracted.copy());
            grid.add(extracted);
        }
        CraftingInput input = CraftingInput.of(3, 3, grid);
        if (!recipe.get().value().matches(input, level)) {
            refund(chassis, lookup.capacity(), taken);
            return;
        }
        ItemStack result = recipe.get().value().assemble(input);
        if (result.isEmpty()) {
            refund(chassis, lookup.capacity(), taken);
            return;
        }
        for (ItemStack leftover : recipe.get().value().getRemainingItems(input)) {
            if (!leftover.isEmpty()) {
                long back = chassis.insert(leftover, lookup.capacity());
                leftover.shrink((int) back);
                if (!leftover.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), leftover);
                }
            }
        }
        if (!pushResult(level, pos, state, result)) {
            refund(chassis, lookup.capacity(), taken);
        }
    }

    private boolean hasIngredients(RaidChassisBlockEntity chassis) {
        long[] need = new long[9];
        ItemStack[] keys = new ItemStack[9];
        int used = 0;
        for (ItemStack ghost : pattern) {
            if (ghost.isEmpty()) {
                continue;
            }
            int found = -1;
            for (int i = 0; i < used; i++) {
                if (ItemStack.isSameItemSameComponents(keys[i], ghost)) {
                    found = i;
                    break;
                }
            }
            if (found < 0) {
                keys[used] = ghost;
                need[used] = 1;
                used++;
            } else {
                need[found]++;
            }
        }
        for (int i = 0; i < used; i++) {
            if (chassis.storage().countOf(keys[i]) < need[i]) {
                return false;
            }
        }
        return used > 0;
    }

    private void refund(RaidChassisBlockEntity chassis, long capacity, List<ItemStack> taken) {
        for (ItemStack stack : taken) {
            long back = chassis.insert(stack, Math.max(capacity, chassis.storage().usedBytes() + RaidMath.bytesFor(stack, stack.getCount())));
            stack.shrink((int) back);
            if (!stack.isEmpty() && level != null) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
            }
        }
    }

    private boolean hasValidOutput(ServerLevel level, BlockPos pos, BlockState state) {
        return !OutputRouter.collect(level, pos, RaidCrafterBlock.outputFace(state), RaidCrafterBlockEntity::looksPushable).isEmpty();
    }

    private static boolean looksPushable(BlockState destState) {
        if (destState.getBlock() instanceof DataCableBlock
                || destState.getBlock() instanceof RaidHopperBlock
                || destState.getBlock() instanceof RaidCrafterBlock
                || destState.getBlock() instanceof RaidChassisBlock
                || destState.getBlock() instanceof RaidChestBlock) {
            return false;
        }
        return RaidConnect.isItemInventory(destState);
    }

    private boolean pushResult(ServerLevel level, BlockPos pos, BlockState state, ItemStack stack) {
        List<OutputRouter.Dest> dests = OutputRouter.collect(level, pos, RaidCrafterBlock.outputFace(state), RaidCrafterBlockEntity::looksPushable);
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

    private boolean insertAt(ServerLevel level, BlockPos dest, BlockState destState, Direction side, ItemStack stack) {
        if (destState.getBlock() instanceof DataCableBlock
                || destState.getBlock() instanceof RaidHopperBlock
                || destState.getBlock() instanceof RaidCrafterBlock
                || destState.getBlock() instanceof RaidChassisBlock
                || destState.getBlock() instanceof RaidChestBlock) {
            return false;
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
            int inserted = handler.insert(resource, stack.getCount(), transaction);
            if (inserted <= 0) {
                return false;
            }
            transaction.commit();
            stack.shrink(inserted);
            return stack.isEmpty();
        }
    }

    private Optional<RecipeHolder<CraftingRecipe>> currentRecipe(Level level) {
        if (!hasPattern()) {
            return Optional.empty();
        }
        if (!(level instanceof ServerLevel server)) {
            return Optional.empty();
        }
        List<ItemStack> grid = new ArrayList<>(9);
        for (ItemStack ghost : pattern) {
            grid.add(ghost.isEmpty() ? ItemStack.EMPTY : ghost.copy());
        }
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return server.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, server);
    }

    public ItemStack previewResult() {
        if (!(level instanceof ServerLevel server)) {
            return ItemStack.EMPTY;
        }
        return currentRecipe(server).map(holder -> {
            List<ItemStack> grid = new ArrayList<>(9);
            for (ItemStack ghost : pattern) {
                grid.add(ghost.isEmpty() ? ItemStack.EMPTY : ghost.copy());
            }
            return holder.value().assemble(CraftingInput.of(3, 3, grid));
        }).orElse(ItemStack.EMPTY);
    }

    private boolean hasPattern() {
        for (ItemStack stack : pattern) {
            if (!stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void refreshRecipeFlag() {
        boolean invalid = hasPattern() && (level == null || currentRecipe(level).isEmpty());
        if (invalid != recipeInvalid) {
            recipeInvalid = invalid;
            if (level instanceof ServerLevel server) {
                NetworkGraph.get(server).refreshVisuals(server);
            }
        }
    }

    private void bump() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < 9; i++) {
            if (!pattern.get(i).isEmpty()) {
                output.store("p" + i, ItemStack.CODEC, pattern.get(i));
            }
        }
        output.putBoolean("running", running);
        output.putInt("cooldown", cooldown);
        output.putInt("stockMask", stockMask);
        output.putBoolean("recipeInvalid", recipeInvalid);
        output.putInt("rr", rr);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < 9; i++) {
            pattern.set(i, input.read("p" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }
        running = input.getBooleanOr("running", false);
        cooldown = input.getIntOr("cooldown", 0);
        stockMask = input.getIntOr("stockMask", 0);
        recipeInvalid = input.getBooleanOr("recipeInvalid", false);
        rr = input.getIntOr("rr", 0);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.raid.auto_crafter");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new com.dubz.raid.menu.CrafterMenu(id, inventory, this);
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
