package org.lyy.lyycore.content.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/** An ItemStack-shaped material requirement with an optional allocation priority. */
public record ResearchMaterial(ItemStack stack, int priority) {
    public ResearchMaterial {
        if (stack.isEmpty()) throw new IllegalArgumentException("Research material cannot be empty");
        stack = stack.copy();
    }
    @Override public ItemStack stack() { return stack.copy(); }

    public static final Codec<ResearchMaterial> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.ITEM_NON_AIR_CODEC.fieldOf("id").forGetter(m -> m.stack.getItemHolder()),
            Codec.intRange(1, 99).optionalFieldOf("count", 1).forGetter(m -> m.stack.getCount()),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(m -> m.stack.getComponentsPatch()),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(ResearchMaterial::priority)
    ).apply(i, (item, count, components, priority) -> new ResearchMaterial(new ItemStack(item, count, components), priority)));

    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchMaterial> STREAM_CODEC = StreamCodec.of(
            (buf, material) -> { ItemStack.STREAM_CODEC.encode(buf, material.stack); buf.writeVarInt(material.priority); },
            buf -> new ResearchMaterial(ItemStack.STREAM_CODEC.decode(buf), buf.readVarInt()));
}
