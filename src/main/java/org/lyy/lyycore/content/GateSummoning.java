package org.lyy.lyycore.content;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.registry.*;
import org.lyy.lyycore.content.entity.*;
import org.lyy.lyycore.content.entity.guiding.GuidingBoss;
import org.lyy.lyycore.content.blockEntities.ImaginaryGateBlockEntity;
import org.lyy.lyycore.content.blockEntities.SummoningAltarBlockEntity;

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
    /** Persist the new body even while the originating controller is unloaded. */
    public static void replaceActiveBoss(ServerLevel level, BlockPos origin, UUID previous, UUID next) {
        SummoningReservations.get(level).replace(origin, previous, next);
        if (!level.hasChunkAt(origin)) return;
        var controller = level.getBlockEntity(origin);
        if (controller instanceof ImaginaryGateBlockEntity gate) gate.replaceActiveBoss(previous, next);
        else if (controller instanceof SummoningAltarBlockEntity altar) altar.replaceActiveBoss(previous, next);
    }
    private GateSummoning() { }
}
