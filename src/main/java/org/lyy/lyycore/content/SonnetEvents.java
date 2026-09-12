package org.lyy.lyycore.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.entity.living.*;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.item.SonnetBowItem;
import org.lyy.lyycore.registry.LyyEffects;

@EventBusSubscriber(modid = LyyCore.MODID)
public final class SonnetEvents {
    private static final ResourceLocation HOVER = ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "sonnet_hover");
    private static final String FROZEN = "SonnetFrozenState";
    public static boolean frozen(LivingEntity entity) { return entity.hasEffect(LyyEffects.CRYSTALLIZATION); }

    @SubscribeEvent public static void playerPre(PlayerTickEvent.Pre event) { hover(event.getEntity(), true); freeze(event.getEntity()); }
    @SubscribeEvent public static void playerPost(PlayerTickEvent.Post event) { hover(event.getEntity(), false); freeze(event.getEntity()); }
    @SubscribeEvent public static void entityPre(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof LivingEntity living && !(living instanceof Player)) freeze(living);
    }
    @SubscribeEvent public static void entityPost(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living && !(living instanceof Player)) freeze(living);
    }

    private static void hover(Player player, boolean beforeTick) {
        boolean drawing = player.isUsingItem() && player.getUseItem().getItem() instanceof SonnetBowItem
                && !SonnetBowItem.isCrystal(player.getUseItem());
        // Remove the previous implementation's modifier when updating an existing player.
        // Gravity remains intact: upward momentum still decelerates normally.
        var gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity != null) gravity.removeModifier(HOVER);
        var data = player.getPersistentData();
        if (!drawing) data.remove("SonnetDrawHeight");
        if ((drawing || frozen(player)) && player instanceof ServerPlayer serverPlayer)
            serverPlayer.connection.aboveGroundTickCount = 0;
        if (drawing) {
            if (beforeTick) data.putDouble("SonnetDrawHeight", player.getY());
            else if (data.contains("SonnetDrawHeight") && player.getY() < data.getDouble("SonnetDrawHeight"))
                player.setPos(player.getX(), data.getDouble("SonnetDrawHeight"), player.getZ());
            Vec3 velocity = player.getDeltaMovement();
            if (velocity.y < 0) player.setDeltaMovement(velocity.x, 0, velocity.z);
            player.fallDistance = 0;
        }
    }

    private static void freeze(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!frozen(entity)) {
            if (data.contains(FROZEN)) {
                if (entity instanceof Mob mob) mob.setNoAi(data.getCompound(FROZEN).getBoolean("NoAI"));
                data.remove(FROZEN);
            }
            return;
        }
        if (!data.contains(FROZEN)) {
            CompoundTag state = new CompoundTag();
            state.putDouble("X", entity.getX()); state.putDouble("Y", entity.getY()); state.putDouble("Z", entity.getZ());
            if (entity instanceof Mob mob) state.putBoolean("NoAI", mob.isNoAi());
            data.put(FROZEN, state);
        }
        var state = data.getCompound(FROZEN);
        var anchor = new Vec3(state.getDouble("X"), state.getDouble("Y"), state.getDouble("Z"));
        entity.stopUsingItem(); entity.setDeltaMovement(Vec3.ZERO); entity.fallDistance = 0;
        if (entity instanceof Mob mob) { mob.getNavigation().stop(); mob.setNoAi(true); }
        if (entity instanceof ServerPlayer player && player.distanceToSqr(anchor) > 0.0001)
            player.connection.teleport(anchor.x, anchor.y, anchor.z, player.getYRot(), player.getXRot());
        else entity.setPos(anchor);
    }

    @SubscribeEvent public static void attack(AttackEntityEvent event) { if (frozen(event.getEntity())) event.setCanceled(true); }
    private static void interact(PlayerInteractEvent event) {
        if (frozen(event.getEntity()) && event instanceof ICancellableEvent cancellable) cancellable.setCanceled(true);
    }
    @SubscribeEvent public static void rightItem(PlayerInteractEvent.RightClickItem event) { interact(event); }
    @SubscribeEvent public static void rightBlock(PlayerInteractEvent.RightClickBlock event) { interact(event); }
    @SubscribeEvent public static void leftBlock(PlayerInteractEvent.LeftClickBlock event) { interact(event); }
    @SubscribeEvent public static void rightEntity(PlayerInteractEvent.EntityInteract event) { interact(event); }
    @SubscribeEvent public static void rightSpecific(PlayerInteractEvent.EntityInteractSpecific event) { interact(event); }
    @SubscribeEvent public static void startUse(LivingEntityUseItemEvent.Start event) { if (frozen(event.getEntity())) event.setCanceled(true); }
    @SubscribeEvent public static void damage(LivingIncomingDamageEvent event) {
        if (event.getSource().getDirectEntity() instanceof LivingEntity attacker && frozen(attacker)) event.setCanceled(true);
    }
}
