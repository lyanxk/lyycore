package org.lyy.lyycore.content.combat;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/** Shared hostile-target rules for player area attacks. */
public final class CombatTargets {
    private CombatTargets() { }
    public static boolean isEnemy(LivingEntity target, Player owner) {
        if (!target.isAlive() || target == owner || target.isAlliedTo(owner) || owner.isAlliedTo(target)) return false;
        if (target instanceof TamableAnimal pet && owner.getUUID().equals(pet.getOwnerUUID())) return false;
        return target instanceof Enemy || target instanceof Mob mob && mob.getTarget() == owner
                || owner.getLastHurtByMob() == target || target.getLastHurtByMob() == owner;
    }
}
