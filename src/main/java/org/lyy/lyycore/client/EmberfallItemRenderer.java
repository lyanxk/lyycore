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
import org.lyy.lyycore.content.item.EmberfallItem;
import org.lyy.lyycore.registry.LyyItems;

@EventBusSubscriber(modid = "lyycore", value = Dist.CLIENT)
public final class EmberfallItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static EmberfallItemRenderer renderer;
    private static LivingEntity holder;
    private AnimatedMeshModel model;
    private EmberfallItemRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()); }
    public static void reload() { if (renderer!=null) renderer.model=null; holder=null; }
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer==null) renderer=new EmberfallItemRenderer(); return renderer;
            }
        },LyyItems.EMBERFALL.get());
    }
    public static BakedModel wrap(BakedModel source) {
        var held = new BakedModelWrapper<>(source) {
            @Override public boolean isCustomRenderer() { return true; }
            @Override public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
                boolean inHand=context.firstPerson() || context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
                source.applyTransform(context,pose,leftHand);
                if (!inHand) holder=null;
                return inHand ? this : source;
            }
        };
        return new BakedModelWrapper<>(source) {
            @Override public ItemOverrides getOverrides() { return new ItemOverrides() {
                @Override public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) { holder=entity; return held; }
            }; }
        };
    }
    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var owner=holder; holder=null;
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if (data.hasUUID(EmberfallItem.THROWN)) return;
        if (model==null) model=new AnimatedMeshModel("emberfall");
        var mc=Minecraft.getInstance(); float partial=mc.getTimer().getGameTimeDeltaPartialTick(true);
        float age=mc.level==null || !data.contains(EmberfallItem.START) ? -1 : (mc.level.getGameTime()-data.getLong(EmberfallItem.START)+partial)/20F;
        String clip=data.getString(EmberfallItem.CLIP);
        if (clip.equals("counter")) clip="heavy_attack";
        float time=age;
        if (owner!=null && owner.isUsingItem() && owner.getUseItem()==stack && !clip.equals("heavy_attack")) {
            time=(owner.getTicksUsingItem()+partial)/20F;
            if (time<.3F) clip="block_enter";
            else { clip="block_hold"; time-=.3F; }
        } else if (age<0 || age>=model.clipLength(clip)) {
            clip=owner!=null && owner.isUsingItem() && owner.getUseItem()==stack ? "block_hold" : "idle";
            time=mc.level==null ? 0 : (mc.level.getGameTime()%20+partial)/20;
        }
        pose.pushPose();
        // ItemRenderer already shifts by (-.5, -.5, -.5). Blockbench's free-model
        // Display preview centers at (0, 8, 0), so keep its -8/16 Y offset while
        // undoing the block-model centering on X/Z. Apply this before bone animation.
        pose.translate(.5,0,.5);
        model.render(pose,buffers,light,clip,time); pose.popPose();
    }
}
