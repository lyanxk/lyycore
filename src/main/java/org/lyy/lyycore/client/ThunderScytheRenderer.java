package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import org.lyy.lyycore.content.item.ThunderScytheItem;

/** The supplied full-size clips are player-relative, shared by first and third person. */
@EventBusSubscriber(modid = "lyycore", value = Dist.CLIENT)
public final class ThunderScytheRenderer {
    private static AnimatedMeshModel mesh;
    public static void reload() { mesh = null; }

    static BakedModel wrap(BakedModel source, BakedModel empty) {
        BakedModel held = new BakedModelWrapper<>(source) {
            @Override public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
                boolean inHand = context.firstPerson() || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                        || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
                return (inHand ? empty : source).applyTransform(context, pose, leftHand);
            }
        };
        ItemOverrides overrides = new ItemOverrides() {
            @Override public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
                return entity instanceof Player ? held : source;
            }
        };
        return new BakedModelWrapper<>(source) {
            @Override public ItemOverrides getOverrides() { return overrides; }
        };
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        var mc = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || mc.level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var camera = event.getCamera().getPosition();
        var pose = event.getPoseStack();
        var buffers = mc.renderBuffers().bufferSource();
        for (var player : mc.level.players()) {
            if (!player.isAlive() || player.isInvisible() || player.isSpectator()) continue;
            var stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof ThunderScytheItem)) stack = player.getOffhandItem();
            if (!(stack.getItem() instanceof ThunderScytheItem) || player.distanceToSqr(camera) > 128 * 128
                    || !event.getFrustum().isVisible(player.getBoundingBox().inflate(8))) continue;
            if (mesh == null) mesh = new AnimatedMeshModel("thunder_scythe");
            var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            String attack = data.getBoolean(ThunderScytheItem.SWEEP_ANIMATION) ? "swing_combo" : "slash_attack";
            double age = data.contains(ThunderScytheItem.ANIMATION_START)
                    ? (mc.level.getGameTime() - data.getLong(ThunderScytheItem.ANIMATION_START) + partial) / 20.0 : -1;
            String clip = "held_idle";
            float time = (float)((mc.level.getGameTime() / 20.0) % mesh.clipLength(clip)) + partial / 20;
            if (age >= 0 && age < mesh.clipLength(attack)) { clip = attack; time = (float)age; }
            else if (age >= mesh.clipLength(attack) && age < mesh.clipLength(attack) + mesh.clipLength("return_to_idle")) {
                clip = "return_to_idle"; time = (float)age - mesh.clipLength(attack);
            } else if (age >= 0) {
                time = (float)((age - mesh.clipLength(attack) - mesh.clipLength("return_to_idle")) % mesh.clipLength(clip));
            }
            var origin = player.getPosition(partial).subtract(camera);
            pose.pushPose();
            pose.translate(origin.x, origin.y, origin.z);
            pose.mulPose(Axis.YP.rotationDegrees(180 - player.getViewYRot(partial)));
            mesh.render(pose, buffers, LevelRenderer.getLightColor(mc.level, player.blockPosition()), clip, time);
            pose.popPose();
        }
    }
}
