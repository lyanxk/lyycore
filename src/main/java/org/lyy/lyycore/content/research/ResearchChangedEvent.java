package org.lyy.lyycore.content.research;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/** Fired after unlock state is persisted, for both research and administrative changes. */
public final class ResearchChangedEvent extends Event {
    public final ServerPlayer player;
    public final ResourceLocation id;
    public final ResearchDefinition definition;
    public final boolean completed;

    public ResearchChangedEvent(ServerPlayer player, ResourceLocation id, ResearchDefinition definition, boolean completed) {
        this.player = player; this.id = id; this.definition = definition; this.completed = completed;
    }
}
