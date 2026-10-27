package org.lyy.lyycore.content.skills;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.common.NeoForgeMod;

/** Each source owns only its modifier; game modes and other flight providers remain independent. */
public final class FlightPermissions {
    private FlightPermissions() { }
    public static void update(ServerPlayer player, ResourceLocation source, boolean enabled) {
        var attribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (enabled && !attribute.hasModifier(source))
            attribute.addTransientModifier(new AttributeModifier(source, 1, AttributeModifier.Operation.ADD_VALUE));
        else if (!enabled && attribute.hasModifier(source)) attribute.removeModifier(source);
        if (!player.mayFly() && player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }
}
