package com.dubz.raid.client;

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
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class RaidCrafterRenderer implements BlockEntityRenderer<RaidCrafterBlockEntity, RaidCrafterRenderer.State> {
    private static final double FACE_RANGE = 3.0;
    private static final int TURN_TICKS = 20;
    private static final float CELL = 3.0F / 16.0F;

    private final ItemModelResolver itemModelResolver;
    private final Map<BlockPos, Turn> turns = new HashMap<>();

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
        Level level = blockEntity.getLevel();
        if (level != null) {
            state.itemLight = LightCoordsUtil.getLightCoords(level, blockEntity.getBlockPos().above());
            state.angle = tickTurn(blockEntity.getBlockPos(), level, partialTicks);
        } else {
            state.itemLight = state.lightCoords;
            state.angle = 0.0F;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.items == null) {
            return;
        }
        for (int slot = 0; slot < state.items.size(); slot++) {
            ItemStackRenderState itemState = state.items.get(slot);
            if (itemState.isEmpty()) {
                continue;
            }
            boolean blockItem = itemState.getModelBoundingBox().getZsize() > 0.0625;
            poseStack.pushPose();
            poseStack.translate(0.5, 0.0, 0.5);
            poseStack.rotateDegrees(Axis.YP, state.angle);
            poseStack.translate((slot % 3) * CELL + 0.3125 - 0.5, blockItem ? 1.0625 : 1.005, (slot / 3) * CELL + 0.3125 - 0.5);
            poseStack.rotateDegrees(Axis.XP, 90.0F);
            poseStack.rotateDegrees(Axis.ZP, 180.0F);
            float scale = blockItem ? 0.25F : 0.175F;
            poseStack.scale(scale, scale, scale);
            int light = state.stocked[slot] ? state.itemLight : 0x400040;
            itemState.submit(poseStack, submitNodeCollector, light, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    private float tickTurn(BlockPos pos, Level level, float partialTicks) {
        Turn turn = turns.computeIfAbsent(pos.immutable(), ignored -> new Turn());
        long now = level.getGameTime();
        if (now != turn.lastTick) {
            turn.time++;
            turn.lastTick = now;
        }
        Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, FACE_RANGE, EntitySelector.NO_SPECTATORS);
        if (player != null) {
            double dx = player.getX() - (pos.getX() + 0.5);
            double dz = player.getZ() - (pos.getZ() + 0.5);
            double playerAngle = (Math.atan2(-dx, -dz) + 3.9269908169872414) % 6.283185307179586;
            int sector = (int) (playerAngle * 2.0 / Math.PI);
            if (turn.sector != sector) {
                turn.animating = true;
                turn.startAngle = turn.current;
                float delta = sector * 90.0F - turn.current;
                float wrapPos = delta + 360.0F;
                float wrapNeg = delta - 360.0F;
                if (Math.abs(wrapNeg) < Math.abs(delta) && Math.abs(wrapNeg) < Math.abs(wrapPos)) {
                    turn.endAngle = wrapNeg + turn.current;
                } else if (Math.abs(wrapPos) < Math.abs(delta) && Math.abs(wrapPos) < Math.abs(wrapNeg)) {
                    turn.endAngle = wrapPos + turn.current;
                } else {
                    turn.endAngle = delta + turn.current;
                }
                turn.startTime = turn.time;
                turn.sector = sector;
            }
        }
        if (turn.animating) {
            double elapsed = turn.time - turn.startTime;
            if (elapsed >= TURN_TICKS) {
                turn.animating = false;
                turn.current = turn.next = (turn.endAngle + 360.0F) % 360.0F;
            } else {
                turn.current = (easeOutQuad(elapsed, turn.startAngle, turn.endAngle - turn.startAngle) + 360.0F) % 360.0F;
                turn.next = (easeOutQuad(Math.min(elapsed + 1, TURN_TICKS), turn.startAngle, turn.endAngle - turn.startAngle) + 360.0F) % 360.0F;
            }
        }
        if (turns.size() > 64) {
            prune(pos);
        }
        return Mth.lerp(partialTicks, turn.current, turn.next);
    }

    private void prune(BlockPos keep) {
        Iterator<BlockPos> it = turns.keySet().iterator();
        while (it.hasNext()) {
            BlockPos other = it.next();
            if (!other.equals(keep) && other.distSqr(keep) > 64 * 64) {
                it.remove();
            }
        }
    }

    private static float easeOutQuad(double t, float start, float change) {
        float z = (float) (t / TURN_TICKS);
        return -change * z * (z - 2.0F) + start;
    }

    private static final class Turn {
        int time;
        long lastTick = -1;
        int sector = -1;
        boolean animating;
        float current;
        float next;
        float startAngle;
        float endAngle;
        double startTime;
    }

    @OnlyIn(Dist.CLIENT)
    public static class State extends BlockEntityRenderState {
        public List<ItemStackRenderState> items = List.of();
        public boolean[] stocked = new boolean[9];
        public float angle;
        public int itemLight;
    }
}
