package org.lyy.lyycore.content.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.lyy.lyycore.content.SpecialDamage;
import org.lyy.lyycore.content.item.LiuxiItem;

/** Straight flight with swept collisions; returning removes only the visual projectile. */
public final class LiuxiKnife extends Projectile {
    private ItemStack weapon = ItemStack.EMPTY;
    private double baseDamage;

    public LiuxiKnife(EntityType<? extends LiuxiKnife> type, Level level) { super(type, level); }

    public void prepare(Player player, ItemStack stack) {
        setOwner(player);
        weapon = stack.copy();
        baseDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE) + 20;
        setPos(player.getEyePosition().add(0, -0.15, 0));
        shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 3, 0);
        // Do not inherit the thrower's movement: 60 blocks/s in world space.
        setDeltaMovement(player.getLookAngle().scale(3));
    }

    @Override public ItemStack getWeaponItem() { return weapon; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }

    @Override public void tick() {
        super.tick();
        var start = position();
        var end = start.add(getDeltaMovement());
        if (!level().isClientSide) {
            if (!(getOwner() instanceof Player player) || !player.isAlive()) { discard(); return; }
            var blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            var hitEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
            for (Entity target : level().getEntities(this, getBoundingBox().expandTowards(hitEnd.subtract(start)).inflate(0.3),
                    entity -> entity != player && entity.isAlive() && !entity.isSpectator() && canHitEntity(entity))) {
                var living = SpecialDamage.healthOwner(target);
                if (living == null || living == player || living instanceof Player other && !player.canHarmPlayer(other)) continue;
                var bounds = target.getBoundingBox().inflate(0.15);
                if (bounds.contains(start) || bounds.clip(start, hitEnd).isPresent())
                    SpecialDamage.combinedHitFromBase(player, this, target, living, SpecialDamage.Element.PHYSICAL, baseDamage, 20);
            }
            if (blockHit.getType() != HitResult.Type.MISS || tickCount >= 20) {
                LiuxiItem.returned(player, this);
                discard();
                return;
            }
        }
        setPos(end);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) { discard(); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
}
