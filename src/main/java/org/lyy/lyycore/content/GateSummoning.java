package org.lyy.lyycore.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.registry.*;
import org.lyy.lyycore.content.entity.*;
import org.lyy.lyycore.content.entity.guiding.GuidingBoss;

/** Offerings and summon initialization shared by gates and the altar. */
public final class GateSummoning {
    public static EntityType<? extends Mob> type(ItemStack offering, boolean advanced) {
        if (offering.is(LyyItems.CRYSTAL_BLOCK.get())) return LyyEntities.IMAGINARY_GUARDIAN.get();
        if (advanced && offering.is(LyyItems.SMALL_IMAGINARY_CORE.get())) return LyyEntities.LIFE_REVEL.get();
        if (advanced && offering.is(LyyItems.PROOF.get())) return LyyEntities.GUIDING_LIGHT.get();
        return null;
    }
    public static void begin(Mob boss, Player player, BlockPos origin) {
        if (boss instanceof ImaginaryGuardian guardian) guardian.beginSummoning(player);
        if (boss instanceof LifeRevel revel) revel.beginSummoning(player);
        if (boss instanceof GuidingBoss guiding) guiding.beginSummoning(player, origin);
    }
    private GateSummoning() { }
}
