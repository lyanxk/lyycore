package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lyy.lyycore.content.trial.TrialAttack;

/** Every warning uses the same fixed position, radius and deadline as the server hit. */
public final class TrialAttackRenderer extends EntityRenderer<TrialAttack> {
    static final ResourceLocation TEXTURE = ResourceLocation.parse("lyycore:textures/entity/spacetime_trial/spatial_cube_palette.png");
    private final AnimatedMeshModel portal = model("summon_portal"), lance = model("crystal_lance"), beam = model("lance_beam");
    private final AnimatedMeshModel warning = model("ground_warning"), spikeWarning = model("spike_warning"), spike = model("ground_spike");
    private final AnimatedMeshModel projectile = model("crystal_projectile"), impact = model("crystal_impact");
    private final AnimatedMeshModel anchor = model("thread_anchor"), thread = model("thread_line");
    static AnimatedMeshModel model(String name) { return new AnimatedMeshModel("trial_" + name); }
    public TrialAttackRenderer(EntityRendererProvider.Context context) { super(context); TrialClient.binding = model("thread_binding"); }
    @Override public ResourceLocation getTextureLocation(TrialAttack entity) { return TEXTURE; }
    @Override public boolean shouldRender(TrialAttack e, Frustum frustum, double x, double y, double z) {
        AABB bounds = switch (e.kind()) {
            case SHOT -> new AABB(e.position(), e.destination()).inflate(2);
            case SPIKE -> e.getBoundingBox().expandTowards(0,25,0).inflate(1);
            case THREADS -> e.getBoundingBox().inflate(7);
            case BARRAGE -> e.getBoundingBox().inflate(2);
        };
        return frustum.isVisible(bounds);
    }
    @Override public void render(TrialAttack e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        switch (e.kind()) {
            case SHOT -> shot(e, partial, pose, buffers);
            case SPIKE -> spike(e, partial, pose, buffers);
            case BARRAGE -> barrage(e, partial, pose, buffers);
            case THREADS -> threads(e, partial, pose, buffers);
        }
    }
    private void shot(TrialAttack e, float partial, PoseStack pose, MultiBufferSource buffers) {
        float age = e.age(partial), time = age / 20;
        float hitTime = e.impactAge(partial) / 20;
        var end = e.aim();
        pose.pushPose();
        face(pose, new Vector3f(0,0,1), end);
        portal.render(pose, buffers, LightTexture.FULL_BRIGHT, e.phase()==2 ? "fire" : age<9 ? "open" : "charge",
                e.phase()==2 ? hitTime : age<9 ? time : Math.min(.8F, (age-9)/20));
        pose.popPose();
        if (e.phase()==0) {
            pose.pushPose(); face(pose, new Vector3f(0,1,0), end);
            lance.render(pose, buffers, LightTexture.FULL_BRIGHT, "spawn", Math.min(.25F, time));
            pose.popPose();
            return;
        }
        pose.pushPose(); pose.translate(end.x, end.y+.02, end.z);
        // The source ring's diameter is 1.125 blocks; bring its rim to the gameplay radius .5.
        pose.scale(8F/9, 1, 8F/9);
        warning.render(pose, buffers, LightTexture.FULL_BRIGHT, "idle", time);
        pose.popPose();
        if (e.phase()==1) {
            line(pose, buffers, thread, Vec3.ZERO, end.add(0,1,0), .32F, "idle", time);
            cylinder(pose, buffers, end, 2, 32);
        } else {
            if (hitTime < .15F) line(pose, buffers, beam, Vec3.ZERO, end.add(0,1,0), 1, "idle", hitTime);
            pose.pushPose(); pose.translate(end.x, end.y, end.z);
            impact.render(pose, buffers, LightTexture.FULL_BRIGHT, "burst", hitTime);
            pose.popPose();
        }
    }
    private void spike(TrialAttack e, float partial, PoseStack pose, MultiBufferSource buffers) {
        float time = e.age(partial)/20;
        float ground = (float)(e.ground()-e.getY());
        pose.pushPose(); pose.translate(0,ground+.025,0);
        pose.scale(1.265F,1,1.265F);
        spikeWarning.render(pose, buffers, LightTexture.FULL_BRIGHT, e.phase()==2 ? "release" : "charge",
                e.phase()==2 ? e.impactAge(partial)/20 : time*1.4F);
        pose.popPose();
        if (e.phase()!=2) {
            cylinder(pose, buffers, Vec3.ZERO, 25, 28);
            return;
        }
        float hitTime = e.impactAge(partial)/20;
        pose.pushPose();
        pose.scale(1.78F,20,2.28F); // The authored 1.25-block crystal spans the full 25-block arena.
        // Damage begins at full emergence; play only the retraction after the hit.
        spike.render(pose, buffers, LightTexture.FULL_BRIGHT, hitTime<.28F ? "erupt" : "retract", hitTime<.28F ? .28F : hitTime-.28F);
        pose.popPose();
        pose.pushPose(); pose.translate(0,ground,0);
        impact.render(pose, buffers, LightTexture.FULL_BRIGHT, "burst", hitTime);
        pose.popPose();
    }
    private void barrage(TrialAttack e, float partial, PoseStack pose, MultiBufferSource buffers) {
        if (e.phase()==2) {
            impact.render(pose, buffers, LightTexture.FULL_BRIGHT, "burst", e.impactAge(partial)/20);
            return;
        }
        float flight = e.age(partial)-e.delay();
        var direction = e.phase()==0 ? e.aim() : e.getDeltaMovement();
        pose.pushPose(); face(pose, new Vector3f(0,1,0), direction);
        // Unfired crystals remain visible; brighten in sequence just before release.
        projectile.render(pose, buffers, e.phase()==0 && flight < -2 ? 0x00800080 : LightTexture.FULL_BRIGHT,
                e.phase()==0 ? "spawn" : "flight", e.phase()==0 ? .25F : flight/20);
        pose.popPose();
        if (flight>=0 && flight<12) line(pose, buffers, thread, direction.normalize().scale(-1.4), Vec3.ZERO, .6F, "idle", flight/20);
        if (flight>=12 && flight<14) {
            pose.pushPose(); pose.scale(.4F,.4F,.4F);
            impact.render(pose, buffers, LightTexture.FULL_BRIGHT, "burst", (flight-12)/4);
            pose.popPose();
        }
    }
    private void threads(TrialAttack e, float partial, PoseStack pose, MultiBufferSource buffers) {
        float age = e.age(partial);
        String clip = age<5 ? "spawn" : age>TrialAttack.FIELD_TICKS-4 ? "despawn" : "idle";
        float time = (age>TrialAttack.FIELD_TICKS-4 ? age-TrialAttack.FIELD_TICKS+4 : age)/20;
        for (int i=0; i<6; i++) {
            var a = e.threadAnchor(i).subtract(e.position());
            pose.pushPose(); pose.translate(a.x,a.y-.3,a.z);
            anchor.render(pose, buffers, LightTexture.FULL_BRIGHT, clip, time);
            pose.popPose();
            for (int j=i+1; j<6; j++) line(pose, buffers, thread, a, e.threadAnchor(j).subtract(e.position()), 1, clip, time);
        }
    }
    private static void face(PoseStack pose, Vector3f from, Vec3 direction) {
        if (direction.lengthSqr()>.00001) pose.mulPose(new Quaternionf().rotationTo(from, direction.normalize().toVector3f()));
    }
    private static void line(PoseStack pose, MultiBufferSource buffers, AnimatedMeshModel model,
                             Vec3 a, Vec3 b, float width, String clip, float time) {
        var direction = b.subtract(a);
        if (direction.lengthSqr()<.00001) return;
        pose.pushPose(); pose.translate(a.x,a.y,a.z); face(pose, new Vector3f(0,1,0), direction);
        pose.scale(width, (float)direction.length()*16, width);
        model.render(pose, buffers, LightTexture.FULL_BRIGHT, clip, time);
        pose.popPose();
    }
    private static void cylinder(PoseStack pose, MultiBufferSource buffers, Vec3 base, double height, int alpha) {
        VertexConsumer vertices = buffers.getBuffer(RenderType.lightning());
        for (int i=0; i<32; i++) {
            double a=i*Math.PI/16, b=(i+1)*Math.PI/16;
            float ax=(float)(base.x+Math.cos(a)*.5), az=(float)(base.z+Math.sin(a)*.5);
            float bx=(float)(base.x+Math.cos(b)*.5), bz=(float)(base.z+Math.sin(b)*.5);
            vertices.addVertex(pose.last(), ax,(float)base.y,az).setColor(210,180,255,alpha);
            vertices.addVertex(pose.last(), bx,(float)base.y,bz).setColor(210,180,255,alpha);
            vertices.addVertex(pose.last(), bx,(float)(base.y+height),bz).setColor(210,180,255,alpha);
            vertices.addVertex(pose.last(), ax,(float)(base.y+height),az).setColor(210,180,255,alpha);
        }
    }
}
