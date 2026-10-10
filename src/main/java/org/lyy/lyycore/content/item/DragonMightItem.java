package org.lyy.lyycore.content.item;

import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.lyy.lyycore.content.*;
import org.lyy.lyycore.content.entity.MagicBeam;
import org.lyy.lyycore.content.menu.DragonControlMenu;
import org.lyy.lyycore.content.skills.StyleSystem;

/** Charge and burst stats are captured on release; each pulse aims along the player's current view. */
@EventBusSubscriber(modid = "lyycore")
public final class DragonMightItem extends Item {
    public static final double RANGE = 64;
    private static final int[] SHOT_TICKS = {1, 4, 8, 12};
    private record Burst(float damage, boolean burn, long released, int shot, int count) { }
    private static final Map<ServerPlayer, Burst> BURSTS = new WeakHashMap<>();
    public DragonMightItem(Properties properties) { super(properties); }
    public static float damage(float seconds) { return (float)Math.pow(1+Math.clamp(seconds, 0, 1), 4)*40; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer server) DragonControlMenu.open(server);
        } else player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }
    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (!(user instanceof ServerPlayer player)) return;
        float charge = Math.clamp((getUseDuration(stack, user)-remaining)/20F, 0, 1);
        var nest = EnderCompanions.boundNest(player);
        boolean stowed = nest != null && nest.mode() == org.lyy.lyycore.content.blockEntities.ImaginaryDragonNestBlockEntity.Mode.STOWED;
        float damage = damage(charge)*(stowed ? 1 : .25F);
        boolean burst = StyleSystem.current(player) == StyleSystem.Style.OFFENSE;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong("DragonReleased", level.getGameTime()); tag.putBoolean("DragonBurst", burst);
        });
        BURSTS.put(player, new Burst(damage, stowed, level.getGameTime(), 0, burst ? 4 : 1));
        player.getCooldowns().addCooldown(this, burst ? 21 : 6);
    }
    private static void fire(ServerPlayer player, float damage, boolean burn) {
        var level = player.serverLevel();
        var start = player.getEyePosition();
        var end = start.add(player.getLookAngle().scale(RANGE));
        end = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
        LivingEntity nearest = null;
        double distance = start.distanceToSqr(end);
        for (var target : level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(.4),
                e -> e != player && e.isAlive() && !e.isSpectator() && e.isPickable())) {
            var box = target.getBoundingBox().inflate(.15);
            var hit = box.contains(start) ? Optional.of(start) : box.clip(start, end);
            if (hit.isPresent() && start.distanceToSqr(hit.get()) <= distance) {
                nearest = target; distance = start.distanceToSqr(hit.get());
            }
        }
        if (nearest != null) {
            end = start.add(player.getLookAngle().scale(Math.sqrt(distance)));
            // Non-projectile magic: endermen take the ray hit without their arrow-dodge routine.
            var source = CombatDamage.source(player, "dragon_laser");
            int cooldown = nearest.invulnerableTime;
            boolean damaged;
            try { nearest.invulnerableTime = 0; damaged = nearest.hurt(source, damage); }
            finally { nearest.invulnerableTime = cooldown; }
            if (damaged && burn && nearest.isAlive()) DragonFireEffect.ignite(nearest, player);
        }
        MagicBeam.show(level, start, end);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var burst = BURSTS.get(player);
        if (burst == null) return;
        if (!player.isAlive() || player.isSpectator()) { BURSTS.remove(player); return; }
        if (player.level().getGameTime()-burst.released < SHOT_TICKS[burst.shot]) return;
        fire(player, burst.damage, burst.burn);
        if (burst.shot+1 < burst.count) BURSTS.put(player, new Burst(burst.damage, burst.burn, burst.released, burst.shot+1, burst.count));
        else BURSTS.remove(player);
    }
    @SubscribeEvent public static void protect(DamageReductionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isUsingItem()
                && player.getUseItem().getItem() instanceof DragonMightItem
                && StyleSystem.current(player) == StyleSystem.Style.TECHNIQUE) event.reduceBy(.5);
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) { BURSTS.remove(event.getEntity()); }
    @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) { BURSTS.clear(); }
}
