package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import org.jetbrains.annotations.Nullable;

/** Keeps the exported display transforms and item predicates as the animation contract. */
class SonnetBowModel extends BakedModelWrapper<BakedModel> {
    private SonnetBowModel(BakedModel source) {
        super(source);
    }

    static BakedModel wrapItem(BakedModel source) {
        Map<BakedModel, BakedModel> poses = new IdentityHashMap<>();
        ItemOverrides overrides = new ItemOverrides() {
            @Override
            public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                      @Nullable LivingEntity entity, int seed) {
                BakedModel selected = source.getOverrides().resolve(source, stack, level, entity, seed);
                return poses.computeIfAbsent(selected == null ? source : selected, SonnetBowModel::new);
            }
        };
        return new SonnetBowModel(source) {
            @Override public ItemOverrides getOverrides() { return overrides; }
        };
    }

    static BakedModel unwrap(BakedModel model) {
        return model instanceof SonnetBowModel wrapped ? wrapped.originalModel : model;
    }

    @Override public boolean isCustomRenderer() { return true; }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
        BakedModel transformed = originalModel.applyTransform(context, pose, leftHand);
        // ItemRenderer calls BEWLR immediately after this, with the extra -0.5 translation.
        // Preserve its already-resolved pose (including the remote player's pull predicate).
        SonnetItemRenderer.select(transformed, context, pose);
        return this;
    }
}
