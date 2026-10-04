package org.lyy.lyycore.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.AegisWings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
abstract class LivingFlightMixin {
    @ModifyExpressionValue(method = "updateFallFlying", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean lyycore$canContinueWingFlight(boolean original) {
        return AegisWings.unlocked((LivingEntity) (Object) this) || original;
    }
    @WrapOperation(method = "updateFallFlying", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;elytraFlightTick(Lnet/minecraft/world/entity/LivingEntity;I)Z"))
    private boolean lyycore$tickWingFlight(ItemStack chest, LivingEntity entity, int ticks, Operation<Boolean> original) {
        // Innate wings take priority and never tick (or consume) the equipped elytra.
        // Without wings, preserve the original operation and other mods' wrappers.
        return AegisWings.unlocked(entity) ? AegisWings.flightTick(entity, ticks) : original.call(chest, entity, ticks);
    }
}
