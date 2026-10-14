package org.lyy.lyycore.content.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.*;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import org.lyy.lyycore.content.SpecialDamage;
import org.lyy.lyycore.content.entity.*;
import org.lyy.lyycore.content.skills.*;
import org.lyy.lyycore.registry.LyyEntities;

/** All damage and cooldown decisions are server-side; stack data only synchronizes animation state. */
@EventBusSubscriber(modid = "lyycore")
public final class EmberfallItem extends Item implements SpecialWeapon {
    public static final String CLIP = "EmberfallClip", START = "EmberfallStart", THROWN = "EmberfallThrown";
    private static final String LEFT_READY = "lyycore:emberfall_left", HEAVY_READY = "lyycore:emberfall_heavy", FLYING = "lyycore:emberfall_flying";
    private static final ResourceLocation COUNTER = ResourceLocation.parse("lyycore:emberfall_counter");
    private static final int BURN_SECONDS = 5;
    public EmberfallItem(Properties properties) { super(properties); }
    @Override public void useSkill(ServerPlayer player, ItemStack stack) { }
    @Override public void useUltimate(ServerPlayer player, ItemStack stack) { }

    public static void animate(Player player, ItemStack stack, String clip) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> { data.putString(CLIP, clip); data.putLong(START, player.level().getGameTime()); });
    }
    private static boolean ready(Player player, String key) { return player.level().getGameTime() >= player.getPersistentData().getLong(key); }
    public static EmberfallThrown thrown(Player player) {
        var data = player.getPersistentData();
        if (player.level() instanceof ServerLevel level && data.hasUUID(FLYING)
                && level.getEntity(data.getUUID(FLYING)) instanceof EmberfallThrown sword && !sword.isRemoved()) return sword;
        return null;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void attack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof EmberfallItem) || player.isSpectator()) return;
        if (thrown(player) != null) { event.setCanceled(true); return; }
        var target = event.getTarget(); var living = SpecialDamage.healthOwner(target);
        if (living == null || !living.isAlive() || !target.isAttackable() || target.isInvulnerableTo(SpecialDamage.source(player,player,SpecialDamage.Element.FIRE))
                || living instanceof Player other && !player.canHarmPlayer(other)) return;
        if (!ready(player, LEFT_READY)) {
            living.igniteForSeconds(BURN_SECONDS);
            return;
        }
        event.setCanceled(true);
        if (target.skipAttackInteraction(player)) return;
        player.getPersistentData().putLong(LEFT_READY, player.level().getGameTime()+20);
        damage(player, player, target, player.getAttributeValue(Attributes.ATTACK_DAMAGE)+40, 40);
        player.resetAttackStrengthTicker();
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player.isSpectator()) return InteractionResultHolder.fail(stack);
        var style = StyleSystem.current(player);
        if (style == StyleSystem.Style.BUILDING) return InteractionResultHolder.pass(stack);
        if (level.isClientSide) return InteractionResultHolder.consume(stack);
        var flying = thrown(player);
        if (flying != null) { flying.recall(); return InteractionResultHolder.consume(stack); }
        switch (style) {
            case OFFENSE -> {
                if (!ready(player, HEAVY_READY)) return InteractionResultHolder.fail(stack);
                player.getPersistentData().putLong(HEAVY_READY, level.getGameTime()+37);
                animate(player, stack, "heavy_attack");
                CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> data.putBoolean("EmberfallRelease", true));
            }
            case TECHNIQUE -> { animate(player, stack, "block_enter"); player.startUsingItem(hand); }
            case MOBILITY -> {
                var sword = LyyEntities.EMBERFALL_THROWN.get().create(level);
                if (sword == null) return InteractionResultHolder.fail(stack);
                sword.prepare(player, stack);
                if (!level.addFreshEntity(sword)) return InteractionResultHolder.fail(stack);
                player.getPersistentData().putUUID(FLYING, sword.getUUID());
                CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> data.putUUID(THROWN, sword.getUUID()));
                animate(player, stack, "throw");
            }
        }
        return InteractionResultHolder.consume(stack);
    }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(entity instanceof ServerPlayer player)) return;
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (data.hasUUID(THROWN) && !(((ServerLevel)level).getEntity(data.getUUID(THROWN)) instanceof EmberfallThrown))
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(THROWN));
        if (!data.getBoolean("EmberfallRelease")) return;
        boolean held = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        if (!held || !player.isAlive() || StyleSystem.current(player) != StyleSystem.Style.OFFENSE) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove("EmberfallRelease")); return;
        }
        // The authored heavy clip releases its slash at 0.64 seconds.
        if (level.getGameTime()-data.getLong(START) >= 13) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove("EmberfallRelease"));
            var slash = LyyEntities.EMBERFALL_SLASH.get().create(level);
            if (slash != null) { slash.prepare(player); level.addFreshEntity(slash); }
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1, .65F);
        }
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer(); var level = context.getLevel();
        if (player == null || StyleSystem.current(player) != StyleSystem.Style.BUILDING) return InteractionResult.PASS;
        var pos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand())
                || !BaseFireBlock.canBePlacedAt(level,pos,context.getHorizontalDirection())) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            level.setBlockAndUpdate(pos, BaseFireBlock.getState(level,pos));
            level.playSound(null,pos,SoundEvents.FLINTANDSTEEL_USE,SoundSource.BLOCKS,1,1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public boolean canPerformAction(ItemStack stack, ItemAbility ability) { return ability == ItemAbilities.SHIELD_BLOCK; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BLOCK; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72000; }
    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (entity instanceof ServerPlayer player) animate(player, stack, "block_exit");
    }
    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (entity instanceof ServerPlayer player && StyleSystem.current(player) != StyleSystem.Style.TECHNIQUE) player.releaseUsingItem();
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void blocked(LivingShieldBlockEvent event) {
        if (!event.getBlocked() || event.getBlockedDamage() <= 0 || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.getUseItem().getItem() instanceof EmberfallItem)
                || StyleSystem.current(player) != StyleSystem.Style.TECHNIQUE || !SkillSystem.consumeAction(player, COUNTER)) return;
        event.setShieldDamage(0);
        animate(player, player.getUseItem(), "counter");
        var forward = Vec3.directionFromRotation(0,player.getYRot());
        var center = player.position().add(0,1,0).add(forward.scale(2.5));
        double base = player.getArmorValue()+GuardSkill.value(player)*4+40;
        for (var target : player.level().getEntitiesOfClass(LivingEntity.class, AABB.ofSize(center,8,5,8), e -> canHit(player,e))) {
            var offset = target.getBoundingBox().getCenter().subtract(center);
            if (Math.abs(offset.dot(forward)) <= 2.5 && Math.abs(offset.x*forward.z-offset.z*forward.x) <= 2.5)
                damage(player, player, target, base, 40);
        }
    }
    /** Ignite before damage so burning-dependent effects and drops see the correct state. */
    public static void damage(Player player, Entity direct, Entity target, double base, int hits) {
        var living = SpecialDamage.healthOwner(target);
        if (living == null || !living.isAlive() || living.isRemoved() || target.isRemoved()) return;
        living.igniteForSeconds(BURN_SECONDS);
        SpecialDamage.burstFromBase(player, direct, target, living, SpecialDamage.Element.FIRE, base, hits);
    }
    public static boolean canHit(Player player, LivingEntity target) {
        return target != player && target.isAlive() && target.isAttackable() && !target.isSpectator()
                && (!(target instanceof Player other) || player.canHarmPlayer(other));
    }
}
