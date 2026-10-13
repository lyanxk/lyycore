package org.lyy.lyycore.content.combat;

import net.minecraft.world.entity.LivingEntity;
import org.lyy.lyycore.registry.LyyEffects;

/** Shared status checks, independent of the weapon that applied the effect. */
public final class ActionRestrictions {
    private ActionRestrictions() { }
    public static boolean frozen(LivingEntity entity) {
        var effect = entity.getEffect(LyyEffects.CRYSTALLIZATION);
        return effect != null && effect.getAmplifier() >= 2;
    }
}
