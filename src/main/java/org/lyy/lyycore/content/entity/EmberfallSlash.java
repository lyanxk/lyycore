package org.lyy.lyycore.content.entity;

import org.lyy.lyycore.content.combat.CombatTargets;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import org.lyy.lyycore.content.item.EmberfallItem;
import org.lyy.lyycore.registry.LyyEntities;

/** Swept collision catches every crossed target once, even between server ticks. */
public final class EmberfallSlash extends Entity {
    private static final double RANGE = 20;
    private UUID owner;
    private double travelled;
    private final Set<Integer> hit = new HashSet<>();
    public EmberfallSlash(EntityType<? extends EmberfallSlash> type, Level level) { super(type,level); noPhysics = true; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
    public void prepare(Player player) {
        owner = player.getUUID(); setPos(player.getEyePosition().add(0,-.25,0));
        setDeltaMovement(player.getLookAngle().scale(1.2)); setYRot(player.getYRot()); setXRot(player.getXRot());
    }
    @Override public void tick() {
        super.tick(); if (level().isClientSide) return;
        var player = owner == null ? null : level().getPlayerByUUID(owner);
        if (player == null || !player.isAlive() || tickCount > 40) { discard(); return; }
        Vec3 from = position(), to = from.add(getDeltaMovement().normalize().scale(Math.min(1.2,RANGE-travelled)));
        var block = level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        boolean blocked = block.getType() != HitResult.Type.MISS;
        if (blocked) to = block.getLocation();
        var damage = new LinkedHashMap<LivingEntity, Double>();
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE) + 30;
        var targets = level().getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(1),
                e -> !hit.contains(e.getId()) && EmberfallItem.canHit(player,e));
        targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
        for (var target : targets) {
            var contact = target.getBoundingBox().inflate(.65).clip(from,to);
            if (contact.isEmpty() && !target.getBoundingBox().inflate(.65).contains(from)) continue;
            hit.add(target.getId());
            if (CombatTargets.isEnemy(target,player)) damage.merge(target, base * 20, Double::sum);
            ring(player,contact.orElse(from),damage,base);
        }
        travelled += from.distanceTo(to); setPos(to);
        if (blocked || travelled >= RANGE - .001) { ring(player,to,damage,base); discard(); }
        // All contacts in this sweep are one settlement per victim; never clear hurt cooldowns.
        damage.forEach((target, amount) -> EmberfallItem.damage(player, this, target, amount, 1));
    }
    private void ring(Player player, Vec3 pos, Map<LivingEntity, Double> damage, double base) {
        var ring = LyyEntities.EMBERFALL_RING.get().create(level());
        if (ring != null) { ring.prepare(pos); level().addFreshEntity(ring); }
        for (var target : level().getEntitiesOfClass(LivingEntity.class,AABB.ofSize(pos,4,4,4),
                e -> EmberfallItem.canHit(player,e) && e.getBoundingBox().getCenter().distanceToSqr(pos)<=4))
            damage.merge(target,base*10,Double::sum);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { discard(); }
}
