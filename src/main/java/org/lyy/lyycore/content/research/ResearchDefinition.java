package org.lyy.lyycore.content.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Optional;

/** A research definition. Completing it unlocks its stable research ID for that player. */
public record ResearchDefinition(ItemStack icon, String title, String summary, String description,
                             Rarity rarity, List<ItemStack> materials, int experienceLevels, int experiencePoints, boolean dangerous,
                             Optional<Production> production, boolean unlocksSkills,
                             List<ResourceLocation> prerequisites, Optional<ResourceLocation> requiredAdvancement) {
    /** A present production definition also enables free transcription in the memory screen. */
    public record Production(ItemStack result, int duration) {
        public static final Codec<Production> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.CODEC.fieldOf("result").forGetter(Production::result),
                Codec.intRange(1, 72000).fieldOf("duration").forGetter(Production::duration)
        ).apply(i, Production::new));
        public Production {
            if (result.isEmpty() || result.getCount() > result.getMaxStackSize() || duration < 1 || duration > 72000)
                throw new IllegalArgumentException("Production requires one valid result stack and 1–72000 ticks");
            result = result.copy();
        }
        @Override public ItemStack result() { return result.copy(); }
    }
    public enum Rarity implements StringRepresentable {
        COMMON(0xFFFFFF), MEDIUM(0x55CCFF), HIGH(0xCC88FF), SPECIAL(0xFFB3D9), UNKNOWN(0xBD3030);
        public final int color;
        Rarity(int color) { this.color = color; }
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public ResearchDefinition {
        if (icon.isEmpty() || experienceLevels < 0 || experiencePoints < 0
                || experienceLevels > 0 && experiencePoints > 0 || materials.stream().anyMatch(ItemStack::isEmpty))
            throw new IllegalArgumentException("Research requires an icon, nonempty materials and either a level or point cost");
        icon = icon.copy();
        materials = materials.stream().map(ItemStack::copy).toList();
        prerequisites = List.copyOf(prerequisites);
    }
    public static final Codec<ResearchDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.STRICT_SINGLE_ITEM_CODEC.fieldOf("icon").forGetter(ResearchDefinition::icon),
            Codec.STRING.fieldOf("title").forGetter(ResearchDefinition::title),
            Codec.STRING.fieldOf("summary").forGetter(ResearchDefinition::summary),
            Codec.STRING.fieldOf("description").forGetter(ResearchDefinition::description),
            StringRepresentable.fromEnum(Rarity::values).optionalFieldOf("rarity", Rarity.COMMON).forGetter(ResearchDefinition::rarity),
            ItemStack.CODEC.listOf().optionalFieldOf("materials", List.of()).forGetter(ResearchDefinition::materials),
            Codec.intRange(0, 10000).optionalFieldOf("experience_levels", 0).forGetter(ResearchDefinition::experienceLevels),
            Codec.intRange(0, 1000000).optionalFieldOf("experience_points", 0).forGetter(ResearchDefinition::experiencePoints),
            Codec.BOOL.optionalFieldOf("dangerous", false).forGetter(ResearchDefinition::dangerous),
            Production.CODEC.optionalFieldOf("production").forGetter(ResearchDefinition::production),
            Codec.BOOL.optionalFieldOf("unlocks_skills", false).forGetter(ResearchDefinition::unlocksSkills),
            ResourceLocation.CODEC.listOf().optionalFieldOf("prerequisites", List.of()).forGetter(ResearchDefinition::prerequisites),
            ResourceLocation.CODEC.optionalFieldOf("required_advancement").forGetter(ResearchDefinition::requiredAdvancement)
    ).apply(i, ResearchDefinition::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchDefinition> STREAM_CODEC = StreamCodec.of((b, r) -> {
        ItemStack.STREAM_CODEC.encode(b, r.icon);
        b.writeUtf(r.title); b.writeUtf(r.summary); b.writeUtf(r.description);
        b.writeEnum(r.rarity);
        b.writeCollection(r.materials, (buf, stack) -> ItemStack.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, stack));
        b.writeVarInt(r.experienceLevels); b.writeVarInt(r.experiencePoints); b.writeBoolean(r.dangerous);
        b.writeBoolean(r.production.isPresent());
        r.production.ifPresent(p -> { ItemStack.STREAM_CODEC.encode(b, p.result); b.writeVarInt(p.duration); });
        b.writeBoolean(r.unlocksSkills);
        b.writeCollection(r.prerequisites, (buf, id) -> buf.writeResourceLocation(id));
        b.writeOptional(r.requiredAdvancement, (buf, id) -> buf.writeResourceLocation(id));
    }, b -> new ResearchDefinition(ItemStack.STREAM_CODEC.decode(b), b.readUtf(), b.readUtf(), b.readUtf(),
            b.readEnum(Rarity.class), b.readList(buf -> ItemStack.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf)),
            b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readBoolean()
                    ? Optional.of(new Production(ItemStack.STREAM_CODEC.decode(b), b.readVarInt())) : Optional.empty(), b.readBoolean(),
            b.readList(buf -> buf.readResourceLocation()), b.readOptional(buf -> buf.readResourceLocation())));
}
