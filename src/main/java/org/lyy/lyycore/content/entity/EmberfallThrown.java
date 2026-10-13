package org.lyy.lyycore.content.entity;

import org.lyy.lyycore.content.combat.CombatTargets;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import org.lyy.lyycore.content.SpecialDamage;
import org.lyy.lyycore.content.item.EmberfallItem;

/** Flight is world-space; the authored spin/tilt clips only supply local sword orientation. */
public final class EmberfallThrown extends Entity {
    private static final EntityDataAccessor<Long> CREATED = SynchedEntityData.defineId(EmberfallThrown.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> RETURN = SynchedEntityData.defineId(EmberfallThrown.class,EntityDataSerializers.LONG);
    private UUID owner;
    private Vec3 start = Vec3.ZERO, destination = Vec3.ZERO, returnStart = Vec3.ZERO;
    public EmberfallThrown(EntityType<? extends EmberfallThrown> type, Level level) { super(type,level); noPhysics=true; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(CREATED,0L); builder.define(RETURN,-1L); }
    public void prepare(Player player, ItemStack stack) {
        owner=player.getUUID(); start=player.getEyePosition().add(0,-.4,0); setPos(start);
        Vec3 end=start.add(player.getLookAngle().scale(5));
        var clip=level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        destination=clip.getType()==HitResult.Type.MISS ? end : clip.getLocation().subtract(player.getLookAngle().scale(.2));
        setYRot(player.getYRot()); entityData.set(CREATED,level().getGameTime());
    }
    public float age(float partial) { return level().getGameTime()-entityData.get(CREATED)+partial; }
    public float returnAge(float partial) { return entityData.get(RETURN)<0 ? -1 : level().getGameTime()-entityData.get(RETURN)+partial; }
    public void recall() { if (entityData.get(RETURN)<0) { returnStart=position(); entityData.set(RETURN,level().getGameTime()); } }
    @Override public void tick() {
        super.tick(); if (level().isClientSide) return;
        var player=owner==null ? null : level().getPlayerByUUID(owner);
        if (player==null || !player.isAlive()) { discard(); return; }
        if (age(0)>=200) recall();
        float returning=returnAge(0);
        if (returning>=0) {
            double t=Math.min(1,returning/11.0); t=t*t*(3-2*t);
            setPos(returnStart.lerp(player.getEyePosition().add(0,-.4,0),t));
            if (returning>=11) {
                for (var stack : player.getInventory().items) clearStack(player,stack);
                for (var stack : player.getInventory().offhand) clearStack(player,stack);
                discard();
            }
            return;
        }
        double t=Math.min(1,age(0)/9.0); t=t*t*(3-2*t); setPos(start.lerp(destination,t));
        if (age(0)>=9 && (tickCount-9)%20==0) {
            var bounds=AABB.ofSize(position(),3,3,3);
            for (var target : level().getEntitiesOfClass(LivingEntity.class,bounds,e -> CombatTargets.isEnemy(e,player)))
                SpecialDamage.burstFromBase(player,this,target,SpecialDamage.Element.FIRE,
                        player.getAttributeValue(Attributes.ARMOR_TOUGHNESS)*4+20,20);
        }
    }
    private void clearStack(Player player, ItemStack stack) {
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if (data.hasUUID(EmberfallItem.THROWN) && data.getUUID(EmberfallItem.THROWN).equals(getUUID()))
            CustomData.update(DataComponents.CUSTOM_DATA,stack,tag -> tag.remove(EmberfallItem.THROWN));
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { discard(); }
}
