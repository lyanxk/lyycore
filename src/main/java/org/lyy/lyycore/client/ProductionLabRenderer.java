package org.lyy.lyycore.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.lyy.lyycore.content.blockEntities.ProductionLabBlockEntity;
import org.lyy.lyycore.content.blocks.ProductionLabBlock;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public final class ProductionLabRenderer implements BlockEntityRenderer<ProductionLabBlockEntity> {
    public static final List<ModelResourceLocation> MODELS = IntStream.range(0, 10)
            .mapToObj(i -> model("part_" + i)).toList();
    private record Frame(float time, float x, float y, float z) { }
    private record Part(ModelResourceLocation model, float[] pivot, List<Frame> position, List<Frame> rotation, List<Frame> scale) { }
    private static List<Part> parts = List.of();
    public ProductionLabRenderer(BlockEntityRendererProvider.Context context) { }
    private static ModelResourceLocation model(String name) {
        return ModelResourceLocation.standalone(ResourceLocation.parse("lyycore:block/production_lab/" + name));
    }
    public static void reload() {
        try (var reader = Minecraft.getInstance().getResourceManager()
                .getResourceOrThrow(ResourceLocation.parse("lyycore:animations/production_lab.json")).openAsReader()) {
            var loaded = new ArrayList<Part>();
            for (var element : JsonParser.parseReader(reader).getAsJsonArray()) {
                var object = element.getAsJsonObject();
                var pivot = object.getAsJsonArray("pivot");
                var channels = object.getAsJsonObject("channels");
                loaded.add(new Part(model(object.get("model").getAsString()),
                        new float[]{pivot.get(0).getAsFloat(), pivot.get(1).getAsFloat(), pivot.get(2).getAsFloat()},
                        frames(channels.getAsJsonArray("position")), frames(channels.getAsJsonArray("rotation")), frames(channels.getAsJsonArray("scale"))));
            }
            parts = List.copyOf(loaded);
        } catch (IOException error) { throw new UncheckedIOException(error); }
    }
    private static List<Frame> frames(JsonArray array) {
        if (array == null) return List.of();
        var frames = new ArrayList<Frame>();
        for (var element : array) {
            var f = element.getAsJsonArray();
            frames.add(new Frame(f.get(0).getAsFloat(), f.get(1).getAsFloat(), f.get(2).getAsFloat(), f.get(3).getAsFloat()));
        }
        return List.copyOf(frames);
    }
    private static Frame sample(List<Frame> frames, float time, float fallback) {
        if (frames.isEmpty()) return new Frame(time, fallback, fallback, fallback);
        if (time <= frames.getFirst().time) return frames.getFirst();
        for (int i = 1; i < frames.size(); i++) {
            Frame next = frames.get(i), previous = frames.get(i - 1);
            if (time <= next.time) {
                float alpha = (time - previous.time) / (next.time - previous.time);
                return new Frame(time, Mth.lerp(alpha, previous.x, next.x), Mth.lerp(alpha, previous.y, next.y), Mth.lerp(alpha, previous.z, next.z));
            }
        }
        return frames.getLast();
    }
    @Override public AABB getRenderBoundingBox(ProductionLabBlockEntity lab) {
        return new AABB(lab.getBlockPos()).inflate(2, 0, 2).expandTowards(0, 2, 0);
    }
    @Override public void render(ProductionLabBlockEntity lab, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float time = lab.animationProgress(partial) * 8;
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-lab.getBlockState().getValue(ProductionLabBlock.FACING).toYRot()));
        for (Part part : parts) {
            Frame position = sample(part.position, time, 0), rotation = sample(part.rotation, time, 0), scale = sample(part.scale, time, 1);
            if (scale.x <= 0 || scale.y <= 0 || scale.z <= 0) continue;
            pose.pushPose();
            pose.translate((part.pivot[0] + position.x) / 16, (part.pivot[1] + position.y) / 16, (part.pivot[2] + position.z) / 16);
            pose.mulPose(Axis.ZP.rotationDegrees(rotation.z));
            pose.mulPose(Axis.YP.rotationDegrees(rotation.y));
            pose.mulPose(Axis.XP.rotationDegrees(rotation.x));
            // Baking at quarter size keeps this large model inside vanilla JSON coordinate limits.
            pose.scale(4 * scale.x, 4 * scale.y, 4 * scale.z);
            pose.translate(-0.5, -0.5, -0.5);
            var type = RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS);
            var mc = Minecraft.getInstance();
            mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(part.model), ItemStack.EMPTY, light, overlay, pose, buffers.getBuffer(type));
            pose.popPose();
        }
        pose.popPose();
    }
}
