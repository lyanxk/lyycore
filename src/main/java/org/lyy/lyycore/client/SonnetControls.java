package org.lyy.lyycore.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.extensions.common.*;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.item.SonnetBowItem;
import org.lyy.lyycore.network.SonnetNetwork;
import org.lyy.lyycore.registry.LyyItems;

@EventBusSubscriber(modid = LyyCore.MODID, value = Dist.CLIENT)
public final class SonnetControls {
    private static float recoil(float age) {
        return age < 2 ? (float) Math.sin(age * Math.PI / 2) : 0;
    }

    public static final KeyMapping ULTIMATE = new KeyMapping("key.lyycore.ultimate", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, "key.categories.lyycore");
    @SubscribeEvent public static void registerKeys(RegisterKeyMappingsEvent event) { event.register(ULTIMATE); }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        while (ULTIMATE.consumeClick()) {
            // Recall also works after switching away from the bow; the server validates ownership.
            if (minecraft.player != null && minecraft.screen == null)
                PacketDistributor.sendToServer(SonnetNetwork.Ultimate.INSTANCE);
        }
    }
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return SonnetItemRenderer.instance();
            }
            @Override public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack pose,
                    net.minecraft.client.player.LocalPlayer player, net.minecraft.world.entity.HumanoidArm arm,
                    ItemStack stack, float partialTick, float equip, float swing) {
                if (!SonnetBowItem.isCrystal(stack)) return false;
                // The horizontal OBJ is already in camera axes; do not apply vanilla bow/swing rotations.
                float age = SonnetBowItem.crystalShotAge(stack, player.level(), partialTick);
                float release = recoil(age);
                float draw = SonnetBowItem.crystalDraw(stack, player.level(), partialTick);
                pose.translate(arm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 0.12 : -0.12,
                        -0.32 - 0.025F * (1 - draw), -0.68 + 0.07F * release - 0.02F * (1 - draw));
                pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-4 * release));
                return true;
            }
            @Override public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return SonnetBowItem.isCrystal(stack) ? SonnetArmPoseParameters.CRYSTAL_BOW.getValue() : null;
            }
        }, LyyItems.WHISPER_OF_THE_PAST.get());
    }
}
