package org.lyy.lyycore.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lyy.lyycore.content.blockEntities.SpacetimeMatrixBlockEntity;
import org.lyy.lyycore.registry.LyySounds;

/** One local loop per nearby controller, independent of camera visibility and server packets. */
@EventBusSubscriber(modid = "lyycore", value = Dist.CLIENT)
public final class SpacetimeMatrixSounds {
    private static final int RANGE = 4;
    private static final Map<BlockPos, Hum> ACTIVE = new HashMap<>();
    private static ClientLevel level;
    private static int scanTicks;

    private SpacetimeMatrixSounds() { }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var sounds = minecraft.getSoundManager();
        if (level != minecraft.level) {
            ACTIVE.values().forEach(sounds::stop);
            ACTIVE.clear();
            level = minecraft.level;
            scanTicks = 0;
        }
        if (level == null || minecraft.isPaused() || minecraft.getCameraEntity() == null) return;
        if (scanTicks++ % 10 != 0) return;
        ACTIVE.values().removeIf(sound -> sound.isStopped() || !sounds.isActive(sound));
        Vec3 listener = minecraft.getCameraEntity().getEyePosition();
        var center = BlockPos.containing(listener);
        // Query only already-loaded chunks intersecting the audible neighborhood.
        for (int x = (center.getX() - RANGE) >> 4; x <= (center.getX() + RANGE) >> 4; x++) {
            for (int z = (center.getZ() - RANGE) >> 4; z <= (center.getZ() + RANGE) >> 4; z++) {
                var chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (var entity : chunk.getBlockEntities().values()) {
                    if (!(entity instanceof SpacetimeMatrixBlockEntity matrix) || matrix.isRemoved()
                            || source(matrix).distanceToSqr(listener) >= RANGE * RANGE
                            || ACTIVE.containsKey(matrix.getBlockPos())) continue;
                    var hum = new Hum(matrix);
                    ACTIVE.put(matrix.getBlockPos(), hum);
                    sounds.play(hum);
                }
            }
        }
    }

    private static Vec3 source(SpacetimeMatrixBlockEntity matrix) {
        return matrix.getBlockPos().getCenter().add(0, 1, 0);
    }

    private static final class Hum extends AbstractTickableSoundInstance {
        private final SpacetimeMatrixBlockEntity matrix;

        Hum(SpacetimeMatrixBlockEntity matrix) {
            super(LyySounds.SPACETIME_MATRIX_HUM.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.matrix = matrix;
            var position = source(matrix);
            x = position.x; y = position.y; z = position.z;
            looping = true;
            delay = 0;
            volume = 0;
        }

        @Override public boolean canStartSilent() { return true; }

        @Override public void tick() {
            var minecraft = Minecraft.getInstance();
            var camera = minecraft.getCameraEntity();
            if (matrix.isRemoved() || matrix.getLevel() != minecraft.level || camera == null
                    || !minecraft.level.hasChunkAt(matrix.getBlockPos())
                    || camera.getEyePosition().distanceToSqr(x, y, z) >= RANGE * RANGE) {
                stop();
                return;
            }
            volume = Math.min(.45F, volume + .045F);
        }
    }
}
