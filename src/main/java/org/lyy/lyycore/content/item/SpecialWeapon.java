package org.lyy.lyycore.content.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Server-side weapon actions. Each implementation owns its activation rules and cooldowns. */
public interface SpecialWeapon {
    void useSkill(ServerPlayer player, ItemStack stack);

    void useUltimate(ServerPlayer player, ItemStack stack);
}
