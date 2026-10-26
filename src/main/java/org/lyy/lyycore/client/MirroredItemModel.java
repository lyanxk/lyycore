package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.lyy.lyycore.LyyCore;

/** Preserve the artist's reflected display transform without rendering inside-out quads. */
@EventBusSubscriber(modid = LyyCore.MODID, value = Dist.CLIENT)
public final class MirroredItemModel extends BakedModelWrapper<BakedModel> {
    private final Map<BakedModel, Reversed> reversedModels = new IdentityHashMap<>();

    private MirroredItemModel(BakedModel original) {
        super(original);
    }

    static BakedModel wrap(BakedModel original) {
        return new MirroredItemModel(original);
    }

    @SubscribeEvent
    public static void register(ModelEvent.ModifyBakingResult event) {
        // These are live 3D item models. Pre-rendered Blender icons are intentionally excluded.
        for (String name : List.of("summoning_beacon", "mirror", "reforging_table", "spacetime_factory", "riftbreaker", "celestial_order", "frozen_moment")) {
            var id = new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, name), "inventory");
            event.getModels().computeIfPresent(id, (key, model) -> wrap(model));
        }
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
        float before = pose.last().pose().determinant();
        BakedModel transformed = originalModel.applyTransform(context, pose, leftHand);
        float after = pose.last().pose().determinant();
        // Compare signs rather than the absolute sign: the GUI already flips its Y axis.
        if (before != 0 && after != 0 && (before < 0) != (after < 0)) {
            return reversedModels.computeIfAbsent(transformed, Reversed::new);
        }
        return transformed;
    }

    static BakedQuad reverseWinding(BakedQuad quad) {
        int[] source = quad.getVertices();
        int stride = source.length / 4;
        int[] reversed = source.clone();
        // [0,1,2,3] -> [0,3,2,1]. Move complete vertex records, including UV/light/normal.
        System.arraycopy(source, stride * 3, reversed, stride, stride);
        System.arraycopy(source, stride, reversed, stride * 3, stride);
        // Do not invert normals: the reflection's normal matrix already transforms them.
        return new BakedQuad(reversed, quad.getTintIndex(), quad.getDirection(), quad.getSprite(),
                quad.isShade(), quad.hasAmbientOcclusion());
    }

    private static final class Reversed extends BakedModelWrapper<BakedModel> {
        private final Map<List<BakedQuad>, List<BakedQuad>> quads = new IdentityHashMap<>();
        private final Map<BakedModel, Reversed> passes = new IdentityHashMap<>();

        private Reversed(BakedModel original) {
            super(original);
        }

        private List<BakedQuad> reverse(List<BakedQuad> source) {
            if (source.isEmpty()) return source;
            return quads.computeIfAbsent(source, list -> list.stream().map(MirroredItemModel::reverseWinding).toList());
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
            return reverse(originalModel.getQuads(state, side, random));
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                       ModelData data, @Nullable RenderType renderType) {
            return reverse(originalModel.getQuads(state, side, random, data, renderType));
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            // Returning the original passes here would silently discard the winding correction.
            return originalModel.getRenderPasses(stack, fabulous).stream()
                    .map(model -> (BakedModel) (model == originalModel ? this : passes.computeIfAbsent(model, Reversed::new)))
                    .toList();
        }
    }
}
