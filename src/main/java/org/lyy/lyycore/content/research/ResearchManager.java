package org.lyy.lyycore.content.research;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Independent data-pack category, with one catalog per server resource generation. */
@EventBusSubscriber(modid = "lyycore")
public final class ResearchManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "lyycore/research";
    private static final FileToIdConverter LEGACY_FILES = FileToIdConverter.json("research");
    private static final Map<ReloadableServerResources, ResearchManager> SERVERS = Collections.synchronizedMap(new WeakHashMap<>());
    private static Map<ResourceLocation, ResearchEntry> clientEntries = Map.of();
    private Map<ResourceLocation, ResearchEntry> entries = Map.of();

    private ResearchManager() { super(GSON, DIRECTORY); }

    @Override protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resources, ProfilerFiller profiler) {
        var files = super.prepare(resources, profiler);
        // Only our namespace owns the legacy directory; other mods use unrelated research formats.
        for (var entry : LEGACY_FILES.listMatchingResourcesFromNamespace(resources, "lyycore").entrySet()) {
            var id = LEGACY_FILES.fileToId(entry.getKey());
            if (files.containsKey(id)) continue; // Explicit definitions in the dedicated directory take precedence.
            try (var reader = entry.getValue().openAsReader()) {
                files.put(id, GsonHelper.fromJson(GSON, reader, JsonElement.class));
            } catch (IllegalArgumentException | IOException | JsonParseException exception) {
                LogUtils.getLogger().error("Couldn't parse legacy research file {}", entry.getKey(), exception);
            }
        }
        return files;
    }

    @SubscribeEvent public static void register(AddReloadListenerEvent event) {
        var manager = new ResearchManager();
        SERVERS.put(event.getServerResources(), manager);
        event.addListener(manager);
    }

    @Override protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resources, ProfilerFiller profiler) {
        var loaded = new LinkedHashMap<ResourceLocation, ResearchEntry>();
        var codec = ConditionalOps.createConditionalCodec(ResearchDefinition.CODEC);
        var ops = makeConditionalOps();
        files.forEach((file, json) -> {
            // Preserve IDs used by existing saves, notes, commands and skills.
            var id = file.withPrefix("research/");
            try {
                codec.parse(ops, json).getOrThrow(JsonParseException::new).ifPresent(value -> loaded.put(id, new ResearchEntry(id, value)));
            } catch (IllegalArgumentException | JsonParseException exception) {
                LogUtils.getLogger().error("Couldn't parse research {}", id, exception);
            }
        });
        entries = Map.copyOf(loaded);
        LogUtils.getLogger().info("Loaded {} research definitions", entries.size());
    }

    private static Map<ResourceLocation, ResearchEntry> catalog(Level level) {
        if (level.isClientSide) return clientEntries;
        var manager = SERVERS.get(level.getServer().getServerResources().managers());
        return manager == null ? Map.of() : manager.entries;
    }

    public static Collection<ResearchEntry> all(Level level) { return catalog(level).values(); }
    @Nullable public static ResearchEntry get(Level level, ResourceLocation id) { return catalog(level).get(id); }

    public static void updateClient(Collection<ResearchEntry> entries) {
        var loaded = new LinkedHashMap<ResourceLocation, ResearchEntry>();
        for (var entry : entries) {
            if (loaded.putIfAbsent(entry.id(), entry) != null)
                throw new IllegalArgumentException("Duplicate research ID: " + entry.id());
        }
        clientEntries = Map.copyOf(loaded);
    }

    public static void clearClient() { clientEntries = Map.of(); }
}
