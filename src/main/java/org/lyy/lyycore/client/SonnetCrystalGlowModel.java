package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.lyy.lyycore.content.item.SonnetBowItem;

/** Keeps the exported crystal textures and adds a restrained, full-bright pink rim. */
final class SonnetCrystalGlowModel extends BakedModelWrapper<BakedModel> {
    // Additive light is order independent; sorting every glowing face adds no visual value.
    private static final List<RenderType> GLOW_TYPES = List.of(RenderType.create(
            "lyycore_crystal_glow", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 65536,
            false, false, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_EYES_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, false))
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false)));
    private static final Set<String> GLOW_MATERIALS = Set.of(
            "crystal_darkedge", "crystal_white", "crystal_pink", "crystal_teal",
            "crystal_ice", "crystal_silver", "facet_pearl", "facet_pink");
    private final List<BakedModel> passes;

    private SonnetCrystalGlowModel(BakedModel source) {
        super(source);
        passes = List.of(source, new GlowPass(source));
    }

    static BakedModel wrapItem(BakedModel source) {
        // ItemOverrides keeps its own baked model references: wrap the resolved pose,
        // rather than replacing pose entries in the model manager after baking.
        Map<BakedModel, BakedModel> poses = new IdentityHashMap<>();
        ItemOverrides overrides = new ItemOverrides() {
            @Override
            public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                      @Nullable LivingEntity entity, int seed) {
                BakedModel pose = source.getOverrides().resolve(source, stack, level, entity, seed);
                if (pose == null || !SonnetBowItem.isCrystal(stack)) return pose;
                return poses.computeIfAbsent(pose, SonnetCrystalGlowModel::new);
            }
        };
        return new BakedModelWrapper<>(source) {
            @Override
            public ItemOverrides getOverrides() {
                return overrides;
            }
        };
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean leftHand) {
        originalModel.applyTransform(context, poseStack, leftHand);
        return this;
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        return passes;
    }

    private static final class GlowPass extends BakedModelWrapper<BakedModel> {
        private final Map<Direction, List<BakedQuad>> quads = new HashMap<>();

        GlowPass(BakedModel source) {
            super(source);
            bakeSide(null);
            for (Direction side : Direction.values()) bakeSide(side);
        }

        private void bakeSide(@Nullable Direction side) {
            List<BakedQuad> originals = originalModel.getQuads(null, side, RandomSource.create(42));
            List<BakedQuad> glow = new ArrayList<>();
            for (BakedQuad quad : originals) {
                String texture = quad.getSprite().contents().name().getPath();
                if (!GLOW_MATERIALS.contains(texture.substring(texture.lastIndexOf('/') + 1))) continue;
                // Only the bright edges and pink cores need a halo. The crystal body
                // already has emissive materials; duplicating it wastes fill rate.
                glow.add(expand(quad, 0.006F, 36));
            }
            quads.put(side, List.copyOf(glow));
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
            return quads.get(side);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                       ModelData data, @Nullable RenderType renderType) {
            return quads.get(side);
        }

        @Override
        public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
            return GLOW_TYPES;
        }

        private static BakedQuad expand(BakedQuad quad, float distance, int alpha) {
            int[] vertices = quad.getVertices().clone();
            int stride = vertices.length / 4;
            for (int vertex = 0; vertex < 4; vertex++) {
                int offset = vertex * stride;
                int packedNormal = vertices[offset + 7];
                float nx = (byte) packedNormal;
                float ny = (byte) (packedNormal >> 8);
                float nz = (byte) (packedNormal >> 16);
                float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (length == 0) {
                    nx = quad.getDirection().getStepX();
                    ny = quad.getDirection().getStepY();
                    nz = quad.getDirection().getStepZ();
                    length = 1;
                }
                vertices[offset] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[offset]) + nx / length * distance);
                vertices[offset + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[offset + 1]) + ny / length * distance);
                vertices[offset + 2] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[offset + 2]) + nz / length * distance);
                // NEW_ENTITY uses packed ABGR, not ARGB.
                vertices[offset + 3] = (alpha << 24) | (231 << 16) | (195 << 8) | 255;
                vertices[offset + 6] = LightTexture.FULL_BRIGHT;
            }
            return new BakedQuad(vertices, -1, quad.getDirection(), quad.getSprite(), false, false);
        }
    }
}
