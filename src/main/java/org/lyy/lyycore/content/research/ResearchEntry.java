package org.lyy.lyycore.content.research;

import net.minecraft.resources.ResourceLocation;

/** Keeps the persistent ID separate from the data-pack definition. */
public record ResearchEntry(ResourceLocation id, ResearchDefinition value) { }
