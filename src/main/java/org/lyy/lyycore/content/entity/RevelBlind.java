package org.lyy.lyycore.content.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class RevelBlind extends RevelMinion {
    private int corner, healingTicks;

    public RevelBlind(EntityType<? extends RevelBlind> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return createMonsterAttributes().add(Attributes.MAX_HEALTH, 50).add(Attributes.ARMOR, 5).add(Attributes.MOVEMENT_SPEED, 0.25);
    }
    public void beginSummoning(int corner) { this.corner = corner; animate("summon", 44); }
    public static Vec3 formationOffset(int corner) {
        double angle = Math.PI / 4 + corner * Math.PI / 2;
        return new Vec3(Math.cos(angle) * 3, 0.35, Math.sin(angle) * 3);
    }
    @Override protected void updateWithBoss(LifeRevel boss) {
        Vec3 offset = boss.position().add(formationOffset(corner)).subtract(position());
        Vec3 desired = offset.normalize().scale(Math.min(0.5, offset.length() * 0.2));
        setDeltaMovement(getDeltaMovement().lerp(desired, 0.35));
        face(boss.position().add(0, 1.5, 0));
        if (++healingTicks >= 100) {
            healingTicks = 0;
            boss.heal(5);
            boss.showRegeneration();
            animate("heal", 32);
        }
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Corner", corner);
        tag.putInt("HealingTicks", healingTicks);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        corner = Math.clamp(tag.getInt("Corner"), 0, 3);
        healingTicks = Math.clamp(tag.getInt("HealingTicks"), 0, 99);
    }
}
