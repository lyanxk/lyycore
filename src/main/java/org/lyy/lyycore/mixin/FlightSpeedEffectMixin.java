package org.lyy.lyycore.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.lyy.lyycore.registry.LyyEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Scale flight displacement once, preserving vanilla steering, drag and collision handling. */
@Mixin(LivingEntity.class)
abstract class FlightSpeedEffectMixin {
    @ModifyArg(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"), index = 1)
    private Vec3 lyycore$flightSpeed(Vec3 movement) {
        var entity = (LivingEntity)(Object)this;
        boolean flying = entity.isFallFlying() || entity instanceof Player player && player.getAbilities().flying;
        return flying && entity.hasEffect(LyyEffects.FLIGHT_SPEED) ? movement.scale(1.5) : movement;
    }
}
