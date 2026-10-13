package org.lyy.lyycore.content;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Machine-local copies of loaded loot tables. No death callbacks or global table mutations. */
public final class CapturedLoot {
    private static final Map<ReloadableServerRegistries.Holder, Map<ResourceKey<LootTable>, LootTable>> CACHE = new WeakHashMap<>();
    private CapturedLoot() { }

    public static List<ItemStack> roll(ServerLevel level, Mob mob, Vec3 origin) {
        var registries = level.getServer().reloadableRegistries();
        var key = mob.getLootTable();
        var table = CACHE.computeIfAbsent(registries, ignored -> new HashMap<>()).computeIfAbsent(key, ignored -> {
            var ops = registries.get().createSerializationContext(JsonOps.INSTANCE);
            var json = copyTable(registries, key, new HashSet<>());
            var copy = LootTable.DIRECT_CODEC.parse(ops, json).getOrThrow();
            copy.setLootTableId(key.location());
            return copy;
        });
        mob.setPos(origin);
        var params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, mob)
                .withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
                .create(LootContextParamSets.ENTITY);
        // Keep the public generation path, including NeoForge global loot modifiers.
        return table.getRandomItems(params);
    }

    private static JsonElement copyTable(ReloadableServerRegistries.Holder registries, ResourceKey<LootTable> key,
                                         Set<ResourceKey<LootTable>> visiting) {
        if (!visiting.add(key)) return new JsonObject();
        var json = LootTable.DIRECT_CODEC.encodeStart(registries.get().createSerializationContext(JsonOps.INSTANCE),
                registries.getLootTable(key)).getOrThrow();
        rewrite(json, registries, visiting);
        visiting.remove(key);
        return json;
    }

    private static void rewrite(JsonElement json, ReloadableServerRegistries.Holder registries, Set<ResourceKey<LootTable>> visiting) {
        if (json.isJsonArray()) { for (var child : json.getAsJsonArray()) rewrite(child, registries, visiting); return; }
        if (!json.isJsonObject()) return;
        var object = json.getAsJsonObject();
        if (object.has("conditions")) {
            var conditions = new JsonArray();
            for (var condition : object.getAsJsonArray("conditions")) {
                var kept = keepCondition(condition.getAsJsonObject());
                if (kept != null) conditions.add(kept);
            }
            object.add("conditions", conditions);
        }
        if (object.has("type") && object.get("type").getAsString().equals("minecraft:loot_table")
                && object.has("value") && object.get("value").isJsonPrimitive()) {
            object.add("value", copyTable(registries, ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.parse(object.get("value").getAsString())), visiting));
            return;
        }
        for (var entry : object.entrySet()) if (!entry.getKey().equals("conditions")) rewrite(entry.getValue(), registries, visiting);
    }

    /** Null means unrestricted. Preserve random rolls and predicates on the captured mob itself. */
    private static JsonObject keepCondition(JsonObject condition) {
        String type = condition.get("condition").getAsString();
        if (Set.of("minecraft:killed_by_player", "minecraft:damage_source_properties", "minecraft:match_tool").contains(type)) return null;
        if (type.equals("minecraft:entity_properties") && !condition.get("entity").getAsString().equals("this")) return null;
        if (type.equals("minecraft:inverted")) {
            var term = keepCondition(condition.getAsJsonObject("term"));
            if (term == null) return null;
            condition.add("term", term);
        } else if (type.equals("minecraft:all_of") || type.equals("minecraft:any_of")) {
            var terms = new JsonArray();
            for (var term : condition.getAsJsonArray("terms")) {
                var kept = keepCondition(term.getAsJsonObject());
                if (kept == null && type.equals("minecraft:any_of")) return null;
                if (kept != null) terms.add(kept);
            }
            if (terms.isEmpty()) return null;
            condition.add("terms", terms);
        }
        return condition;
    }
}
