package org.lyy.lyycore.checks;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.*;
import org.lyy.lyycore.content.blockEntities.ProductionLabBlockEntity;
import org.lyy.lyycore.content.item.ResearchNotesItem;
import org.lyy.lyycore.content.research.ResearchManager;
import org.lyy.lyycore.registry.LyyBlocks;

@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class ProductionSyncRegressions {
    @GameTest(template = "empty", batch = "production_sync")
    public static void productionCompletionPublishesOneCompleteSnapshot(GameTestHelper test) {
        var level = test.getLevel();
        var packets = new ArrayList<CompoundTag>();
        var pos = test.absolutePos(new BlockPos(2, 1, 2));
        var player = ReviewRegressions.player(level, packet -> {
            if (packet instanceof ClientboundBlockEntityDataPacket update && update.getPos().equals(pos)) packets.add(update.getTag().copy());
        });
        player.moveTo(pos.getCenter());
        level.addNewPlayer(player);
        try {
            // The silent test transport does not send/ack chunk batches; mark this loaded chunk delivered.
            player.connection.chunkSender.dropChunk(player, new ChunkPos(pos));
            test.assertTrue(level.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false).contains(player), "Test player is not watching lab chunk");
            var lab = new ProductionLabBlockEntity(pos, LyyBlocks.PRODUCTION_LAB.get().defaultBlockState());
            lab.setLevel(level);
            var id = ResourceLocation.parse("lyycore:research/imaginary_reaper");
            lab.items().setStackInSlot(0, ResearchNotesItem.create(id, player.getUUID()));
            test.assertTrue(!packets.isEmpty(), "External insertion did not publish inventory");
            int duration = ResearchManager.get(level, id).value().production().orElseThrow().duration();
            for (int tick = 1; tick < duration; tick++)
                ProductionLabBlockEntity.serverTick(level, pos, lab.getBlockState(), lab);
            packets.clear();
            ProductionLabBlockEntity.serverTick(level, pos, lab.getBlockState(), lab);
            test.assertTrue(packets.size() == 1, "Completion published an intermediate/duplicate snapshot");
            test.assertTrue(packets.getFirst().getBoolean("Output") && packets.getFirst().getInt("Progress") == 0
                    && lab.data().get(1) == 2, "Completion snapshot is inconsistent");
            packets.clear();
            lab.items().extractItem(0, 1, false);
            test.assertTrue(packets.size() == 1 && !packets.getFirst().getBoolean("Output") && lab.data().get(1) == 0,
                    "External extraction no longer publishes/reset state");
        } finally { level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED); }
        test.succeed();
    }
}
