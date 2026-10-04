package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
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
import org.lyy.lyycore.content.blockEntities.ResearchTableBlockEntity;

import java.util.stream.IntStream;
import java.util.List;

/** The supplied model's table stays still; only its book and glyph groups animate. */
public final class ResearchTableRenderer implements BlockEntityRenderer<ResearchTableBlockEntity> {
    private static final String[] BOOK_PARTS = {"book_hover", "book_left", "book_right", "turning_page"};
    public static final List<ModelResourceLocation> MODELS = IntStream.range(0, 12)
            .mapToObj(i -> ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("lyycore",
                    "block/research_table/" + (i < 4 ? BOOK_PARTS[i] : "glyph_" + (i - 4))))).toList();
    private static final double[][] GLYPHS = {
            {3.25, 13.7, 0}, {-2.14926, 14.25, 2.43786}, {-0.40733, 14.8, -3.22437},
            {2.68801, 15.35, 1.82677}, {-3.1479, 15.9, 0.80824}, {1.47547, 16.45, -2.89577},
            {1.1964, 17, 3.02177}, {-3.05786, 17.55, -1.1009}
    };

    public ResearchTableRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public AABB getRenderBoundingBox(ResearchTableBlockEntity table) {
        return new AABB(table.getBlockPos()).expandTowards(0, 0.75, 0);
    }

    @Override
    public void render(ResearchTableBlockEntity table, float partial, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        if (table.getLevel() == null) return;
        float seconds = (table.getLevel().getGameTime() % 24000 + partial) / 20F;
        float phase = seconds * Mth.TWO_PI / 4;
        pose.pushPose();
        pose.translate(0.5, (19 + Mth.sin(phase) * 0.6) / 16, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partial, table.previousBookYaw, table.bookYaw)));
        pose.mulPose(Axis.XP.rotationDegrees(15));
        draw(0, pose, buffers, light, overlay);
        bookWing(1, -22.5F, pose, buffers, light, overlay);
        bookWing(2, 22.5F, pose, buffers, light, overlay);
        float cycle = seconds % 4;
        // Reset the leaf while hidden behind the left pages.
        if (cycle < 2.6F || cycle > 2.74F) {
            pose.pushPose();
            pose.translate(0, 0.6 / 16, 0);
            float turn = cycle > 2.74F ? 0 : Mth.clamp((cycle - 1.25F) / 1.05F, 0, 1);
            turn = turn * turn * (3 - 2 * turn);
            bookWing(3, 22.5F + 135 * turn, pose, buffers, light, overlay);
            pose.popPose();
        }
        pose.popPose();

        for (int i = 0; i < GLYPHS.length; i++) {
            double[] pivot = GLYPHS[i];
            float wave = Mth.sin(phase + i * 0.7F);
            pose.pushPose();
            pose.translate(0.5 + pivot[0] / 16, (pivot[1] + wave * 0.6) / 16, 0.5 + pivot[2] / 16);
            pose.mulPose(Axis.YP.rotationDegrees(35 + wave * 12));
            float size = 0.8F + wave * 0.2F;
            pose.scale(size, size, size);
            draw(i + 4, pose, buffers, LightTexture.FULL_BRIGHT, overlay);
            pose.popPose();
        }
    }

    private static void bookWing(int part, float angle, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        draw(part, pose, buffers, light, overlay);
        pose.popPose();
    }

    private static void draw(int part, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(-0.5, -0.5, -0.5);
        var minecraft = Minecraft.getInstance();
        minecraft.getItemRenderer().renderModelLists(minecraft.getModelManager().getModel(MODELS.get(part)),
                ItemStack.EMPTY, light, overlay, pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)));
        pose.popPose();
    }
}
