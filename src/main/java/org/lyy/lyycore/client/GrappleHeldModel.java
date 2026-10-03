package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

/** Hides only the held model; inventory icons and dropped items remain visible. */
final class GrappleHeldModel {
    static BakedModel wrap(BakedModel source, BakedModel empty) {
        BakedModel deployed = new BakedModelWrapper<>(source) {
            @Override public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
                boolean held = context.firstPerson() || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                        || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
                return (held ? empty : source).applyTransform(context, pose, leftHand);
            }
        };
        ItemOverrides overrides = new ItemOverrides() {
            @Override public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
                return GrappleControls.hasActiveHook(entity) ? deployed : source;
            }
        };
        return new BakedModelWrapper<>(source) {
            @Override public ItemOverrides getOverrides() { return overrides; }
        };
    }
}
