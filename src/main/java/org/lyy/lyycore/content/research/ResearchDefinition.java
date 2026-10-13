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
                             Rarity rarity, List<ResearchMaterial> materials, int experienceLevels, int experiencePoints, boolean dangerous,
                             Optional<Production> production, boolean unlocksSkills,
                             List<ResourceLocation> prerequisites, Optional<ResourceLocation> requiredAdvancement, Display display, Optional<ResourceLocation> completionAdvancement) {
    public enum Purpose implements StringRepresentable {
        GENERAL, RESEARCH, PRODUCTION, LOGISTICS, EXPLORATION, COMBAT, RITUAL;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    /** Presentation metadata does not change unlock conditions or rarity. */
    public record Display(int stage, Purpose purpose, int order) {
        public static final Display DEFAULT = new Display(1, Purpose.GENERAL, 0);
        public static final Codec<Display> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, 4).optionalFieldOf("stage", 1).forGetter(Display::stage),
                StringRepresentable.fromEnum(Purpose::values).optionalFieldOf("purpose", Purpose.GENERAL).forGetter(Display::purpose),
                Codec.INT.optionalFieldOf("order", 0).forGetter(Display::order)
        ).apply(i, Display::new));
    }
    /** A present production definition also enables free transcription in the memory screen. */
    public record Production(ItemStack result, int duration, Optional<ExperimentDefinition> experiment) {
        public Production(ItemStack result, int duration) { this(result, duration, Optional.empty()); }
        public static final Codec<Production> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.CODEC.fieldOf("result").forGetter(Production::result),
                Codec.intRange(1, 72000).fieldOf("duration").forGetter(Production::duration),
                ExperimentDefinition.CODEC.optionalFieldOf("experiment").forGetter(Production::experiment)
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
                || experienceLevels > 0 && experiencePoints > 0)
            throw new IllegalArgumentException("Research requires an icon, nonempty materials and either a level or point cost");
        icon = icon.copy();
        materials = List.copyOf(materials);
        prerequisites = List.copyOf(prerequisites);
    }
    public static final Codec<ResearchDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.STRICT_SINGLE_ITEM_CODEC.fieldOf("icon").forGetter(ResearchDefinition::icon),
            Codec.STRING.fieldOf("title").forGetter(ResearchDefinition::title),
            Codec.STRING.fieldOf("summary").forGetter(ResearchDefinition::summary),
            Codec.STRING.fieldOf("description").forGetter(ResearchDefinition::description),
            StringRepresentable.fromEnum(Rarity::values).optionalFieldOf("rarity", Rarity.COMMON).forGetter(ResearchDefinition::rarity),
            ResearchMaterial.CODEC.listOf().optionalFieldOf("materials", List.of()).forGetter(ResearchDefinition::materials),
            Codec.intRange(0, 10000).optionalFieldOf("experience_levels", 0).forGetter(ResearchDefinition::experienceLevels),
            Codec.intRange(0, 1000000).optionalFieldOf("experience_points", 0).forGetter(ResearchDefinition::experiencePoints),
            Codec.BOOL.optionalFieldOf("dangerous", false).forGetter(ResearchDefinition::dangerous),
            Production.CODEC.optionalFieldOf("production").forGetter(ResearchDefinition::production),
            Codec.BOOL.optionalFieldOf("unlocks_skills", false).forGetter(ResearchDefinition::unlocksSkills),
            ResourceLocation.CODEC.listOf().optionalFieldOf("prerequisites", List.of()).forGetter(ResearchDefinition::prerequisites),
            ResourceLocation.CODEC.optionalFieldOf("required_advancement").forGetter(ResearchDefinition::requiredAdvancement),
            Display.CODEC.optionalFieldOf("display", Display.DEFAULT).forGetter(ResearchDefinition::display),
            ResourceLocation.CODEC.optionalFieldOf("completion_advancement").forGetter(ResearchDefinition::completionAdvancement)
    ).apply(i, ResearchDefinition::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchDefinition> STREAM_CODEC = StreamCodec.of((b, r) -> {
        ItemStack.STREAM_CODEC.encode(b, r.icon);
        b.writeUtf(r.title); b.writeUtf(r.summary); b.writeUtf(r.description);
        b.writeEnum(r.rarity);
        b.writeCollection(r.materials, (buf, material) -> ResearchMaterial.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, material));
        b.writeVarInt(r.experienceLevels); b.writeVarInt(r.experiencePoints); b.writeBoolean(r.dangerous);
        b.writeBoolean(r.production.isPresent());
        r.production.ifPresent(p -> {
            ItemStack.STREAM_CODEC.encode(b, p.result); b.writeVarInt(p.duration);
            b.writeBoolean(p.experiment.isPresent()); p.experiment.ifPresent(e -> e.write(b));
        });
        b.writeBoolean(r.unlocksSkills);
        b.writeCollection(r.prerequisites, (buf, id) -> buf.writeResourceLocation(id));
        b.writeOptional(r.requiredAdvancement, (buf, id) -> buf.writeResourceLocation(id));
        b.writeVarInt(r.display.stage); b.writeEnum(r.display.purpose); b.writeVarInt(r.display.order);
        b.writeOptional(r.completionAdvancement, (buf, id) -> buf.writeResourceLocation(id));
    }, b -> new ResearchDefinition(ItemStack.STREAM_CODEC.decode(b), b.readUtf(), b.readUtf(), b.readUtf(),
            b.readEnum(Rarity.class), b.readList(buf -> ResearchMaterial.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf)),
            b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readBoolean()
                    ? Optional.of(new Production(ItemStack.STREAM_CODEC.decode(b), b.readVarInt(),
                    b.readBoolean() ? Optional.of(ExperimentDefinition.read(b)) : Optional.empty())) : Optional.empty(), b.readBoolean(),
            b.readList(buf -> buf.readResourceLocation()), b.readOptional(buf -> buf.readResourceLocation()),
            new Display(b.readVarInt(), b.readEnum(Purpose.class), b.readVarInt()), b.readOptional(buf -> buf.readResourceLocation())));
}
