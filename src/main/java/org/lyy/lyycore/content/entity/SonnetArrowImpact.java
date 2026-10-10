package org.lyy.lyycore.content.entity;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.EntityHitResult;
import org.lyy.lyycore.content.SpecialDamage;

/** Shared ordinary-arrow adapter. Formula policy is separate from successful/blocked impact effects. */
final class SonnetArrowImpact {
    private SonnetArrowImpact() { }

    static void hit(AbstractArrow arrow, EntityHitResult result, Consumer<LivingEntity> effects,
                    BiConsumer<LivingEntity, DamageSource> knockback) {
        if (!(arrow.getOwner() instanceof Player owner) || !(arrow.level() instanceof ServerLevel level)) return;
        Entity target = result.getEntity();
        DamageSource source = SpecialDamage.source(owner, arrow, SpecialDamage.Element.PHYSICAL);
        float damage = damage(arrow, owner, target, source, level);
        owner.setLastHurtMob(target);
        int previousFire = target.getRemainingFireTicks();
        if (arrow.isOnFire() && target.getType() != EntityType.ENDERMAN) target.igniteForSeconds(5);
        if (target.hurt(source, damage)) successful(arrow, owner, target, source, level, effects, knockback);
        else blocked(arrow, owner, target, previousFire);
    }

    /** Apply launch speed, enchantments and critical roll before the shared special-damage formula. */
    private static float damage(AbstractArrow arrow, Player owner, Entity target, DamageSource source, ServerLevel level) {
        float base = (float)arrow.getBaseDamage();
        var weapon = arrow.getWeaponItem();
        if (weapon != null) base = EnchantmentHelper.modifyDamage(level, weapon, target, source, base);
        int impact = Mth.ceil(Math.clamp(arrow.getDeltaMovement().length() * base, 0, Integer.MAX_VALUE));
        if (arrow.isCritArrow()) impact = (int)Math.min(Integer.MAX_VALUE, (long)impact + owner.getRandom().nextInt(impact / 2 + 2));
        return SpecialDamage.amount(owner, SpecialDamage.Element.PHYSICAL, impact);
    }

    private static void successful(AbstractArrow arrow, Player owner, Entity target, DamageSource source, ServerLevel level,
                                   Consumer<LivingEntity> effects, BiConsumer<LivingEntity, DamageSource> knockback) {
        if (target instanceof LivingEntity living) {
            living.setArrowCount(living.getArrowCount() + 1);
            knockback.accept(living, source);
            EnchantmentHelper.doPostAttackEffectsWithItemSource(level, living, source, arrow.getWeaponItem());
            effects.accept(living);
            if (living != owner && living instanceof Player && owner instanceof ServerPlayer server && !arrow.isSilent())
                server.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.ARROW_HIT_PLAYER, 0));
        }
        arrow.playSound(SoundEvents.ARROW_HIT, 1, 1.2F);
        arrow.discard();
    }

    private static void blocked(AbstractArrow arrow, Player owner, Entity target, int previousFire) {
        target.setRemainingFireTicks(previousFire);
        arrow.deflect(ProjectileDeflection.REVERSE, target, owner, false);
        arrow.setDeltaMovement(arrow.getDeltaMovement().scale(.2));
        if (arrow.getDeltaMovement().lengthSqr() < 1.0E-7) arrow.discard();
    }
}
