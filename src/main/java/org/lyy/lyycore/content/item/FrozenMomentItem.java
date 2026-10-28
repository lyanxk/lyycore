package org.lyy.lyycore.content.item;

import java.util.HashSet;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.lyy.lyycore.content.ProductionOwnership;
import org.lyy.lyycore.content.SpecialDamage;
import org.lyy.lyycore.content.entity.MagicBeam;
import org.lyy.lyycore.registry.LyySounds;

@EventBusSubscriber(modid = "lyycore")
public final class FrozenMomentItem extends Item implements SpecialWeapon {
    private static final String READY = "lyycore:frozen_moment_ready";
    private static final double RANGE = 128, HIT_RADIUS = 1;
    public FrozenMomentItem(Properties properties) { super(properties); }

    public static boolean scoping(LivingEntity entity) {
        return entity.isAlive() && entity.isUsingItem() && entity.getUseItem().getItem() instanceof FrozenMomentItem;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isSpectator()) return InteractionResultHolder.pass(player.getItemInHand(hand));
        return ItemUtils.startUsingInstantly(level, player, hand);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        return context.getPlayer() == null ? InteractionResult.PASS
                : use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return context.isSecondaryUseActive() ? useOn(context) : InteractionResult.PASS;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void entity(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getItemStack().getItem() instanceof FrozenMomentItem item)) return;
        event.setCancellationResult(item.use(event.getLevel(), event.getEntity(), event.getHand()).getResult());
        event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void entitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getItemStack().getItem() instanceof FrozenMomentItem item)) return;
        event.setCancellationResult(item.use(event.getLevel(), event.getEntity(), event.getHand()).getResult());
        event.setCanceled(true);
    }
    @Override public int getUseDuration(ItemStack stack, LivingEntity user) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }

    public static void fire(ServerPlayer player) {
        var stack = player.getMainHandItem();
        if (!player.isAlive() || player.isSpectator() || !(stack.getItem() instanceof FrozenMomentItem)
                || ProductionOwnership.isForeign(stack, player.getUUID())) return;
        var level = player.serverLevel();
        long now = level.getGameTime();
        if (now < player.getPersistentData().getLong(READY)) return;
        // Keep firing cooldown separate from vanilla item cooldowns, so aiming remains available.
        player.getPersistentData().putLong(READY, now + 20);
        Vec3 start = player.getEyePosition();
        Vec3 end = level.clip(new ClipContext(start, start.add(player.getLookAngle().scale(RANGE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
        var damaged = new HashSet<Integer>();
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE) + 20;
        for (var target : level.getEntities(player, new AABB(start, end).inflate(HIT_RADIUS),
                entity -> !entity.isSpectator() && entity.isPickable() && SpecialDamage.healthOwner(entity) != null)) {
            var owner = SpecialDamage.healthOwner(target);
            if (owner == null || owner == player || !owner.isAlive() || damaged.contains(owner.getId())) continue;
            var box = target.getBoundingBox();
            var expanded = box.inflate(HIT_RADIUS);
            var intersection = expanded.contains(start) ? java.util.Optional.of(start) : expanded.clip(start, end);
            if (intersection.isEmpty()) continue;
            // Expanded hitboxes must not catch a target through a wall beside or behind the beam endpoint.
            Vec3 hit = intersection.get();
            Vec3 surface = new Vec3(Mth.clamp(hit.x, box.minX, box.maxX), Mth.clamp(hit.y, box.minY, box.maxY),
                    Mth.clamp(hit.z, box.minZ, box.maxZ));
            if (level.clip(new ClipContext(start, surface, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                    .getType() != HitResult.Type.MISS) continue;
            damaged.add(owner.getId());
            if (SpecialDamage.combinedHitFromBase(player, player, target, owner, SpecialDamage.Element.PHYSICAL, base, 400)
                    && owner.isAlive()) owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 254));
        }
        // Offset only the visible muzzle; damage still follows the crosshair's eye ray.
        Vec3 forward = player.getLookAngle();
        double yaw = Math.toRadians(player.getYRot());
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 up = right.cross(forward).normalize();
        double handSide = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
        double horizontalOffset = scoping(player) ? 0 : .3 * handSide;
        Vec3 muzzle = start.add(forward.scale(.6)).add(right.scale(horizontalOffset)).add(up.scale(-.2));
        muzzle = level.clip(new ClipContext(start, muzzle, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
        MagicBeam.show(level, muzzle, end, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), LyySounds.FROZEN_MOMENT_FIRE.get(),
                SoundSource.PLAYERS, 1, 1);
    }

    @Override public void useSkill(ServerPlayer player, ItemStack stack) { }
    @Override public void useUltimate(ServerPlayer player, ItemStack stack) { }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flags) {
        lines.add(Component.translatable("tooltip.lyycore.frozen_moment").withColor(0x9CD9F3));
    }
}
