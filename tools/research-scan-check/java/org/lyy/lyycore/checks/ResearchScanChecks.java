package org.lyy.lyycore.checks;

import com.google.gson.JsonElement;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.lyy.lyycore.content.research.ResearchManager;

@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class ResearchScanChecks {
    @GameTest(template = "research_empty")
    public static void builtinsAreIsolatedAndIdsRemainStable(GameTestHelper test) {
        var resources = test.getLevel().getServer().getResourceManager();
        var files = FileToIdConverter.json("lyy_research").listMatchingResourcesFromNamespace(resources, "lyycore");
        test.assertTrue(files.size() == 31, "All 31 builtin definitions must be in lyy_research");
        test.assertTrue(FileToIdConverter.json("research").listMatchingResourcesFromNamespace(resources, "lyycore").isEmpty(),
                "A foreign scanner of generic research must not see our builtins");
        test.assertTrue(FileToIdConverter.json("lyycore/research").listMatchingResourcesFromNamespace(resources, "lyycore").isEmpty(),
                "Builtin resources must not be duplicated into previous directories");
        var converter = FileToIdConverter.json("lyy_research");
        for (var file : files.keySet()) {
            var stableId = converter.fileToId(file).withPrefix("research/");
            test.assertTrue(ResearchManager.get(test.getLevel(), stableId) != null, "Missing stable research ID " + stableId);
        }
        test.assertTrue(ResearchManager.all(test.getLevel()).size() == 31, "Directory migration must preserve the full catalog");
        test.succeed();
    }

    private static Resource resource(String marker) {
        return new Resource(null, () -> new ByteArrayInputStream(("{\"marker\":\"" + marker + "\"}").getBytes(StandardCharsets.UTF_8)));
    }
    private static Resource mustNotOpen() {
        return new Resource(null, () -> { throw new AssertionError("A foreign or shadowed research file was opened"); });
    }

    @SuppressWarnings("unchecked")
    @GameTest(template = "research_empty")
    public static void newDirectoryOverridesLegacyWithoutReadingForeignResearch(GameTestHelper test) throws Exception {
        var resources = new LinkedHashMap<ResourceLocation, Resource>();
        resources.put(ResourceLocation.parse("lyycore:lyy_research/same.json"), resource("new"));
        resources.put(ResourceLocation.parse("lyycore:lyycore/research/same.json"), mustNotOpen());
        resources.put(ResourceLocation.parse("lyycore:research/same.json"), mustNotOpen());
        resources.put(ResourceLocation.parse("lyycore:research/legacy.json"), resource("legacy"));
        resources.put(ResourceLocation.parse("addon:lyycore/research/previous.json"), resource("previous"));
        resources.put(ResourceLocation.parse("addon:lyy_research/nested/custom.json"), resource("custom"));
        resources.put(ResourceLocation.parse("astralsorcery:research/unrelated.json"), mustNotOpen());
        resources.put(ResourceLocation.parse("addon:research/unrelated.json"), mustNotOpen());
        var manager = (ResourceManager) Proxy.newProxyInstance(ResourceManager.class.getClassLoader(),
                new Class<?>[]{ResourceManager.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("listResources")) throw new AssertionError("Unexpected resource operation: " + method);
                    var result = new LinkedHashMap<ResourceLocation, Resource>();
                    var prefix = (String) args[0] + "/";
                    var predicate = (Predicate<ResourceLocation>) args[1];
                    resources.forEach((id, resource) -> {
                        if (id.getPath().startsWith(prefix) && predicate.test(id)) result.put(id, resource);
                    });
                    return result;
                });
        var constructor = ResearchManager.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var listener = constructor.newInstance();
        var prepare = ResearchManager.class.getDeclaredMethod("prepare", ResourceManager.class, ProfilerFiller.class);
        prepare.setAccessible(true);
        var prepared = (Map<ResourceLocation, JsonElement>) prepare.invoke(listener, manager, InactiveProfiler.INSTANCE);
        test.assertTrue(prepared.size() == 4, "Only explicit or supported legacy directories may be scanned");
        test.assertTrue(prepared.get(ResourceLocation.parse("lyycore:same")).getAsJsonObject().get("marker").getAsString().equals("new"),
                "New directory must override both older paths");
        test.assertTrue(prepared.containsKey(ResourceLocation.parse("lyycore:legacy"))
                && prepared.containsKey(ResourceLocation.parse("addon:previous"))
                && prepared.containsKey(ResourceLocation.parse("addon:nested/custom")),
                "Namespace, nested paths and previous datapacks must remain supported");
        test.succeed();
    }
}
