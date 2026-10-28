package com.dubz.raid.client;

import com.dubz.raid.block.RaidConnect;
import com.dubz.raid.blockentity.RaidCrafterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class RaidCrafterRenderer implements BlockEntityRenderer<RaidCrafterBlockEntity, RaidCrafterRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public RaidCrafterRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            RaidCrafterBlockEntity blockEntity,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.facing = blockEntity.getBlockState().getValue(RaidConnect.FACING);
        int seed = (int) blockEntity.getBlockPos().asLong();
        state.items = new ArrayList<>(9);
        state.stocked = new boolean[9];
        for (int slot = 0; slot < 9; slot++) {
            ItemStackRenderState itemState = new ItemStackRenderState();
            ItemStack ghost = blockEntity.patternSlot(slot);
            if (!ghost.isEmpty()) {
                this.itemModelResolver.updateForTopItem(
                        itemState, ghost, ItemDisplayContext.FIXED, blockEntity.getLevel(), null, seed + slot);
            }
            state.items.add(itemState);
            state.stocked[slot] = blockEntity.stocked(slot);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.items == null) {
            return;
        }
        float yaw = -state.facing.toYRot();
        for (int slot = 0; slot < state.items.size(); slot++) {
            ItemStackRenderState itemState = state.items.get(slot);
            if (itemState.isEmpty()) {
                continue;
            }
            int col = slot % 3;
            int row = slot / 3;
            poseStack.pushPose();
            poseStack.translate(0.5F, 1.01F, 0.5F);
            poseStack.rotateDegrees(Axis.YP, yaw);
            float x = (col - 1) * 0.3125F;
            float z = (row - 1) * 0.3125F;
            poseStack.translate(x, 0.0F, z);
            poseStack.rotateDegrees(Axis.XP, 90.0F);
            poseStack.scale(0.28F, 0.28F, 0.28F);
            int light = state.stocked[slot] ? state.lightCoords : 0x400040;
            itemState.submit(poseStack, submitNodeCollector, light, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public List<ItemStackRenderState> items = List.of();
        public boolean[] stocked = new boolean[9];
    }
}
