package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.*;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import org.lyy.lyycore.registry.LyyItems;

@EventBusSubscriber(modid = "lyycore", value = Dist.CLIENT)
public final class DragonMightRenderer extends BlockEntityWithoutLevelRenderer {
    private static DragonMightRenderer renderer;
    private AnimatedMeshModel model;
    private static LivingEntity holder;
    private static ItemDisplayContext display;
    private DragonMightRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()); }
    public static void reload() { if (renderer != null) renderer.model = null; }
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new DragonMightRenderer(); return renderer;
            }
        }, LyyItems.DRAGON_MIGHT.get());
    }
    public static BakedModel wrap(BakedModel original) {
        return new BakedModelWrapper<>(original) {
            @Override public boolean isCustomRenderer() { return true; }
            @Override public ItemOverrides getOverrides() {
                return new ItemOverrides() {
                    @Override public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
                        holder = entity; return model;
                    }
                };
            }
            @Override public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
                original.applyTransform(context, pose, leftHand); display = context; return this;
            }
        };
    }
    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (model == null) model = new AnimatedMeshModel("dragon_might");
        var level = Minecraft.getInstance().level;
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        String clip = "idle"; float time = 0;
        boolean held = display != ItemDisplayContext.GUI && display != ItemDisplayContext.GROUND && display != ItemDisplayContext.FIXED;
        if (held && holder != null && holder.isUsingItem() && holder.getUseItem() == stack) {
            clip = "draw"; time = Math.min(1, (holder.getTicksUsingItem()+partial)/20);
        } else if (held && level != null) {
            var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            float age = data.contains("DragonReleased") ? (level.getGameTime()-data.getLong("DragonReleased")+partial)/20 : 100;
            if (age >= 0 && age < (data.getBoolean("DragonBurst") ? 1.05F : .18F)) { clip = "release"; time = age; }
        }
        pose.pushPose(); pose.translate(.5, .5, .5);
        model.render(pose, buffers, light, clip, time); pose.popPose();
        holder = null;
    }
}
