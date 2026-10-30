package org.lyy.lyycore.client.finality;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.*;

import org.joml.Quaternionf;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lyy.lyycore.client.finality.model.*;
import org.lyy.lyycore.content.finality.*;

public final class FinalityProjectileRenderer extends EntityRenderer<FinalityProjectile> {
    private static final String[] NAMES = {
        "starfall_stone", "crystal_primary", "oath_sword", "sacred_beam"
    };
    private final ModelPart[] models = {
        StarfallStoneGeometry.createBodyLayer().bakeRoot(),
        CrystalPrimaryGeometry.createBodyLayer().bakeRoot(),
        OathSwordGeometry.createBodyLayer().bakeRoot(),
        SacredBeamGeometry.createBodyLayer().bakeRoot()
    };
    private final ModelPart trail = StarfallTrailGeometry.createBodyLayer().bakeRoot();
    private final ModelPart warning = StarfallWarningGeometry.createBodyLayer().bakeRoot();

    public FinalityProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override
    public ResourceLocation getTextureLocation(FinalityProjectile entity) {
        return FinalitySummonRenderer.texture(NAMES[entity.kind().ordinal()]);
    }

    @Override
    public boolean shouldRender(FinalityProjectile e, Frustum f, double x, double y, double z) {
        var p = e.position();
        if (e.kind() == FinalityProjectile.Kind.BEAM)
            return f.isVisible(e.beamBounds(e.at(e.age(0) - 1), p).inflate(1));
        return f.isVisible(new AABB(p.x - 2, 0, p.z - 2, p.x + 2, 61, p.z + 2));
    }

    @Override
    public void render(
            FinalityProjectile e,
            float yaw,
            float partial,
            PoseStack pose,
            MultiBufferSource buffers,
            int light) {
        float age = e.age(partial);
        Vec3 position = e.getPosition(partial);
        if (e.kind() == FinalityProjectile.Kind.STONE) {
            pose.pushPose();
            pose.translate(0, .025 - position.y, 0);
            draw(
                    warning,
                    FinalitySummonRenderer.texture("starfall_warning"),
                    pose,
                    buffers,
                    1,
                    1,
                    1);
            pose.popPose();
            line(
                    pose,
                    buffers,
                    new Vec3(0, -position.y, 0),
                    new Vec3(0, 60 - position.y, 0),
                    .018F,
                    0x50FFE5AA);
        }
        if (age < e.delay() && e.kind() != FinalityProjectile.Kind.SWORD) return;
        if (e.kind() == FinalityProjectile.Kind.STONE)
            draw(trail, FinalitySummonRenderer.texture("starfall_trail"), pose, buffers, 1, 1, 1);
        pose.pushPose();
        if (e.kind() == FinalityProjectile.Kind.BEAM) {
            var direction = e.beamTop().subtract(position);
            // Shear the column so the top stays fixed and the foot light stays on the ground.
            pose.mulPose(new Matrix4f()
                    .m10((float) (direction.x / direction.y))
                    .m12((float) (direction.z / direction.y)));
            draw(models[3], getTextureLocation(e), pose, buffers, 8, (float) direction.y, 8);
        } else {
            if (e.kind() == FinalityProjectile.Kind.SWORD)
                pose.mulPose(
                        new Quaternionf()
                                .rotationTo(
                                        new Vector3f(1, 1, 0).normalize(),
                                        e.velocity().normalize().toVector3f()));
            draw(models[e.kind().ordinal()], getTextureLocation(e), pose, buffers, 1, 1, 1);
        }
        pose.popPose();
    }

    static void draw(
            ModelPart model,
            ResourceLocation texture,
            PoseStack pose,
            MultiBufferSource buffers,
            float x,
            float y,
            float z) {
        pose.pushPose();
        pose.scale(x, y, z);
        pose.translate(0, 1.5, 0);
        pose.scale(-1, -1, 1);
        model.render(
                pose,
                buffers.getBuffer(RenderType.entityTranslucentEmissive(texture)),
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    static void line(
            PoseStack pose, MultiBufferSource buffers, Vec3 from, Vec3 to, float width, int color) {
        var v = buffers.getBuffer(RenderType.lightning());
        Vec3 direction = to.subtract(from).normalize();
        Vec3 side = direction.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < .001) side = new Vec3(1, 0, 0);
        side = side.normalize().scale(width);
        for (int i = 0; i < 2; i++) {
            for (var p :
                    new Vec3[] {
                        from.add(side), to.add(side), to.subtract(side), from.subtract(side)
                    })
                v.addVertex(pose.last().pose(), (float) p.x, (float) p.y, (float) p.z)
                        .setColor(color);
            side = direction.cross(side);
        }
    }
}
