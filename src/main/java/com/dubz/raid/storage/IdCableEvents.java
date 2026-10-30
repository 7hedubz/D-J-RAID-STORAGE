package com.dubz.raid.storage;

import com.dubz.raid.registry.ModBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class IdCableEvents {
    private IdCableEvents() {
    }

    public static void onExpire(ItemExpireEvent event) {
        ItemEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        int id = IdLinks.pairIdOf(entity);
        if (id > 0) {
            IdLinks.get(level).onIrrevocableLoss(level.getServer(), id);
        }
    }

    public static void onLeave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof ItemEntity entity) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        RemovalReason reason = entity.getRemovalReason();
        if (reason == RemovalReason.UNLOADED_TO_CHUNK
                || reason == RemovalReason.UNLOADED_WITH_PLAYER
                || reason == RemovalReason.CHANGED_DIMENSION) {
            return;
        }
        if (entity.getItem().isEmpty() || !entity.getItem().is(ModBlocks.ID_CABLE.get().asItem())) {
            return;
        }
        int id = IdLinks.pairIdOf(entity);
        if (id > 0) {
            IdLinks.get(level).onIrrevocableLoss(level.getServer(), id);
        }
    }

    public static void onTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ItemEntity item) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        int id = IdLinks.pairIdOf(item);
        if (id <= 0) {
            return;
        }
        IdLinks links = IdLinks.get(level);
        if (links.isDoomed(id)) {
            IdLinks.explode(level, item.getX(), item.getY(), item.getZ());
            item.discard();
            return;
        }
        if (item.isInLava() || item.getY() < level.getMinY() - 32) {
            links.onIrrevocableLoss(level.getServer(), id);
            item.discard();
        }
    }
}
