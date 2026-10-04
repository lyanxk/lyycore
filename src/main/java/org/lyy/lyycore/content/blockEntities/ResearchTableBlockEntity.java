package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.lyy.lyycore.registry.LyyBlockEntities;

public final class ResearchTableBlockEntity extends BlockEntity {
    public float bookYaw, previousBookYaw;
    public ResearchTableBlockEntity(BlockPos pos, BlockState state) { super(LyyBlockEntities.RESEARCH_TABLE.get(), pos, state); }
    public static void clientTick(Level level, BlockPos pos, BlockState state, ResearchTableBlockEntity table) {
        table.previousBookYaw = table.bookYaw;
        var player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 3, false);
        if (player != null) {
            float target = (float) Math.toDegrees(Math.atan2(player.getX() - pos.getX() - 0.5, player.getZ() - pos.getZ() - 0.5));
            table.bookYaw += Mth.wrapDegrees(target - table.bookYaw) * 0.1F;
        } else table.bookYaw += 0.5F;
        table.bookYaw = Mth.wrapDegrees(table.bookYaw);
    }
}
