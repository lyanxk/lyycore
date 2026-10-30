package org.lyy.lyycore.content.finality;

import com.mojang.serialization.MapCodec;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

/** Solid flat layers with a cylindrical arena carved out around the origin. */
public final class FinalityArenaGenerator extends FlatLevelSource {
    public static final int RADIUS = 42, FLOOR_Y = 0, CEILING_Y = 60;
    public static final MapCodec<FinalityArenaGenerator> CODEC =
            FlatLevelGeneratorSettings.CODEC.fieldOf("settings")
                    .xmap(FinalityArenaGenerator::new, FinalityArenaGenerator::settings);

    public FinalityArenaGenerator(FlatLevelGeneratorSettings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<FinalityArenaGenerator> codec() {
        return CODEC;
    }

    private static boolean insideArena(int x, int z) {
        double centerX = x + 0.5, centerZ = z + 0.5;
        return centerX * centerX + centerZ * centerZ < RADIUS * RADIUS;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
        return super.fillFromNoise(blender, random, structures, chunk).thenApply(generated -> {
            var pos = new BlockPos.MutableBlockPos();
            var air = Blocks.AIR.defaultBlockState();
            int minY = Math.max(FLOOR_Y, generated.getMinBuildHeight());
            int maxY = Math.min(CEILING_Y, generated.getMaxBuildHeight());
            for (int x = 0; x < 16; x++) {
                int worldX = generated.getPos().getMinBlockX() + x;
                for (int z = 0; z < 16; z++) {
                    int worldZ = generated.getPos().getMinBlockZ() + z;
                    if (!insideArena(worldX, worldZ)) continue;
                    for (int y = minY; y < maxY; y++)
                        generated.setBlockState(pos.set(worldX, y, worldZ), air, false);
                }
            }
            return generated;
        });
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        var column = super.getBaseColumn(x, z, level, random);
        if (insideArena(x, z))
            for (int y = Math.max(FLOOR_Y, level.getMinBuildHeight());
                    y < Math.min(CEILING_Y, level.getMaxBuildHeight()); y++)
                column.setBlock(y, Blocks.AIR.defaultBlockState());
        return column;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        // Every column retains the solid roof above the arena.
        return level.getMinBuildHeight() + Math.min(level.getHeight(), settings().getLayers().size());
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor level) {
        return FLOOR_Y + 1;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getGenDepth() {
        return 256;
    }
}
