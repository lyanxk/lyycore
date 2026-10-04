package org.lyy.lyycore.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.player.Player;
import org.lyy.lyycore.content.AegisWings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
abstract class PlayerFlightMixin {
    @ModifyExpressionValue(method = "tryToStartFallFlying", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean lyycore$canStartWingFlight(boolean original) {
        return AegisWings.unlocked((Player) (Object) this) || original;
    }
}
