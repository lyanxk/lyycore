package org.lyy.lyycore.checks;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.*;
import org.lyy.lyycore.content.ResearchProgress;
import org.lyy.lyycore.content.research.*;

@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class ResearchMaterialRegressions {
    private static ResearchDefinition definition(List<ResearchMaterial> materials) {
        return new ResearchDefinition(new ItemStack(Items.BOOK), "test", "test", "test", ResearchDefinition.Rarity.COMMON,
                materials, 0, 0, false, Optional.empty(), false, List.of(), Optional.empty());
    }
    private static ItemStack special() {
        var stack = new ItemStack(Items.DIAMOND);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("reserved"));
        return stack;
    }
    @GameTest(template = "empty")
    public static void prioritizedRequirementsReserveBeforeGenericOnes(GameTestHelper test) {
        var player = ReviewRegressions.player(test.getLevel());
        player.getInventory().setItem(0, special());
        player.getInventory().setItem(1, new ItemStack(Items.DIAMOND));
        var research = definition(List.of(new ResearchMaterial(new ItemStack(Items.DIAMOND), 0), new ResearchMaterial(special(), 10)));
        test.assertTrue(ResearchProgress.canAfford(player, research), "Generic requirement stole high-priority material");
        test.assertFalse(player.getInventory().getItem(0).isEmpty(), "Preview consumed material");
        test.assertTrue(ResearchProgress.complete(player, ResourceLocation.parse("lyycore:regression_priority"), research), "Prioritized payment failed");
        test.assertTrue(player.getInventory().getItem(0).isEmpty() && player.getInventory().getItem(1).isEmpty(), "Payment did not consume both requirements");
        test.succeed();
    }
    @GameTest(template = "empty")
    public static void equalPriorityKeepsOrderAndFailureIsAtomic(GameTestHelper test) {
        var player = ReviewRegressions.player(test.getLevel());
        player.getInventory().setItem(0, special());
        player.getInventory().setItem(1, new ItemStack(Items.DIAMOND));
        var broad = new ResearchMaterial(new ItemStack(Items.DIAMOND), 0);
        var narrow = new ResearchMaterial(special(), 0);
        var id = ResourceLocation.parse("lyycore:regression_priority_order");
        test.assertFalse(ResearchProgress.complete(player, id, definition(List.of(broad, narrow))), "Equal priority silently reordered requirements");
        test.assertTrue(player.getInventory().getItem(0).getCount() == 1 && player.getInventory().getItem(1).getCount() == 1
                && !ResearchProgress.completed(player, id), "Failed payment partially consumed/unlocked");
        test.assertTrue(ResearchProgress.complete(player, id, definition(List.of(narrow, broad))), "Configured order was ignored");
        test.succeed();
    }
    @GameTest(template = "empty")
    public static void materialPriorityDefaultsAndNetworkRoundTrip(GameTestHelper test) {
        var ops = test.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var legacy = ResearchMaterial.CODEC.parse(ops, JsonParser.parseString("{\"id\":\"minecraft:diamond\",\"count\":2}")).getOrThrow();
        test.assertTrue(legacy.priority() == 0 && legacy.stack().getCount() == 2, "Legacy material default changed");
        var high = ResearchMaterial.CODEC.parse(ops, JsonParser.parseString("{\"id\":\"minecraft:diamond\",\"priority\":100}")).getOrThrow();
        test.assertTrue(high.priority() == 100, "Priority field not decoded");
        var original = definition(List.of(legacy, new ResearchMaterial(special(), -2), high));
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), test.getLevel().registryAccess());
        try {
            ResearchDefinition.STREAM_CODEC.encode(buf, original);
            var decoded = ResearchDefinition.STREAM_CODEC.decode(buf);
            test.assertTrue(decoded.materials().get(0).priority() == 0 && decoded.materials().get(1).priority() == -2
                    && decoded.materials().get(2).priority() == 100 && ItemStack.matches(decoded.materials().get(1).stack(), special()),
                    "Research sync lost priority/order/components");
        } finally { buf.release(); }
        test.succeed();
    }
}
