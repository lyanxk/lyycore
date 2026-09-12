package org.lyy.lyycore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import org.lyy.lyycore.content.item.SonnetBowItem;

/** Constructor parameters for the client-only arm-pose enum extension. */
public final class SonnetArmPoseParameters {
    public static final EnumProxy<HumanoidModel.ArmPose> CRYSTAL_BOW = new EnumProxy<>(
            HumanoidModel.ArmPose.class, true, (IArmPoseTransformer) SonnetArmPoseParameters::animate);

    private static void animate(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        boolean right = arm == HumanoidArm.RIGHT;
        var hand = arm == entity.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        var stack = entity.getItemInHand(hand);
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float draw = SonnetBowItem.crystalDraw(stack, entity.level(), partial);
        float age = SonnetBowItem.crystalShotAge(stack, entity.level(), partial);
        float recoil = age < 2 ? (float) Math.sin(age * Math.PI / 2) : 0;
        AnimationUtils.animateCrossbowHold(model.rightArm, model.leftArm, model.head, right);
        var stringArm = right ? model.leftArm : model.rightArm;
        stringArm.yRot += (right ? 1 : -1) * (1 - draw) * 0.45F;
        stringArm.xRot += (1 - draw) * 0.18F;
        var bowArm = right ? model.rightArm : model.leftArm;
        bowArm.xRot -= recoil * 0.10F;
    }
}
