package org.lyy.lyycore.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lyy.lyycore.network.SpacetimeChainPayload;

import java.util.ArrayList;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** UUID-only saved connections never keep an entity or a chunk loaded. */
@EventBusSubscriber(modid = "lyycore")
public final class SpacetimeChains {
    private static final String LINKS = "SpacetimeChains", DIMENSION = "SpacetimeChainDimension", EPOCH = "SpacetimeChainEpoch";
    private static final Set<ResourceKey<Level>> DISPLAYED = new HashSet<>();
    private SpacetimeChains() { }

    private static ListTag links(Player player) { return player.getPersistentData().getList(LINKS, Tag.TAG_COMPOUND); }
    private static UUID epoch(Entity target) {
        var data = target.getPersistentData();
        String dimension = target.level().dimension().location().toString();
        if (!dimension.equals(data.getString(DIMENSION)) || !data.hasUUID(EPOCH)) {
            data.putString(DIMENSION, dimension);
            data.putUUID(EPOCH, UUID.randomUUID());
        }
        return data.getUUID(EPOCH);
    }

    public static void toggle(ServerPlayer player, LivingEntity target) {
        if (target == player || !target.isAlive()) return;
        var links = links(player);
        for (int i = 0; i < links.size(); i++) {
            if (links.getCompound(i).hasUUID("Target") && links.getCompound(i).getUUID("Target").equals(target.getUUID())) {
                links.remove(i);
                player.getPersistentData().put(LINKS, links);
                sync(player.serverLevel());
                return;
            }
        }
        if (links.size() >= 10) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("item.lyycore.spacetime_chain.limit"), true);
            return;
        }
        var link = new CompoundTag();
        link.putUUID("Target", target.getUUID());
        link.putUUID("Epoch", epoch(target));
        links.add(link);
        player.getPersistentData().put(LINKS, links);
        sync(player.serverLevel());
    }

    public static void pull(ServerPlayer player) {
        var links = links(player);
        for (int i = 0; i < links.size(); i++) {
            var target = resolve(player.serverLevel(), links.getCompound(i));
            if (target == null) continue;
            double angle = i * Math.PI * 2 / Math.max(1, links.size());
            Vec3 destination = player.position().add(Math.cos(angle) * 1.5, .1, Math.sin(angle) * 1.5);
            // Entity#teleportTo handles both mobs and other players in this same dimension.
            target.teleportTo(destination.x, destination.y, destination.z);
            target.setDeltaMovement(Vec3.ZERO);
            target.fallDistance = 0;
            target.hurtMarked = true;
        }
        player.getPersistentData().remove(LINKS);
        sync(player.serverLevel());
    }

    private static LivingEntity resolve(ServerLevel level, CompoundTag link) {
        if (!link.hasUUID("Target") || !link.hasUUID("Epoch")) return null;
        var target = level.getEntity(link.getUUID("Target"));
        return target instanceof LivingEntity living && living.isAlive() && !living.isRemoved()
                && epoch(living).equals(link.getUUID("Epoch")) ? living : null;
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        for (var level : event.getServer().getAllLevels()) {
            for (var player : level.players()) {
                var links = links(player);
                if (!player.isAlive()) { player.getPersistentData().remove(LINKS); continue; }
                boolean changed = false;
                for (int i = links.size() - 1; i >= 0; i--) {
                    var link = links.getCompound(i);
                    var target = resolve(level, link);
                    if (target == null) {
                        // Missing entities may be unloaded. Keep these UUIDs until they return.
                        if (!link.hasUUID("Target") || level.getEntity(link.getUUID("Target")) != null) {
                            links.remove(i);
                            changed = true;
                        }
                        continue;
                    }
                    Vec3 offset = player.position().subtract(target.position());
                    double distance = offset.length();
                    if (distance > 2) {
                        double strength = Math.min(.4, (distance - 2) * .08);
                        target.setDeltaMovement(target.getDeltaMovement().scale(.85).add(offset.scale(strength / distance)));
                        target.hurtMarked = true;
                    }
                }
                if (changed) player.getPersistentData().put(LINKS, links);
            }
            if (level.getGameTime() % 10 == 0) sync(level);
        }
    }

    public static void sync(ServerLevel level) {
        var visible = new ArrayList<SpacetimeChainPayload.Link>();
        for (var player : level.players()) {
            var links = links(player);
            for (int i = 0; i < links.size(); i++) {
                var target = resolve(level, links.getCompound(i));
                if (target != null) visible.add(new SpacetimeChainPayload.Link(player.getId(), target.getId()));
            }
        }
        org.lyy.lyycore.content.finality.DestinyWeaverItem.appendLinks(level, visible);
        var payload = new SpacetimeChainPayload(level.dimension().location(), visible);
        if (visible.isEmpty() && !DISPLAYED.remove(level.dimension())) return;
        if (!visible.isEmpty()) DISPLAYED.add(level.dimension());
        for (var player : level.players()) PacketDistributor.sendToPlayer(player, payload);
    }

    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity().getPersistentData().hasUUID(EPOCH)) epoch(event.getEntity());
    }
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent event) {
        var entity = event.getEntity();
        if (!(event.getLevel() instanceof ServerLevel level) || !entity.getPersistentData().hasUUID(EPOCH)) return;
        var reason = entity.getRemovalReason();
        if (reason != Entity.RemovalReason.CHANGED_DIMENSION && reason != Entity.RemovalReason.KILLED
                && reason != Entity.RemovalReason.DISCARDED) return;
        // Invalidate the saved connection even if its owner is currently offline.
        entity.getPersistentData().putUUID(EPOCH, UUID.randomUUID());
        for (var player : level.players()) {
            var links = links(player);
            links.removeIf(tag -> tag instanceof CompoundTag link && link.hasUUID("Target")
                    && link.getUUID("Target").equals(entity.getUUID()));
            player.getPersistentData().put(LINKS, links);
        }
        sync(level);
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        event.getEntity().getPersistentData().remove(LINKS);
        if (event.getEntity() instanceof ServerPlayer player) sync(player.serverLevel());
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { DISPLAYED.clear(); }
}
