package org.lyy.lyycore.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.lyy.lyycore.content.wings.WingsScoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Covers normal server teleports, including short dashes, commands and relative teleports. */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class ScoopTeleportMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("TAIL"))
    private void lyycore$resetScoopSweep(CallbackInfo callback) {
        WingsScoop.teleported(player);
    }
}
