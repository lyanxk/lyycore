package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.lyy.lyycore.content.ChestLidAnimation;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.function.Function;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/** Stationary surfaces stay in the chunk mesh; only the lid and motes animate here. */
public final class AnimatedChestRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private final AnimatedMeshModel mesh;
    private final Function<T, ChestLidAnimation> animation;
    public AnimatedChestRenderer(String model, Function<T, ChestLidAnimation> animation) {
        this.mesh = new AnimatedMeshModel(model);
        this.animation = animation;
    }
    private final Map<T, Motion> motions = new WeakHashMap<>();
    private final class Motion {
        boolean open;
        final float[] from = mesh.newPose(), current = mesh.newPose();
        Motion(boolean open, boolean transitioning) {
            this.open = open;
            boolean initialOpen = transitioning ? !open : open;
            mesh.sample(initialOpen ? "opened" : "closed", 0, current);
            System.arraycopy(current, 0, from, 0, current.length);
        }
    }
    @Override public AABB getRenderBoundingBox(T chest) {
        return new AABB(chest.getBlockPos()).inflate(.5, 0, .5).expandTowards(0, 1, 0);
    }
    @Override public void render(T chest, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (chest.getLevel() == null) return;
        var lid = animation.apply(chest);
        boolean open = lid.open();
        float progress = lid.progress(partial);
        var motion = motions.computeIfAbsent(chest, key -> new Motion(open, progress < 1));
        if (open != motion.open) {
            System.arraycopy(motion.current, 0, motion.from, 0, motion.current.length);
            motion.open = open;
        }
        String transition = open ? "open" : "close";
        float elapsed = progress * mesh.clipLength(transition);
        String clip = progress < 1 ? transition : open ? "opened" : "closed";
        float time = progress < 1 ? elapsed : lid.idleTime(partial);
        mesh.sample(clip, time, motion.current);
        // Reversing the lid midway starts from the visible pose, rather than snapping.
        if (elapsed < .2F) {
            float blend = Mth.clamp(elapsed / .2F, 0, 1);
            blend = blend * blend * (3 - 2 * blend);
            for (int i = 0; i < motion.current.length; i++) motion.current[i] = Mth.lerp(blend, motion.from[i], motion.current[i]);
        }
        pose.pushPose();
        pose.translate(.5, 0, .5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - chest.getBlockState().getValue(HorizontalDirectionalBlock.FACING).toYRot()));
        mesh.renderPose(pose, buffers, light, motion.current);
        pose.popPose();
    }
}
