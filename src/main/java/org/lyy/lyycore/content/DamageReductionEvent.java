package org.lyy.lyycore.content;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/** Collects LyyCore reductions before damage is settled. Fired on the server game event bus. */
public final class DamageReductionEvent extends LivingEvent {
    private final DamageSource source;
    private final float damage;
    private double multiplier = 1;

    public DamageReductionEvent(LivingEntity entity, DamageSource source, float damage) {
        super(entity);
        this.source = source;
        this.damage = damage;
    }

    public DamageSource getSource() { return source; }

    /** Damage after armor, enchantments and resistance, before any collected reductions or absorption. */
    public float getDamage() { return damage; }

    public double getMultiplier() { return multiplier; }

    /** Adds a reduction fraction: 0.2 means 20% less damage; 0.5 and 0.2 together leave 40%. */
    public void reduceBy(double fraction) {
        if (!Double.isFinite(fraction) || fraction < 0 || fraction > 1) {
            throw new IllegalArgumentException("Damage reduction must be between 0 and 1: " + fraction);
        }
        multiplier *= 1 - fraction;
    }
}
