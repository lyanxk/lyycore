package org.lyy.lyycore.checks;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.gametest.*;
import org.lyy.lyycore.content.blockEntities.AdvancedImaginaryGateBlockEntity;
import org.lyy.lyycore.registry.LyyBlocks;

@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class BiomeConversionRegressions {
    @GameTest(template = "empty")
    public static void repeatedQuartColumnsSkipWritesAcrossSaveAndRestart(GameTestHelper test) throws Exception {
        var level = test.getLevel();
        var origin = test.absolutePos(new BlockPos(2, 1, 2));
        // The first three distance-sorted block columns are within this same quart column.
        var pos = new BlockPos((origin.getX() & ~3) + 2, origin.getY(), (origin.getZ() & ~3) + 2);
        int qx = QuartPos.fromBlock(pos.getX()), qz = QuartPos.fromBlock(pos.getZ());
        var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        var desert = registry.getHolderOrThrow(Biomes.DESERT);
        var plains = registry.getHolderOrThrow(Biomes.PLAINS);
        var chunk = level.getChunkAt(pos);
        var sampler = level.getChunkSource().randomState().sampler();
        var method = AdvancedImaginaryGateBlockEntity.class.getDeclaredMethod("convertNextColumn", net.minecraft.server.level.ServerLevel.class);
        method.setAccessible(true);
        var gate = new AdvancedImaginaryGateBlockEntity(pos, LyyBlocks.ADVANCED_IMAGINARY_GATE.get().defaultBlockState());
        gate.setLevel(level); gate.selectBiome(Biomes.DESERT.location()); gate.setRunning(true);
        method.invoke(gate, level);
        test.assertTrue(chunk.getNoiseBiome(qx, 0, qz).equals(desert), "Initial column was not converted");
        // A later outside change must not be rewritten merely because the next block maps to the same cell.
        chunk.fillBiomesFromNoise((x, y, z, noise) -> x == qx && z == qz ? plains : chunk.getNoiseBiome(x, y, z), sampler);
        method.invoke(gate, level);
        test.assertTrue(gate.completedColumns() == 2 && chunk.getNoiseBiome(qx, 0, qz).equals(plains), "Duplicate column rewrote biomes");
        var saved = gate.saveWithoutMetadata(level.registryAccess());
        var loaded = new AdvancedImaginaryGateBlockEntity(pos, gate.getBlockState());
        loaded.setLevel(level); loaded.loadWithComponents(saved, level.registryAccess());
        method.invoke(loaded, level);
        test.assertTrue(loaded.completedColumns() == 3 && chunk.getNoiseBiome(qx, 0, qz).equals(plains), "Save/load lost de-duplication");
        // Restarting a completed pass must process the cells again.
        saved.putInt("Columns", AdvancedImaginaryGateBlockEntity.totalColumns());
        saved.putBoolean("Running", false);
        loaded.loadWithComponents(saved, level.registryAccess()); loaded.setRunning(true);
        method.invoke(loaded, level);
        test.assertTrue(loaded.completedColumns() == 1 && chunk.getNoiseBiome(qx, 0, qz).equals(desert), "New pass reused completed-cell cache");
        test.succeed();
    }
}
