package org.lyy.lyycore.content.item;

import org.lyy.lyycore.content.combat.CombatTargets;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import org.lyy.lyycore.content.SpecialDamage;
import org.lyy.lyycore.content.skills.StyleSystem;

/** Player-owned cooldowns prevent swapping stacks from bypassing either attack interval. */
@EventBusSubscriber(modid = "lyycore")
public final class ThunderScytheItem extends Item implements SpecialWeapon {
    public static final String ANIMATION_START = "ScytheAnimationStart", SWEEP_ANIMATION = "ScytheSweep";
    private static final String LEFT_READY = "lyycore:scythe_left_ready", SWEEP_READY = "lyycore:scythe_sweep_ready";
    private static final int COOLDOWN = 20;

    public ThunderScytheItem(Properties properties) {
        super(properties.attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(ResourceLocation.parse("lyycore:thunder_scythe_reach"), 4,
                                AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.BLOCK_INTERACTION_RANGE,
                        new AttributeModifier(ResourceLocation.parse("lyycore:thunder_scythe_reach"), 4,
                                AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build()));
    }

    @Override public void useSkill(ServerPlayer player, ItemStack stack) { }
    @Override public void useUltimate(ServerPlayer player, ItemStack stack) { }

    private static boolean ready(Player player, String key) {
        return player.level().getGameTime() >= player.getPersistentData().getLong(key);
    }
    private static void begin(Player player, ItemStack stack, String cooldown, boolean sweep) {
        long now = player.level().getGameTime();
        player.getPersistentData().putLong(cooldown, now + COOLDOWN);
        // Equipment synchronization delivers the same animation start to the holder and observers.
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong(ANIMATION_START, now);
            tag.putBoolean(SWEEP_ANIMATION, sweep);
        });
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1, .8F);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void attack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var stack = player.getMainHandItem();
        // A cooling-down scythe leaves the whole ordinary attack path to vanilla.
        if (!(stack.getItem() instanceof ThunderScytheItem) || player.isSpectator() || !ready(player, LEFT_READY)) return;
        var target = event.getTarget();
        var healthOwner = SpecialDamage.healthOwner(target);
        if (healthOwner == null || !healthOwner.isAlive() || healthOwner.isRemoved() || target.isRemoved()
                || !target.isAttackable() || target.isInvulnerableTo(player.damageSources().playerAttack(player))
                || healthOwner instanceof Player other && !player.canHarmPlayer(other)) return;
        if (target.skipAttackInteraction(player)) {
            // The target handled this interaction. Do not let vanilla invoke the hook a second time.
            event.setCanceled(true);
            return;
        }
        event.setCanceled(true);
        begin(player, stack, LEFT_READY, false);
        SpecialDamage.burstFromBase(player, player, target, healthOwner, SpecialDamage.Element.LIGHTNING,
                player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) * 20 + 240, 8);
        player.resetAttackStrengthTicker();
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player.isSpectator() || StyleSystem.current(player) != StyleSystem.Style.OFFENSE)
            return InteractionResultHolder.pass(stack);
        if (!ready(player, SWEEP_READY)) return InteractionResultHolder.fail(stack);
        if (player instanceof ServerPlayer) {
            begin(player, stack, SWEEP_READY, true);
            double base = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) * 4 + 20;
            for (var target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5),
                    entity -> entity.distanceToSqr(player) <= 25 && CombatTargets.isEnemy(entity, player)))
                SpecialDamage.burstFromBase(player, player, target, SpecialDamage.Element.LIGHTNING, base, 16);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || player.isSpectator()) return InteractionResult.PASS;
        var style = StyleSystem.current(player);
        if (style == StyleSystem.Style.OFFENSE)
            return use(context.getLevel(), player, context.getHand()).getResult();
        if (style != StyleSystem.Style.BUILDING) return InteractionResult.PASS;
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (!level.mayInteract(player, pos) || !level.getWorldBorder().isWithinBounds(pos)) return InteractionResult.FAIL;
        if (player instanceof ServerPlayer server) {
            var bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt == null) return InteractionResult.FAIL;
            bolt.moveTo(Vec3.atBottomCenterOf(pos).add(0, 1, 0));
            bolt.setCause(server);
            level.addFreshEntity(bolt);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
