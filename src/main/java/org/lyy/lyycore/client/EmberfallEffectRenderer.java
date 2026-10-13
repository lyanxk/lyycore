package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.lyy.lyycore.content.entity.*;

public final class EmberfallEffectRenderer<T extends Entity> extends EntityRenderer<T> {
    private static AnimatedMeshModel slash,ring,sword;
    private static float[] swordPose, recallPose, returnPose;
    public EmberfallEffectRenderer(EntityRendererProvider.Context context) { super(context); }
    public static void reload() { slash=ring=sword=null; }
    @Override public ResourceLocation getTextureLocation(T entity) { return ResourceLocation.parse("lyycore:textures/item/emberfall/jiejin_vfx_heat.png"); }
    @Override public void render(T entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        if (entity instanceof EmberfallThrown thrown) {
            if (sword==null) {
                sword=new AnimatedMeshModel("emberfall_thrown");
                swordPose=sword.newPose(); recallPose=sword.newPose(); returnPose=sword.newPose();
                sword.sample("return",0,returnPose);
            }
            pose.mulPose(Axis.YP.rotationDegrees(180-entity.getYRot()));
            pose.translate(0,-7.5647/16,0);
            float returning=thrown.returnAge(partial), age=thrown.age(partial);
            String clip=returning>=0 ? "return" : age<8.4 ? "throw" : "spin";
            float time=returning>=0 ? returning/20 : age<8.4 ? age/20 : (age-8.4F)/20;
            sword.sample(clip,time,swordPose);
            if (returning>=0 && returning<3) {
                float recallAge=age-returning;
                sword.sample(recallAge<8.4 ? "throw" : "spin",recallAge<8.4 ? recallAge/20 : (recallAge-8.4F)/20,recallPose);
                float blend=1-returning/3;
                blend=blend*blend*(3-2*blend);
                for (int bone=0; bone<swordPose.length; bone+=9) for (int axis=3; axis<6; axis++)
                    swordPose[bone+axis]+=Mth.wrapDegrees(recallPose[bone+axis]-returnPose[bone+axis])*blend;
            }
            sword.renderPose(pose,buffers,light,swordPose);
        } else if (entity instanceof EmberfallRing effect) {
            if (ring==null) ring=new AnimatedMeshModel("emberfall_ring");
            ring.render(pose,buffers,light,"heavy_attack",.96F+effect.age(partial)/20);
        } else {
            if (slash==null) slash=new AnimatedMeshModel("emberfall_slash");
            pose.mulPose(Axis.YP.rotationDegrees(180-entity.getYRot()));
            pose.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));
            slash.render(pose,buffers,light,"heavy_attack",.8F);
        }
        pose.popPose();
    }
}
